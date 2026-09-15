package com.generated.ldmurdergame.dto;

import java.util.List;

/** 取消结果：包含最新场次视图，以及本次退位后按报名先后自动递补上车的玩家。 */
public record CancelResult(SessionView session, List<String> promoted) {
}
