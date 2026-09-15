package com.generated.ldmurdergame.dto;

import java.time.LocalDateTime;

/** 报名记录视图，position 为在所属队列（已确认/候补）中的顺序，从 1 开始。 */
public record SignupView(long id, String playerName, String status, int position, LocalDateTime createdAt) {
}
