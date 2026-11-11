package com.reqflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reqflow.domain.entity.Task;
import org.apache.ibatis.annotations.Select;

public interface TaskMapper extends BaseMapper<Task> {

    @Select("""
            select count(*) from task
            where deleted = 0 and status not in ('DONE', 'CANCELLED')
              and plan_date is not null and plan_date < CURRENT_DATE
            """)
    long countOverdue();

    @Select("""
            select count(*) from task
            where deleted = 0 and status not in ('DONE', 'CANCELLED')
              and plan_date is not null and plan_date < CURRENT_DATE and assignee_id = #{userId}
            """)
    long countOverdueByUser(Long userId);
}
