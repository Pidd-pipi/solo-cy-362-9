package com.generated.ldmurdergame.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.generated.ldmurdergame.dto.CancelResult;
import com.generated.ldmurdergame.dto.SessionView;
import com.generated.ldmurdergame.dto.SignupResult;
import com.generated.ldmurdergame.model.SignupStatuses;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** 边界与业务流程回归：空场次、满员带候补、取消后重报、无效场次、非法参数。 */
class SessionApiRegressionTest extends SessionApiTestBase {

  @Test
  @DisplayName("空场次：详情与列表均自洽，余位等于座位数")
  void emptySessionIsConsistent() {
    SessionView created = createSession("空场回归", 6);

    SessionView fromDetail = detail(created.id());
    assertEquals(0, fromDetail.confirmedCount(), "空场次确认人数应为 0");
    assertEquals(6, fromDetail.remainingSeats(), "空场次余位应等于座位数");
    assertEquals(List.of(), fromDetail.confirmed(), "空场次确认名单应为空");
    assertEquals(List.of(), fromDetail.waitlist(), "空场次候补队列应为空");
    assertConsistent(fromDetail, "空场次详情");

    List<SessionView> list = listByDate(TEST_DATE);
    assertEquals(1, list.size(), "列表应恰好包含本场次");
    assertConsistent(list.get(0), "空场次列表项");
  }

  @Test
  @DisplayName("满员带候补：候补按报名先后排位，位次连续")
  void fullSessionWithWaitlistKeepsOrder() {
    SessionView session = createSession("候补回归", 2);
    signup(session.id(), "甲");
    signup(session.id(), "乙");
    signup(session.id(), "丙");
    signup(session.id(), "丁");

    SessionView view = detail(session.id());
    assertEquals(List.of("甲", "乙"), playerNames(view.confirmed()), "确认名单应按报名先后");
    assertEquals(List.of("丙", "丁"), playerNames(view.waitlist()), "候补队列应按报名先后");
    assertEquals(0, view.remainingSeats(), "满员场次余位应为 0");
    assertTrue(view.full(), "满员标记应为 true");
    assertConsistent(view, "满员带候补详情");
  }

  @Test
  @DisplayName("取消后重报：回到候补队尾，同一场次不重复占位")
  void cancelThenResignupAppendsToQueueTail() {
    SessionView session = createSession("重报回归", 1);
    signup(session.id(), "甲");
    signup(session.id(), "乙");

    ResponseEntity<CancelResult> cancelResponse = cancel(session.id(), "甲");
    assertEquals(HttpStatus.OK, cancelResponse.getStatusCode());
    assertEquals(List.of("乙"), cancelResponse.getBody().promoted(), "退位后应按报名先后递补乙");
    assertConsistent(cancelResponse.getBody().session(), "取消回包");

    ResponseEntity<SignupResult> resignup = signup(session.id(), "甲");
    assertEquals(HttpStatus.CREATED, resignup.getStatusCode());
    assertEquals(SignupStatuses.WAITLIST, resignup.getBody().status(), "重报时满员应进入候补");
    assertEquals(1, resignup.getBody().position(), "重报应排在候补队尾");

    SessionView view = detail(session.id());
    assertEquals(List.of("乙"), playerNames(view.confirmed()), "乙应处于占位状态");
    assertEquals(List.of("甲"), playerNames(view.waitlist()), "甲应只出现在候补队列");
    assertConsistent(view, "取消后重报详情");
  }

  @Test
  @DisplayName("重复报名：同一场次同一玩家只能占一个位置")
  void duplicateSignupRejected() {
    SessionView session = createSession("重复报名回归", 3);
    assertEquals(HttpStatus.CREATED, signup(session.id(), "甲").getStatusCode());

    ResponseEntity<String> duplicate = rest.postForEntity("/sessions/" + session.id() + "/signups",
        Map.of("playerName", "甲"), String.class);
    assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode(), "重复报名应返回 409");

