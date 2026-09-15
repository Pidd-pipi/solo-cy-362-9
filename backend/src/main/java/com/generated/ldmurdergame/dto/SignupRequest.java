package com.generated.ldmurdergame.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @NotBlank(message = "玩家昵称不能为空") @Size(max = 80, message = "玩家昵称最长 80 字") String playerName) {
}
