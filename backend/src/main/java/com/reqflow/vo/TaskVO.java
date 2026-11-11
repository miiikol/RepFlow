package com.reqflow.vo;

import com.reqflow.domain.entity.Task;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TaskVO extends Task {

    private String assigneeName;
    private String requirementNo;
    private String requirementTitle;
    private String statusLabel;
    private String typeLabel;
    private boolean overdue;
}
