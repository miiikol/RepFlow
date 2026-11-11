package com.reqflow.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RequirementDetailVO {

    private RequirementVO requirement;
    private List<ReviewVO> reviews;
    private List<ChangeVO> changes;
    private List<TaskVO> tasks;
    private List<DeliverableVO> deliverables;
    private List<AcceptanceVO> acceptances;
    private List<VersionVO> versions;
    private List<AttachmentVO> attachments;
    private List<Map<String, Object>> timeline;
    private List<String> availableActions;
}
