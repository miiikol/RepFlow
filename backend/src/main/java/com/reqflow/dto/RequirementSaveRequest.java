package com.reqflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RequirementSaveRequest {

    @NotBlank(message = "需求标题不能为空")
    @Size(max = 60, message = "需求标题不能超过 60 字")
    private String title;

    @NotBlank(message = "需求类型不能为空")
    private String type;

    @NotBlank(message = "需求来源不能为空")
    private String source;

    @NotNull(message = "业务部门不能为空")
    private Long deptId;

    @NotBlank(message = "优先级不能为空")
    private String priority;

    @NotNull(message = "期望交付日期不能为空")
    private LocalDate expectDate;

    @NotBlank(message = "业务背景不能为空")
    private String background;

    @NotBlank(message = "需求描述不能为空")
    private String description;

    @NotBlank(message = "验收标准不能为空")
    private String acceptanceCriteria;

    private LocalDate planDate;
    private Long analystId;
    private Long pmId;
    private Long projectId;
    private Long milestoneId;
    private String tags;
}
