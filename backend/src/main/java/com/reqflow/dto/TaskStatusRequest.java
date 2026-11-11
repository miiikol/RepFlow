package com.reqflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TaskStatusRequest {

    @NotBlank(message = "任务状态不能为空")
    private String status;

    private BigDecimal actualHours;
    private String finishNote;
    private String blockReason;
}
