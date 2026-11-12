package com.reqflow.vo;

import com.reqflow.domain.entity.Review;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReviewVO extends Review {

    private String requirementNo;
    private String requirementTitle;
    private String hostName;
    /** 评审人 ID 列表，来自 review_participant */
    private List<Long> reviewerIds;
    private String reviewerNames;
    private String statusLabel;
    private List<ReviewOpinionVO> opinions;
}
