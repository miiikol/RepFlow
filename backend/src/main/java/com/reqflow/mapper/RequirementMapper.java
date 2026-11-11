package com.reqflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reqflow.domain.entity.Requirement;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface RequirementMapper extends BaseMapper<Requirement> {

    @Select("select status, count(*) as total from requirement where deleted = 0 group by status")
    List<Map<String, Object>> countByStatus();

    @Select("""
            select d.name as name, count(*) as total
            from requirement r left join sys_dept d on d.id = r.dept_id
            where r.deleted = 0 group by d.name order by total desc
            """)
    List<Map<String, Object>> countByDept();
}
