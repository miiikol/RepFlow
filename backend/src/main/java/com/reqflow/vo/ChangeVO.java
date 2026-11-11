package com.reqflow.vo;

import com.reqflow.domain.entity.ChangeRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChangeVO extends ChangeRequest {

    private String requirementNo;
    private String requirementTitle;
    private String applicantName;
    private String approverName;
    private String statusLabel;
}
