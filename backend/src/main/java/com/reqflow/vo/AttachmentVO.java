package com.reqflow.vo;

import com.reqflow.domain.entity.RequirementAttachment;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AttachmentVO extends RequirementAttachment {

    private String uploaderName;
}
