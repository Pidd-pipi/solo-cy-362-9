package com.generated.ldmurdergame.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateSessionRequest(
    @NotNull(message = "座位数不能为空") @Min(value = 1, message = "座位数至少为 1") @Max(value = 100, message = "座位数最多为 100") Integer seatCount) {
}
