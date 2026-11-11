package com.reqflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectCreateRequest {

    @NotBlank(message = "项目名称不能为空")
    private String name;

    @NotNull(message = "所属部门不能为空")
    private Long deptId;

    @NotNull(message = "项目经理不能为空")
    private Long pmId;

    private LocalDate startDate;
    private LocalDate endDate;
}
