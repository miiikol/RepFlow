package com.reqflow.vo;

import com.reqflow.domain.entity.ReviewOpinion;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReviewOpinionVO extends ReviewOpinion {

    private String reviewerName;
    private String resultLabel;
}
