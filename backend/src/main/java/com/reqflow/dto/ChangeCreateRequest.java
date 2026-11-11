package com.reqflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ChangeCreateRequest {

    @NotNull(message = "需求不能为空")
    private Long requirementId;

    @NotBlank(message = "变更原因不能为空")
    private String reason;

    @NotBlank(message = "变更内容不能为空")
    private String content;

    private BigDecimal impactHours;
    private String impactDesc;
}
