package com.reqflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AcceptanceConclusionRequest {

    @NotNull(message = "请选择验收结论")
    private Boolean pass;

    private String comment;
    private String reworkRequirement;
}
