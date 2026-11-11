package com.reqflow.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_sequence")
public class SysSequence {

    @TableId(value = "seq_key", type = IdType.INPUT)
    private String seqKey;
    private String seqDate;
    private Integer seqValue;
}
