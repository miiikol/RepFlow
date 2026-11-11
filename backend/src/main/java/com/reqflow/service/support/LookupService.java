package com.reqflow.service.support;

import com.reqflow.domain.entity.*;
import com.reqflow.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 轻量名称查询。演示数据规模小，统一加载字典表后内存映射，避免列表查询 N+1。
 */
@Service
@RequiredArgsConstructor
public class LookupService {

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final ProjectMapper projectMapper;
    private final MilestoneMapper milestoneMapper;
    private final SysRoleMapper roleMapper;

    /** 一次请求内复用同一份字典快照，避免详情页重复全表扫描 */
    public record Snapshot(Map<Long, String> users, Map<Long, String> depts,
                           Map<Long, String> projects, Map<Long, String> milestones) {
    }

    public Snapshot snapshot() {
        return new Snapshot(userNames(), deptNames(), projectNames(), milestoneNames());
    }

    public Map<Long, String> userNames() {
        Map<Long, String> map = new HashMap<>();
        for (SysUser user : userMapper.selectList(null)) {
            map.put(user.getId(), user.getRealName());
        }
        return map;
    }

    public Map<Long, String> deptNames() {
        Map<Long, String> map = new HashMap<>();
        for (SysDept dept : deptMapper.selectList(null)) {
            map.put(dept.getId(), dept.getName());
        }
        return map;
    }

    public Map<Long, String> projectNames() {
        Map<Long, String> map = new HashMap<>();
        for (Project project : projectMapper.selectList(null)) {
            map.put(project.getId(), project.getName());
        }
        return map;
    }

    public Map<Long, String> milestoneNames() {
        Map<Long, String> map = new HashMap<>();
        for (Milestone milestone : milestoneMapper.selectList(null)) {
            map.put(milestone.getId(), milestone.getName());
        }
        return map;
    }

    public String userName(Long id) {
        if (id == null) {
            return null;
        }
        SysUser user = userMapper.selectById(id);
        return user == null ? null : user.getRealName();
    }

    public String deptName(Long id) {
        if (id == null) {
            return null;
        }
        SysDept dept = deptMapper.selectById(id);
        return dept == null ? null : dept.getName();
    }

    public String projectName(Long id) {
        if (id == null) {
            return null;
        }
        Project project = projectMapper.selectById(id);
        return project == null ? null : project.getName();
    }

    public String milestoneName(Long id) {
        if (id == null) {
            return null;
        }
        Milestone milestone = milestoneMapper.selectById(id);
        return milestone == null ? null : milestone.getName();
    }

    public List<SysUser> users() {
        return userMapper.selectList(null);
    }

    public List<SysDept> depts() {
        return deptMapper.selectList(null);
    }

    public List<Project> projects() {
        return projectMapper.selectList(null);
    }

    public List<Milestone> milestones() {
        return milestoneMapper.selectList(null);
    }

    public List<SysRole> roles() {
        return roleMapper.selectList(null);
    }
}
