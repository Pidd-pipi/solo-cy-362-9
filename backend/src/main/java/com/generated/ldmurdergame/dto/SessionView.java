package com.generated.ldmurdergame.dto;

import java.time.LocalDate;
import java.util.List;

/** 场次视图：余位、候补顺序与状态均由数据库实时计算，页面与接口保持一致。 */
public record SessionView(
    long id,
    String title,
    LocalDate sessionDate,
    String timeSlot,
    int seatCount,
    int confirmedCount,
    int remainingSeats,
    int waitlistCount,
    String status,
    boolean full,
    List<SignupView> confirmed,
    List<SignupView> waitlist) {
}
