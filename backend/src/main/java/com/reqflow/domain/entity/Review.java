package com.reqflow.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String reviewNo;
    private Long requirementId;

    @TableField("review_type")
    private String type;
    @TableField("review_method")
    private String method;

    private Long hostId;
    private LocalDateTime deadline;
    private String status;
    private String conclusion;

    @TableField("comment_text")
    private String comment;

    private String snapshotJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
}
