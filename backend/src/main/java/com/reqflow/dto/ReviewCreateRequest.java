package com.reqflow.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewCreateRequest {

    @NotNull(message = "需求不能为空")
    private Long requirementId;

    private String type = "REQUIREMENT";
    private String method = "MEETING";
    private Long hostId;

    @NotEmpty(message = "请选择评审人")
    private List<Long> reviewerIds;

    private LocalDateTime deadline;
    private String comment;
}
