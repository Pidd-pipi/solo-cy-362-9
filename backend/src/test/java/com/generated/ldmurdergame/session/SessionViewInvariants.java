package com.generated.ldmurdergame.session;

import com.generated.ldmurdergame.dto.SessionView;
import com.generated.ldmurdergame.dto.SignupView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 场次视图不变量校验器。
 *
 * <p>同一份场次响应（列表项、详情、写接口回包）必须来自同一时点快照；
 * 任何「座位数与名单来自不同提交时点」的撕裂读取都会违反下列某条不变量，
 * 校验失败信息会指出具体是哪一条。
 */
public final class SessionViewInvariants {

  private SessionViewInvariants() {
  }

  /** 返回全部被违反的不变量描述；空列表表示该视图自洽。 */
  public static List<String> check(SessionView view, String source) {
    List<String> violations = new ArrayList<>();
    String where = "[来源=" + source + ", 场次=" + view.id() + "] ";
    List<SignupView> confirmed = view.confirmed();
    List<SignupView> waitlist = view.waitlist();

    if (confirmed.size() != view.confirmedCount()) {
      violations.add(where + "不变量「确认计数与确认名单等长」被违反: confirmedCount="
          + view.confirmedCount() + ", 实际名单 " + confirmed.size() + " 人 | " + snapshot(view));
    }
    if (waitlist.size() != view.waitlistCount()) {
      violations.add(where + "不变量「候补计数与候补队列等长」被违反: waitlistCount="
          + view.waitlistCount() + ", 实际队列 " + waitlist.size() + " 人 | " + snapshot(view));
    }
    if (confirmed.size() > view.seatCount()) {
      violations.add(where + "不变量「确认人数不超过座位数（不超卖）」被违反: 确认 "
          + confirmed.size() + " 人 > 座位 " + view.seatCount() + " | " + snapshot(view));
    }
    int expectedRemaining = Math.max(0, view.seatCount() - confirmed.size());
    if (view.remainingSeats() != expectedRemaining) {
      violations.add(where + "不变量「余位 = 座位数 - 确认数」被违反: remainingSeats="
          + view.remainingSeats() + ", 应为 " + expectedRemaining + " | " + snapshot(view));
    }
    boolean expectedFull = confirmed.size() >= view.seatCount();
    if (view.full() != expectedFull) {
      violations.add(where + "不变量「满员标记与占位一致」被违反: full="
          + view.full() + ", 应为 " + expectedFull + " | " + snapshot(view));
    }
    if (!positionsSequential(confirmed)) {
      violations.add(where + "不变量「确认名单位次从 1 连续递增」被违反 | " + snapshot(view));
    }
    if (!positionsSequential(waitlist)) {
      violations.add(where + "不变量「候补队列位次从 1 连续递增」被违反 | " + snapshot(view));
    }
    Set<String> confirmedNames = namesOf(confirmed);
    Set<String> waitlistNames = namesOf(waitlist);
    if (confirmedNames.size() != confirmed.size() || waitlistNames.size() != waitlist.size()) {
      violations.add(where + "不变量「同一队列内玩家不重复」被违反 | " + snapshot(view));
    }
    Set<String> both = new HashSet<>(confirmedNames);
    both.retainAll(waitlistNames);
    if (!both.isEmpty()) {
      violations.add(where + "不变量「同一玩家不同时占位与候补」被违反: " + both + " | " + snapshot(view));
    }
    if (!waitlist.isEmpty() && confirmed.size() < view.seatCount()) {
      violations.add(where + "不变量「有候补时必须满员」被违反: 候补 " + waitlist.size()
          + " 人但空位 " + (view.seatCount() - confirmed.size()) + " | " + snapshot(view));
    }
    return violations;
  }

  /** 打印场次快照，用于失败信息定位。 */
  public static String snapshot(SessionView view) {
    return "快照{座位=" + view.seatCount()
        + ", 确认=" + namesOf(view.confirmed())
        + ", 候补=" + namesOf(view.waitlist())
        + ", 余位=" + view.remainingSeats()
        + ", 满员=" + view.full()
        + ", 状态=" + view.status() + "}";
  }

  private static boolean positionsSequential(List<SignupView> queue) {
    for (int i = 0; i < queue.size(); i++) {
      if (queue.get(i).position() != i + 1) {
        return false;
      }
    }
    return true;
  }

  private static Set<String> namesOf(List<SignupView> queue) {
    Set<String> names = new HashSet<>();
    for (SignupView signup : queue) {
      names.add(signup.playerName());
    }
    return names;
  }
}
