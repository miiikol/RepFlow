package com.reqflow.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("change_request")
public class ChangeRequest {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String changeNo;
    private Long requirementId;
    private String reason;
    private String content;
    private BigDecimal impactHours;
    private String impactDesc;
    private String status;
    private Long applicantId;
    private Long approverId;
    private String approveComment;
    private LocalDateTime approvedAt;
    private LocalDateTime appliedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
