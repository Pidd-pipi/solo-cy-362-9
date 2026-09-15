package com.generated.ldmurdergame.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class GameSession {
  private Long id;
  private String title;
  private LocalDate sessionDate;
  private String timeSlot;
  private Integer seatCount;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
