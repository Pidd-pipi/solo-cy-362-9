package com.generated.ldmurdergame.service;

import com.generated.ldmurdergame.dto.CancelResult;
import com.generated.ldmurdergame.dto.CreateSessionRequest;
import com.generated.ldmurdergame.dto.SessionView;
import com.generated.ldmurdergame.dto.SignupRequest;
import com.generated.ldmurdergame.dto.SignupResult;
import com.generated.ldmurdergame.dto.SignupView;
import com.generated.ldmurdergame.exception.ApiException;
import com.generated.ldmurdergame.mapper.GameSessionMapper;
import com.generated.ldmurdergame.mapper.SessionSignupMapper;
import com.generated.ldmurdergame.model.GameSession;
import com.generated.ldmurdergame.model.SessionRosterRow;
import com.generated.ldmurdergame.model.SessionSignup;
import com.generated.ldmurdergame.model.SessionStatuses;
import com.generated.ldmurdergame.model.SignupStatuses;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {
  private final GameSessionMapper sessionMapper;
  private final SessionSignupMapper signupMapper;

  public SessionService(GameSessionMapper sessionMapper, SessionSignupMapper signupMapper) {
    this.sessionMapper = sessionMapper;
    this.signupMapper = signupMapper;
  }

  @Transactional(readOnly = true)
  public List<SessionView> listSessions(LocalDate date) {
    LocalDate effectiveDate = date != null ? date : LocalDate.now();
    // 单条联表查询 = 同一时点快照，避免多场次的行与名单分别读取导致撕裂
    Map<Long, List<SessionRosterRow>> rowsBySession = new LinkedHashMap<>();
    for (SessionRosterRow row : sessionMapper.findRosterByDate(effectiveDate)) {
      rowsBySession.computeIfAbsent(row.getSessionId(), key -> new ArrayList<>()).add(row);
    }
    return rowsBySession.values().stream().map(this::toViewFromRows).toList();
  }

  @Transactional(readOnly = true)
  public SessionView getSession(long sessionId) {
    List<SessionRosterRow> rows = sessionMapper.findRosterById(sessionId);
    if (rows.isEmpty()) {
      throw new ApiException(HttpStatus.NOT_FOUND, "场次不存在");
    }
    return toViewFromRows(rows);
  }

  /** 开设场次：按日期 + 时段创建，默认直接上架开放报名。 */
  @Transactional
  public SessionView createSession(CreateSessionRequest request) {
    GameSession session = new GameSession();
    session.setTitle(request.title().trim());
    session.setSessionDate(request.sessionDate());
    session.setTimeSlot(request.timeSlot());
    session.setSeatCount(request.seatCount());
    session.setStatus(SessionStatuses.OPEN);
    sessionMapper.insert(session);
    return toView(sessionMapper.findById(session.getId()));
  }

  @Transactional
  public SessionView publish(long sessionId) {
    lockSession(sessionId);
    sessionMapper.updateStatus(sessionId, SessionStatuses.OPEN);
    return toView(sessionMapper.findById(sessionId));
  }

  @Transactional
  public SessionView unpublish(long sessionId) {
    lockSession(sessionId);
    sessionMapper.updateStatus(sessionId, SessionStatuses.OFF);
    return toView(sessionMapper.findById(sessionId));
  }

  /** 调整座位数：不得小于已确认人数；调大后立即按候补顺序递补。 */
  @Transactional
  public SessionView updateSeatCount(long sessionId, int seatCount) {
    lockSession(sessionId);
    int confirmed = signupMapper.countConfirmed(sessionId);
    if (seatCount < confirmed) {
      throw new ApiException(HttpStatus.CONFLICT, "座位数不能小于已确认人数（当前 " + confirmed + " 人）");
    }
    sessionMapper.updateSeatCount(sessionId, seatCount);
    promoteWaitlist(sessionId, seatCount);
    return toView(sessionMapper.findById(sessionId));
  }

  /**
   * 报名：锁定场次行后判断余位，有位则占位、满员则进入候补队尾。
   * 同一玩家在同一场次只允许一条有效报名（CONFIRMED/WAITLIST），取消后可再次报名。
   */
  @Transactional
  public SignupResult signup(long sessionId, SignupRequest request) {
    GameSession session = lockSession(sessionId);
    if (!SessionStatuses.OPEN.equals(session.getStatus())) {
      throw new ApiException(HttpStatus.CONFLICT, "场次已下架，暂时无法报名");
    }
    String playerName = request.playerName().trim();
    SessionSignup existing = signupMapper.findBySessionAndPlayer(sessionId, playerName);
    if (existing != null && !SignupStatuses.CANCELLED.equals(existing.getStatus())) {
      throw new ApiException(HttpStatus.CONFLICT, "该昵称已在此场次报名，不能重复占位");
    }

    int confirmedCount = signupMapper.countConfirmed(sessionId);
    String status = confirmedCount < session.getSeatCount()
        ? SignupStatuses.CONFIRMED
        : SignupStatuses.WAITLIST;
    long seq = signupMapper.maxSeq(sessionId) + 1;
    if (existing == null) {
      SessionSignup signup = new SessionSignup();
      signup.setSessionId(sessionId);
      signup.setPlayerName(playerName);
      signup.setStatus(status);
      signup.setSeq(seq);
      signupMapper.insert(signup);
    } else {
      // 取消后再次报名：复用记录，以新序号排到队尾
      signupMapper.updateStatusAndSeq(existing.getId(), status, seq);
    }

    SessionView view = toView(sessionMapper.findById(sessionId));
    return new SignupResult(view, status, positionOf(view, playerName, status));
  }

  /** 取消报名：释放座位后按报名先后自动递补候补玩家。 */
  @Transactional
  public CancelResult cancel(long sessionId, SignupRequest request) {
    GameSession session = lockSession(sessionId);
    String playerName = request.playerName().trim();
    SessionSignup existing = signupMapper.findBySessionAndPlayer(sessionId, playerName);
    if (existing == null || SignupStatuses.CANCELLED.equals(existing.getStatus())) {
      throw new ApiException(HttpStatus.NOT_FOUND, "该昵称在此场次没有有效报名");
    }

    boolean freedSeat = SignupStatuses.CONFIRMED.equals(existing.getStatus());
    signupMapper.updateStatusAndSeq(existing.getId(), SignupStatuses.CANCELLED, existing.getSeq());

    List<String> promoted = List.of();
    if (freedSeat) {
      promoted = promoteWaitlist(sessionId, session.getSeatCount());
    }
    return new CancelResult(toView(sessionMapper.findById(sessionId)), promoted);
  }

  /** 锁定场次行，串行化该场次的一切写操作。 */
  private GameSession lockSession(long sessionId) {
    GameSession session = sessionMapper.lockById(sessionId);
    if (session == null) {
      throw new ApiException(HttpStatus.NOT_FOUND, "场次不存在");
    }
    return session;
  }

  /** 在持有场次行锁的前提下，把空出的座位按候补顺序（seq 升序）补满。 */
  private List<String> promoteWaitlist(long sessionId, int seatCount) {
    List<String> promoted = new ArrayList<>();
    while (signupMapper.countConfirmed(sessionId) < seatCount) {
      SessionSignup next = signupMapper.firstWaiting(sessionId);
      if (next == null) {
        break;
      }
      signupMapper.updateStatusAndSeq(next.getId(), SignupStatuses.CONFIRMED, next.getSeq());
      promoted.add(next.getPlayerName());
    }
    return promoted;
  }

  private SessionView toView(GameSession session) {
    List<SessionSignup> confirmed = signupMapper.findConfirmed(session.getId());
    List<SessionSignup> waitlist = signupMapper.findWaitlist(session.getId());
    int remaining = Math.max(0, session.getSeatCount() - confirmed.size());
    return new SessionView(
        session.getId(),
        session.getTitle(),
        session.getSessionDate(),
        session.getTimeSlot(),
        session.getSeatCount(),
        confirmed.size(),
        remaining,
        waitlist.size(),
        session.getStatus(),
        confirmed.size() >= session.getSeatCount(),
        toSignupViews(confirmed),
        toSignupViews(waitlist));
  }

  /** 由单条联表查询的平铺行组装视图；同批行来自同一快照，天然自洽。 */
  private SessionView toViewFromRows(List<SessionRosterRow> rows) {
    SessionRosterRow first = rows.get(0);
    List<SignupView> confirmed = new ArrayList<>();
    List<SignupView> waitlist = new ArrayList<>();
    for (SessionRosterRow row : rows) {
      if (row.getSignupId() == null) {
        continue;
      }
      SignupView view = new SignupView(
          row.getSignupId(),
          row.getPlayerName(),
          row.getSignupStatus(),
          0,
          row.getSignupCreatedAt());
      if (SignupStatuses.CONFIRMED.equals(row.getSignupStatus())) {
        confirmed.add(view);
      } else {
        waitlist.add(view);
      }
    }
    return new SessionView(
        first.getSessionId(),
        first.getTitle(),
        first.getSessionDate(),
        first.getTimeSlot(),
        first.getSeatCount(),
        confirmed.size(),
        Math.max(0, first.getSeatCount() - confirmed.size()),
        waitlist.size(),
        first.getSessionStatus(),
        confirmed.size() >= first.getSeatCount(),
        withPositions(confirmed),
        withPositions(waitlist));
  }

  private List<SignupView> withPositions(List<SignupView> views) {
    List<SignupView> positioned = new ArrayList<>(views.size());
    for (int i = 0; i < views.size(); i++) {
      SignupView view = views.get(i);
      positioned.add(new SignupView(view.id(), view.playerName(), view.status(), i + 1, view.createdAt()));
    }
    return positioned;
  }

  private List<SignupView> toSignupViews(List<SessionSignup> signups) {
    List<SignupView> views = new ArrayList<>(signups.size());
    for (int i = 0; i < signups.size(); i++) {
      SessionSignup signup = signups.get(i);
      views.add(new SignupView(signup.getId(), signup.getPlayerName(), signup.getStatus(), i + 1, signup.getCreatedAt()));
    }
    return views;
  }

  private int positionOf(SessionView view, String playerName, String status) {
    List<SignupView> queue = SignupStatuses.CONFIRMED.equals(status) ? view.confirmed() : view.waitlist();
    for (SignupView signup : queue) {
      if (signup.playerName().equals(playerName)) {
        return signup.position();
      }
    }
    return 0;
  }
}
