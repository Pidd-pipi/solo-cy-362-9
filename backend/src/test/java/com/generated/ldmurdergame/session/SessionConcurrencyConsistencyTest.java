package com.generated.ldmurdergame.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.generated.ldmurdergame.dto.CancelResult;
import com.generated.ldmurdergame.dto.SessionView;
import com.generated.ldmurdergame.dto.SignupResult;
import com.generated.ldmurdergame.model.SignupStatuses;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 并发一致性回归：调座、取消递补、报名/取消交错压力下，
 * 列表、详情与写接口回包中的每份场次视图都必须来自同一时点（满足全部不变量）。
 */
class SessionConcurrencyConsistencyTest extends SessionApiTestBase {

  @Test
  @DisplayName("并发报名：30 人抢 4 座不超卖，候补位次唯一且连续")
  void concurrentSignupsNeverOversell() throws Exception {
    SessionView session = createSession("并发报名回归", 4);
    int racers = 30;

    ExecutorService pool = Executors.newFixedThreadPool(racers);
    CountDownLatch ready = new CountDownLatch(racers);
    CountDownLatch go = new CountDownLatch(1);
    List<Future<ResponseEntity<SignupResult>>> futures = new ArrayList<>();
    for (int i = 1; i <= racers; i++) {
      String name = "玩家" + i;
      futures.add(pool.submit(() -> {
        ready.countDown();
        go.await();
        return rest.postForEntity("/sessions/" + session.id() + "/signups",
            Map.of("playerName", name), SignupResult.class);
      }));
    }
    assertTrue(ready.await(10, TimeUnit.SECONDS), "全部报名线程应就绪");
    go.countDown();

    List<String> violations = new ArrayList<>();
    int confirmed = 0;
    Set<Integer> waitlistPositions = new TreeSet<>();
    for (Future<ResponseEntity<SignupResult>> future : futures) {
      ResponseEntity<SignupResult> response = future.get(15, TimeUnit.SECONDS);
      assertEquals(HttpStatus.CREATED, response.getStatusCode(), "不同玩家的并发报名不应被拒绝");
      SignupResult result = response.getBody();
      violations.addAll(SessionViewInvariants.check(result.session(), "并发报名回包"));
      if (SignupStatuses.CONFIRMED.equals(result.status())) {
        confirmed++;
      } else {
        waitlistPositions.add(result.position());
      }
    }
    pool.shutdown();

    assertEquals(4, confirmed, "不变量「不超卖」: 恰好 4 人应确认占位");
    assertEquals(racers - 4, waitlistPositions.size(), "不变量「候补位次唯一」: 26 个候补位次应互不重复");
    assertEquals(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26),
        waitlistPositions, "不变量「候补位次连续」: 应恰好为 1..26");

