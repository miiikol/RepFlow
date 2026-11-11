package com.reqflow.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("deliverable")
public class Deliverable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String deliverableNo;
    private Long requirementId;

    @TableField("deliverable_type")
    private String type;

    private String name;
    private String fileName;
    private String storedName;
    private Long fileSize;
    private String description;
    private Long uploaderId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
