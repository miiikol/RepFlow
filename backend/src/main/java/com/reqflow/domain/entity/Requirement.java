package com.reqflow.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("requirement")
public class Requirement {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String reqNo;
    private String title;

    @TableField("req_type")
    private String type;
    @TableField("req_source")
    private String source;

    private Long deptId;
    private Long proposerId;
    private Long analystId;
    private Long pmId;
    private String priority;
    private LocalDate expectDate;
    private LocalDate planDate;

    @TableField("business_background")
    private String background;
    @TableField("req_description")
    private String description;
    private String acceptanceCriteria;

    private String status;
    private String holdFromStatus;

    @TableField("version_no")
    private String version;
    private Integer changeCount;

    /** 乐观锁版本，仅用于并发更新控制，与业务版本号 version 无关 */
    @Version
    private Integer lockVersion;

    private Long projectId;
    private Long milestoneId;
    private String tags;
    private LocalDateTime archivedAt;
    private Long archivedBy;
    private String rejectReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableLogic
    private Integer deleted;
}
