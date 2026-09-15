package com.generated.ldmurdergame.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateSessionRequest(
    @NotBlank(message = "场次标题不能为空") @Size(max = 120, message = "场次标题最长 120 字") String title,
    @NotNull(message = "场次日期不能为空") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate sessionDate,
    @NotBlank(message = "时段不能为空") @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "时段格式应为 HH:mm") String timeSlot,
    @NotNull(message = "座位数不能为空") @Min(value = 1, message = "座位数至少为 1") @Max(value = 100, message = "座位数最多为 100") Integer seatCount) {
}
