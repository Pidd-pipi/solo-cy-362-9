package com.generated.ldmurdergame.dto;

/** 报名结果：包含最新场次视图，以及本次报名的状态与队列位置。 */
public record SignupResult(SessionView session, String status, int position) {
}
