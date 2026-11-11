package com.reqflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MilestoneCreateRequest {

    @NotNull(message = "项目不能为空")
    private Long projectId;

    @NotBlank(message = "里程碑名称不能为空")
    private String name;

    private LocalDate planDate;
}
