package com.reqflow.service;

import com.reqflow.domain.entity.Milestone;
import com.reqflow.domain.entity.Project;
import com.reqflow.domain.entity.SysDept;
import com.reqflow.domain.entity.SysRole;
import com.reqflow.domain.entity.SysUser;
import com.reqflow.mapper.SysPermissionMapper;
import com.reqflow.service.support.DictService;
import com.reqflow.service.support.LookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetaService {

    private final LookupService lookupService;
    private final DictService dictService;
    private final SysPermissionMapper permissionMapper;

    public Map<String, Object> options() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<SysDept> depts = lookupService.depts();
        result.put("depts", depts.stream()
                .map(d -> Map.of("id", d.getId(), "name", d.getName())).toList());
        result.put("users", lookupService.users().stream()
                .map(u -> Map.of("id", u.getId(), "name", u.getRealName(), "username", u.getUsername(),
                        "deptId", u.getDeptId() == null ? 0 : u.getDeptId()))
                .toList());
        List<Milestone> milestones = lookupService.milestones();
        result.put("projects", lookupService.projects().stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("deptId", p.getDeptId());
            map.put("pmId", p.getPmId());
            map.put("status", p.getStatus());
            map.put("milestones", milestones.stream().filter(m -> m.getProjectId().equals(p.getId()))
                    .map(m -> Map.of("id", m.getId(), "name", m.getName(), "projectId", m.getProjectId()))
                    .toList());
            return map;
        }).toList());
        result.put("roles", lookupService.roles().stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("code", r.getCode());
            map.put("name", r.getName());
            map.put("dataScope", r.getDataScope());
            return map;
        }).toList());
        result.put("permissions", permissionMapper.selectList(null).stream()
                .map(p -> Map.of("id", p.getId(), "code", p.getCode(), "name", p.getName(),
                        "module", p.getModule() == null ? "" : p.getModule()))
                .toList());
        result.put("dicts", dictService.all());
        return result;
    }
}
