package com.generated.ldmurdergame.controller;

import com.generated.ldmurdergame.dto.CancelResult;
import com.generated.ldmurdergame.dto.CreateSessionRequest;
import com.generated.ldmurdergame.dto.SessionView;
import com.generated.ldmurdergame.dto.SignupRequest;
import com.generated.ldmurdergame.dto.SignupResult;
import com.generated.ldmurdergame.dto.UpdateSessionRequest;
import com.generated.ldmurdergame.service.SessionService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {
  private final SessionService sessionService;

  public SessionController(SessionService sessionService) {
    this.sessionService = sessionService;
  }

  /** 按日期查询场次（默认今天），返回余位、候补队列与状态。 */
  @GetMapping({"/sessions", "/api/sessions"})
  public List<SessionView> list(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return sessionService.listSessions(date);
  }

  @GetMapping({"/sessions/{id}", "/api/sessions/{id}"})
  public SessionView detail(@PathVariable long id) {
    return sessionService.getSession(id);
  }

  /** 开设场次：日期 + 时段 + 座位数，创建后默认上架。 */
  @PostMapping({"/sessions", "/api/sessions"})
  public ResponseEntity<SessionView> create(@Valid @RequestBody CreateSessionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.createSession(request));
  }

  /** 上架：开放报名。 */
  @PostMapping({"/sessions/{id}/publish", "/api/sessions/{id}/publish"})
  public SessionView publish(@PathVariable long id) {
    return sessionService.publish(id);
  }

  /** 下架：停止报名，已有报名与候补保留。 */
  @PostMapping({"/sessions/{id}/unpublish", "/api/sessions/{id}/unpublish"})
  public SessionView unpublish(@PathVariable long id) {
    return sessionService.unpublish(id);
  }

  /** 调整座位数；调大后空位按候补顺序自动递补。 */
  @PatchMapping({"/sessions/{id}", "/api/sessions/{id}"})
  public SessionView updateSeats(@PathVariable long id, @Valid @RequestBody UpdateSessionRequest request) {
    return sessionService.updateSeatCount(id, request.seatCount());
  }

  /** 单人报名：有位直接占位，满员进入候补队列。 */
  @PostMapping({"/sessions/{id}/signups", "/api/sessions/{id}/signups"})
  public ResponseEntity<SignupResult> signup(@PathVariable long id, @Valid @RequestBody SignupRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.signup(id, request));
  }

  /** 取消报名：释放座位并按报名先后自动递补。 */
  @PostMapping({"/sessions/{id}/cancel", "/api/sessions/{id}/cancel"})
  public CancelResult cancel(@PathVariable long id, @Valid @RequestBody SignupRequest request) {
    return sessionService.cancel(id, request);
  }
}
