package com.reqflow.vo;

import com.reqflow.domain.entity.RequirementVersion;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VersionVO extends RequirementVersion {

    private String operatorName;
}
