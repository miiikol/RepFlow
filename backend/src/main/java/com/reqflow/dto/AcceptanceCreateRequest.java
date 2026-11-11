package com.reqflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AcceptanceCreateRequest {

    @NotNull(message = "需求不能为空")
    private Long requirementId;

    @NotNull(message = "请选择验收人")
    private Long acceptorId;

    private String comment;
}
