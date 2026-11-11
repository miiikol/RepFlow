package com.reqflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalRequest {

    @NotNull(message = "请选择审批结果")
    private Boolean approved;

    private String comment;
}