    SessionView view = detail(session.id());
    assertEquals(1, view.confirmedCount(), "重复报名不得增加占位");
    assertConsistent(view, "重复报名后详情");
  }

  @Test
  @DisplayName("递补顺序：多人候补时严格按报名先后递补")
  void promotionFollowsSignupOrder() {
    SessionView session = createSession("递补回归", 2);
    signup(session.id(), "甲");
    signup(session.id(), "乙");
    signup(session.id(), "丙");
    signup(session.id(), "丁");

    ResponseEntity<CancelResult> first = cancel(session.id(), "甲");
    assertEquals(List.of("丙"), first.getBody().promoted(), "第一个退位应递补丙");
    assertConsistent(first.getBody().session(), "第一次递补回包");

    ResponseEntity<CancelResult> second = cancel(session.id(), "乙");
    assertEquals(List.of("丁"), second.getBody().promoted(), "第二个退位应递补丁");
    SessionView view = second.getBody().session();
    assertEquals(List.of("丙", "丁"), playerNames(view.confirmed()), "最终确认名单应为丙丁");
    assertConsistent(view, "第二次递补回包");
  }

  @Test
  @DisplayName("调座位：不允许低于已确认人数，调大后自动递补")
  void seatAdjustmentRespectsConfirmedAndPromotes() {
    SessionView session = createSession("调座回归", 2);
    signup(session.id(), "甲");
    signup(session.id(), "乙");
    signup(session.id(), "丙");

    ResponseEntity<String> shrink = rest.exchange("/sessions/" + session.id(), HttpMethod.PATCH,
        new HttpEntity<>(Map.of("seatCount", 1)), String.class);
    assertEquals(HttpStatus.CONFLICT, shrink.getStatusCode(), "座位数低于已确认人数应返回 409");

    ResponseEntity<SessionView> grow = rest.exchange("/sessions/" + session.id(), HttpMethod.PATCH,
        new HttpEntity<>(mapOf3()), SessionView.class);
    assertEquals(HttpStatus.OK, grow.getStatusCode());
    assertEquals(3, grow.getBody().seatCount(), "座位数应更新为 3");
    assertEquals(List.of("丙"), tail(grow.getBody().confirmed(), 1), "调大后应按候补顺序递补丙");
    assertConsistent(grow.getBody(), "调座回包");
    assertConsistent(detail(session.id()), "调座后详情");
  }

  @Test
  @DisplayName("无效场次：详情、报名、取消、调座、上下架均返回 404")
  void invalidSessionReturns404() {
    long missing = 999_999L;
    assertEquals(HttpStatus.NOT_FOUND,
        rest.getForEntity("/sessions/" + missing, String.class).getStatusCode(), "详情应返回 404");
    assertEquals(HttpStatus.NOT_FOUND, signup(missing, "甲").getStatusCode(), "报名应返回 404");
    assertEquals(HttpStatus.NOT_FOUND, cancel(missing, "甲").getStatusCode(), "取消应返回 404");
    assertEquals(HttpStatus.NOT_FOUND,
        rest.exchange("/sessions/" + missing, HttpMethod.PATCH,
            new HttpEntity<>(Map.of("seatCount", 3)), String.class).getStatusCode(), "调座应返回 404");
    assertEquals(HttpStatus.NOT_FOUND,
        rest.postForEntity("/sessions/" + missing + "/publish", null, String.class).getStatusCode(), "上架应返回 404");
    assertEquals(HttpStatus.NOT_FOUND,
        rest.postForEntity("/sessions/" + missing + "/unpublish", null, String.class).getStatusCode(), "下架应返回 404");
  }

  @Test
  @DisplayName("非法参数：坏时段、空标题、零座位、空昵称均返回 400")
  void invalidRequestsReturn400() {
    assertEquals(HttpStatus.BAD_REQUEST, rest.postForEntity("/sessions",
        Map.of("title", "x", "sessionDate", TEST_DATE, "timeSlot", "25:99", "seatCount", 6),
        String.class).getStatusCode(), "非法时段应返回 400");
    assertEquals(HttpStatus.BAD_REQUEST, rest.postForEntity("/sessions",
        Map.of("title", " ", "sessionDate", TEST_DATE, "timeSlot", "19:00", "seatCount", 6),
        String.class).getStatusCode(), "空标题应返回 400");
    assertEquals(HttpStatus.BAD_REQUEST, rest.postForEntity("/sessions",
        Map.of("title", "x", "sessionDate", TEST_DATE, "timeSlot", "19:00", "seatCount", 0),
        String.class).getStatusCode(), "零座位应返回 400");

    SessionView session = createSession("参数回归", 2);
    assertEquals(HttpStatus.BAD_REQUEST, rest.postForEntity("/sessions/" + session.id() + "/signups",
        Map.of("playerName", " "), String.class).getStatusCode(), "空昵称应返回 400");
  }

  @Test
  @DisplayName("下架后禁止报名，已有报名与候补保留，重新上架后恢复")
  void unpublishBlocksSignupButKeepsRoster() {
    SessionView session = createSession("下架回归", 1);
    signup(session.id(), "甲");
    signup(session.id(), "乙");

    ResponseEntity<SessionView> off = rest.postForEntity("/sessions/" + session.id() + "/unpublish",
        null, SessionView.class);
    assertEquals("OFF", off.getBody().status(), "下架后状态应为 OFF");
    assertConsistent(off.getBody(), "下架回包");

    assertEquals(HttpStatus.CONFLICT, signup(session.id(), "丙").getStatusCode(), "下架后报名应返回 409");

    SessionView view = detail(session.id());
    assertEquals(List.of("甲"), playerNames(view.confirmed()), "下架不得清退已确认玩家");
    assertEquals(List.of("乙"), playerNames(view.waitlist()), "下架不得清空候补队列");

    ResponseEntity<SessionView> on = rest.postForEntity("/sessions/" + session.id() + "/publish",
        null, SessionView.class);
    assertEquals("OPEN", on.getBody().status(), "重新上架后状态应为 OPEN");
    assertEquals(HttpStatus.CREATED, signup(session.id(), "丙").getStatusCode(), "重新上架后应可报名");
    assertConsistent(detail(session.id()), "重新上架后详情");
  }

  private static Map<String, Object> mapOf3() {
    return Map.of("seatCount", 3);
  }

  private static List<String> playerNames(List<com.generated.ldmurdergame.dto.SignupView> signups) {
    return signups.stream().map(com.generated.ldmurdergame.dto.SignupView::playerName).toList();
  }

  private static List<String> tail(List<com.generated.ldmurdergame.dto.SignupView> signups, int count) {
    return signups.subList(signups.size() - count, signups.size()).stream()
        .map(com.generated.ldmurdergame.dto.SignupView::playerName).toList();
  }
}
