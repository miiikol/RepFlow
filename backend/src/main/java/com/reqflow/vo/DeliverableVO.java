package com.reqflow.vo;

import com.reqflow.domain.entity.Deliverable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeliverableVO extends Deliverable {

    private String uploaderName;
    private String typeLabel;
}
