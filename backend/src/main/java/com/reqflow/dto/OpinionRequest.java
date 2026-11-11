package com.reqflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OpinionRequest {

    @NotBlank(message = "意见类型不能为空")
    private String result;

    @NotBlank(message = "意见内容不能为空")
    private String content;
}
