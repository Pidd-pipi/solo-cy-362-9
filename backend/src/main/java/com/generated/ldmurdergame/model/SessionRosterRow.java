package com.generated.ldmurdergame.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 场次 + 报名的联表平铺行：单条 SQL 返回，保证同一份场次视图来自同一时点快照。 */
public class SessionRosterRow {
  private Long sessionId;
  private String title;
  private LocalDate sessionDate;
  private String timeSlot;
  private Integer seatCount;
  private String sessionStatus;
  private Long signupId;
  private String playerName;
  private String signupStatus;
  private Long signupSeq;
  private LocalDateTime signupCreatedAt;

  public Long getSessionId() {
    return sessionId;
  }

  public void setSessionId(Long sessionId) {
    this.sessionId = sessionId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public LocalDate getSessionDate() {
    return sessionDate;
  }

  public void setSessionDate(LocalDate sessionDate) {
    this.sessionDate = sessionDate;
  }

  public String getTimeSlot() {
    return timeSlot;
  }

  public void setTimeSlot(String timeSlot) {
    this.timeSlot = timeSlot;
  }

  public Integer getSeatCount() {
    return seatCount;
  }

  public void setSeatCount(Integer seatCount) {
    this.seatCount = seatCount;
  }

  public String getSessionStatus() {
    return sessionStatus;
  }

  public void setSessionStatus(String sessionStatus) {
    this.sessionStatus = sessionStatus;
  }

  public Long getSignupId() {
    return signupId;
  }

  public void setSignupId(Long signupId) {
    this.signupId = signupId;
  }

  public String getPlayerName() {
    return playerName;
  }

  public void setPlayerName(String playerName) {
    this.playerName = playerName;
  }

  public String getSignupStatus() {
    return signupStatus;
  }

  public void setSignupStatus(String signupStatus) {
    this.signupStatus = signupStatus;
  }

  public Long getSignupSeq() {
    return signupSeq;
  }

  public void setSignupSeq(Long signupSeq) {
    this.signupSeq = signupSeq;
  }

  public LocalDateTime getSignupCreatedAt() {
    return signupCreatedAt;
  }

  public void setSignupCreatedAt(LocalDateTime signupCreatedAt) {
    this.signupCreatedAt = signupCreatedAt;
  }
}
