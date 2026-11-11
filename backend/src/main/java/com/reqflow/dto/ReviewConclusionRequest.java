package com.reqflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewConclusionRequest {

    @NotNull(message = "请选择评审结论")
    private Boolean pass;

    private String comment;
}
