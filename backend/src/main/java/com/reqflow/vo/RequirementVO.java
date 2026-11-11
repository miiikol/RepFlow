package com.reqflow.vo;

import com.reqflow.domain.entity.Requirement;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RequirementVO extends Requirement {

    private String deptName;
    private String proposerName;
    private String analystName;
    private String pmName;
    private String projectName;
    private String milestoneName;
    private String statusLabel;
    private String typeLabel;
    private String sourceLabel;
    private String priorityLabel;
    private int taskTotal;
    private int taskDone;
    private int progress;
    private boolean overdue;
}
