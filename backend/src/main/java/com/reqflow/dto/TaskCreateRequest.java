package com.reqflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TaskCreateRequest {

    @NotNull(message = "需求不能为空")
    private Long requirementId;

    private Long milestoneId;
    private Long parentId;

    @NotBlank(message = "任务名称不能为空")
    private String name;

    @NotBlank(message = "任务类型不能为空")
    private String type;

    private Long assigneeId;
    private BigDecimal estimateHours;
    private LocalDate planDate;
}
