package com.reqflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ActionRequest {

    @NotBlank(message = "动作不能为空")
    private String action;

    private String comment;
    private String holdReason;
    private Long projectId;
    private Long milestoneId;
    private Long acceptorId;
    private LocalDate planDate;
}
