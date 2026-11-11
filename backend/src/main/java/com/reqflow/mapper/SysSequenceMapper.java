package com.reqflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reqflow.domain.entity.SysSequence;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface SysSequenceMapper extends BaseMapper<SysSequence> {

    /** 原子自增，返回影响行数；行锁保证并发下不会出现重复值 */
    @Update("update sys_sequence set seq_value = seq_value + 1 where seq_key = #{key}")
    int increment(@Param("key") String key);

    @Select("select seq_value from sys_sequence where seq_key = #{key}")
    Integer selectValue(@Param("key") String key);
}
