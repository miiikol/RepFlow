package com.reqflow.vo;

import com.reqflow.domain.entity.Acceptance;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AcceptanceVO extends Acceptance {

    private String requirementNo;
    private String requirementTitle;
    private String submitterName;
    private String acceptorName;
    private String statusLabel;
}