    SessionView finalView = detail(session.id());
    assertEquals(4, finalView.confirmedCount(), "最终确认人数应等于座位数");
    assertEquals(26, finalView.waitlistCount(), "最终候补人数应为 26");
    violations.addAll(SessionViewInvariants.check(finalView, "并发报名后详情"));
    violations.addAll(SessionViewInvariants.check(onlySession(listByDate(TEST_DATE)), "并发报名后列表"));
    assertNoViolations(violations);
  }

  @Test
  @DisplayName("并发同名报名：同一场次同一玩家至多一个有效占位")
  void concurrentSamePlayerSignupsSingleOccupancy() throws Exception {
    SessionView session = createSession("同名并发回归", 2);
    int racers = 12;

    ExecutorService pool = Executors.newFixedThreadPool(racers);
    CountDownLatch ready = new CountDownLatch(racers);
    CountDownLatch go = new CountDownLatch(1);
    List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
    for (int i = 0; i < racers; i++) {
      futures.add(pool.submit(() -> {
        ready.countDown();
        go.await();
        return rest.postForEntity("/sessions/" + session.id() + "/signups",
            Map.of("playerName", "同名人"), String.class);
      }));
    }
    assertTrue(ready.await(10, TimeUnit.SECONDS), "全部报名线程应就绪");
    go.countDown();

    int created = 0;
    int conflicted = 0;
    for (Future<ResponseEntity<String>> future : futures) {
      HttpStatus status = HttpStatus.valueOf(future.get(15, TimeUnit.SECONDS).getStatusCode().value());
      if (status == HttpStatus.CREATED) {
        created++;
      } else if (status == HttpStatus.CONFLICT) {
        conflicted++;
      }
    }
    pool.shutdown();

    assertEquals(1, created, "不变量「不重复占位」: 同名并发报名应恰好成功一次");
    assertEquals(racers - 1, conflicted, "其余同名报名应全部返回 409");

    SessionView view = detail(session.id());
    long occurrences = view.confirmed().stream().filter(s -> s.playerName().equals("同名人")).count()
        + view.waitlist().stream().filter(s -> s.playerName().equals("同名人")).count();
    assertEquals(1, occurrences, "不变量「不重复占位」: 该玩家在占位与候补中总共只应出现一次");
    assertConsistent(view, "同名并发后详情");
  }

  @Test
  @DisplayName("调座/取消递补/报名交错：每份响应的座位数、名单、候补、余位、状态始终自洽")
  void concurrentWritesEveryViewStaysConsistent() throws Exception {
    SessionView session = createSession("一致性回归", 3);
    for (String name : List.of("p1", "p2", "p3", "p4", "p5")) {
      signup(session.id(), name); // 3 确认 + 2 候补
    }

    long deadline = System.currentTimeMillis() + 4_000;
    Queue<String> violations = new ConcurrentLinkedQueue<>();
    ExecutorService pool = Executors.newFixedThreadPool(8);
    CountDownLatch go = new CountDownLatch(1);
    List<Future<?>> futures = new ArrayList<>();

    // 3 个读线程：持续拉取详情与列表，逐份校验不变量
    for (int r = 0; r < 3; r++) {
      futures.add(pool.submit(() -> {
        go.await();
        while (System.currentTimeMillis() < deadline) {
          violations.addAll(SessionViewInvariants.check(detail(session.id()), "并发期间详情"));
          for (SessionView view : listByDate(TEST_DATE)) {
            violations.addAll(SessionViewInvariants.check(view, "并发期间列表"));
          }
        }
        return null;
      }));
    }
    // 2 个写线程：确认位玩家取消后立即重报（触发递补）
    for (String name : List.of("p1", "p2")) {
      futures.add(pool.submit(() -> {
        go.await();
        while (System.currentTimeMillis() < deadline) {
          checkMutation(cancel(session.id(), name), "取消回包", violations);
          checkMutation(signup(session.id(), name), "重报回包", violations);
        }
        return null;
      }));
    }
    // 1 个写线程：座位数 3 ↔ 5 来回调整（调大触发递补，调小可能被拒）
    futures.add(pool.submit(() -> {
      go.await();
      while (System.currentTimeMillis() < deadline) {
        checkMutation(rest.exchange("/sessions/" + session.id(), HttpMethod.PATCH,
            new HttpEntity<>(Map.of("seatCount", 5)), SessionView.class), "调大座位回包", violations);
        checkMutation(rest.exchange("/sessions/" + session.id(), HttpMethod.PATCH,
            new HttpEntity<>(Map.of("seatCount", 3)), SessionView.class), "调小座位回包", violations);
      }
      return null;
    }));
    // 1 个写线程：临时玩家报名即退，制造名额扰动
    futures.add(pool.submit(() -> {
      go.await();
      int i = 0;
      while (System.currentTimeMillis() < deadline) {
        String name = "temp" + (i++ % 4);
        checkMutation(signup(session.id(), name), "临时报名回包", violations);
        checkMutation(cancel(session.id(), name), "临时取消回包", violations);
      }
      return null;
    }));

    go.countDown();
    for (Future<?> future : futures) {
      future.get(30, TimeUnit.SECONDS);
    }
    pool.shutdown();

    SessionView finalView = detail(session.id());
    violations.addAll(SessionViewInvariants.check(finalView, "压测结束后详情"));
    assertNoViolations(new ArrayList<>(violations));
  }

  /** 校验写接口回包中的场次视图（2xx 时）；409/404 属于业务拒绝，跳过。 */
  private static void checkMutation(ResponseEntity<?> response, String source, Queue<String> violations) {
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      return;
    }
    Object body = response.getBody();
    SessionView view = null;
    if (body instanceof SignupResult result) {
      view = result.session();
    } else if (body instanceof CancelResult result) {
      view = result.session();
    } else if (body instanceof SessionView sessionView) {
      view = sessionView;
    }
    if (view != null) {
      violations.addAll(SessionViewInvariants.check(view, source));
    }
  }

  private static SessionView onlySession(List<SessionView> sessions) {
    assertEquals(1, sessions.size(), "列表应恰好包含本场次");
    return sessions.get(0);
  }
}
