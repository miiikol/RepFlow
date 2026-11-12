package com.reqflow.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评审参与人。
 * <p>
 * 原先用 review.reviewer_ids 逗号分隔存储，无法建索引、无法按人检索；
 * 拆成关联表后，数据权限与“我的评审”都走普通索引查询。
 */
@Data
@TableName("review_participant")
public class ReviewParticipant {

    /** 参与人类型：评审人 */
    public static final String TYPE_REVIEWER = "REVIEWER";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reviewId;
    private Long userId;
    private String participantType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
