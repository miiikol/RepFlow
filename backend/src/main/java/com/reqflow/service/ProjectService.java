package com.reqflow.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.reqflow.common.BizException;
import com.reqflow.common.ErrorCode;
import com.reqflow.domain.entity.Milestone;
import com.reqflow.domain.entity.Project;
import com.reqflow.dto.MilestoneCreateRequest;
import com.reqflow.dto.ProjectCreateRequest;
import com.reqflow.mapper.MilestoneMapper;
import com.reqflow.mapper.ProjectMapper;
import com.reqflow.service.support.BizNoGenerator;
import com.reqflow.service.support.LookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final MilestoneMapper milestoneMapper;
    private final BizNoGenerator bizNoGenerator;
    private final LookupService lookupService;

    public List<Map<String, Object>> list() {
        Map<Long, String> users = lookupService.userNames();
        Map<Long, String> depts = lookupService.deptNames();
        List<Milestone> milestones = milestoneMapper.selectList(new QueryWrapper<Milestone>().orderByAsc("plan_date"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Project project : projectMapper.selectList(new QueryWrapper<Project>().orderByDesc("id"))) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", project.getId());
            map.put("projectNo", project.getProjectNo());
            map.put("name", project.getName());
            map.put("deptId", project.getDeptId());
            map.put("deptName", depts.get(project.getDeptId()));
            map.put("pmId", project.getPmId());
            map.put("pmName", users.get(project.getPmId()));
            map.put("status", project.getStatus());
            map.put("startDate", project.getStartDate());
            map.put("endDate", project.getEndDate());
            map.put("milestones", milestones.stream()
                    .filter(m -> m.getProjectId().equals(project.getId()))
                    .map(m -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", m.getId());
                        item.put("name", m.getName());
                        item.put("planDate", m.getPlanDate());
                        item.put("actualDate", m.getActualDate());
                        item.put("status", m.getStatus());
                        return item;
                    }).toList());
            result.add(map);
        }
        return result;
    }

    @Transactional
    public Long create(ProjectCreateRequest request) {
        Project project = new Project();
        project.setProjectNo(bizNoGenerator.next("PRJ"));
        project.setName(request.getName());
        project.setDeptId(request.getDeptId());
        project.setPmId(request.getPmId());
        project.setStatus("PLANNING");
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        projectMapper.insert(project);
        return project.getId();
    }

    @Transactional
    public Long createMilestone(MilestoneCreateRequest request) {
        Project project = projectMapper.selectById(request.getProjectId());
        if (project == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "项目不存在");
        }
        Milestone milestone = new Milestone();
        milestone.setProjectId(request.getProjectId());
        milestone.setName(request.getName());
        milestone.setPlanDate(request.getPlanDate());
        milestone.setStatus("TODO");
        milestoneMapper.insert(milestone);
        return milestone.getId();
    }
}
