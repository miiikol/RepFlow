package com.reqflow.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("acceptance")
public class Acceptance {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String acceptanceNo;
    private Long requirementId;

    @TableField("round_no")
    private Integer round;

    private Long submitterId;
    private Long acceptorId;
    private String deliverableSnapshot;
    private String status;
    private String conclusion;

    @TableField("comment_text")
    private String comment;

    private String reworkRequirement;
    private LocalDateTime submittedAt;
    private LocalDateTime confirmedAt;
}
