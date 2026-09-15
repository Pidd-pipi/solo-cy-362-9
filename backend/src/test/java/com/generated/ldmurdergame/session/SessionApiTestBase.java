package com.generated.ldmurdergame.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.generated.ldmurdergame.dto.CancelResult;
import com.generated.ldmurdergame.dto.SessionView;
import com.generated.ldmurdergame.dto.SignupResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 回归测试基座：随机端口启动完整应用，使用内存 H2（PostgreSQL 兼容模式），
 * 每个用例前清表，保证 `mvn test` 可重复运行、互不依赖。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:session-regression;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
abstract class SessionApiTestBase {

  static final String TEST_DATE = "2099-01-01";

  @Autowired
  protected TestRestTemplate rest;

  @Autowired
  protected JdbcTemplate jdbc;

  @BeforeEach
  void cleanTables() {
    jdbc.update("DELETE FROM session_signups");
    jdbc.update("DELETE FROM game_sessions");
  }

  protected SessionView createSession(String title, int seatCount) {
    ResponseEntity<SessionView> response = rest.postForEntity("/sessions",
        Map.of("title", title, "sessionDate", TEST_DATE, "timeSlot", "19:00", "seatCount", seatCount),
        SessionView.class);
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), "开设场次应返回 201");
    return response.getBody();
  }

  protected ResponseEntity<SignupResult> signup(long sessionId, String playerName) {
    return rest.postForEntity("/sessions/" + sessionId + "/signups",
        Map.of("playerName", playerName), SignupResult.class);
  }

  protected ResponseEntity<CancelResult> cancel(long sessionId, String playerName) {
    return rest.postForEntity("/sessions/" + sessionId + "/cancel",
        Map.of("playerName", playerName), CancelResult.class);
  }

  protected SessionView detail(long sessionId) {
    ResponseEntity<SessionView> response = rest.getForEntity("/sessions/" + sessionId, SessionView.class);
    assertEquals(HttpStatus.OK, response.getStatusCode(), "查询场次详情应返回 200");
    return response.getBody();
  }

  protected List<SessionView> listByDate(String date) {
    ResponseEntity<SessionView[]> response = rest.getForEntity("/sessions?date=" + date, SessionView[].class);
    assertEquals(HttpStatus.OK, response.getStatusCode(), "查询场次列表应返回 200");
    return List.of(response.getBody());
  }

  /** 断言视图自洽；失败时输出被违反的具体不变量。 */
  protected static void assertConsistent(SessionView view, String source) {
    List<String> violations = SessionViewInvariants.check(view, source);
    assertTrue(violations.isEmpty(), () -> "场次视图违反不变量:\n" + String.join("\n", violations));
  }

  protected static void assertNoViolations(List<String> violations) {
    assertTrue(violations.isEmpty(),
        () -> "出现 " + violations.size() + " 处不一致:\n" + String.join("\n", violations));
  }
}
