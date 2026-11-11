package com.reqflow.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reqflow.common.BizException;
import com.reqflow.common.ErrorCode;
import com.reqflow.common.PageResult;
import com.reqflow.common.SecurityUtils;
import com.reqflow.domain.entity.AuditLog;
import com.reqflow.domain.entity.SysPermission;
import com.reqflow.domain.entity.SysRole;
import com.reqflow.domain.entity.SysRolePermission;
import com.reqflow.domain.entity.SysUser;
import com.reqflow.domain.entity.SysUserRole;
import com.reqflow.dto.UserSaveRequest;
import com.reqflow.mapper.AuditLogMapper;
import com.reqflow.mapper.SysPermissionMapper;
import com.reqflow.mapper.SysRoleMapper;
import com.reqflow.mapper.SysRolePermissionMapper;
import com.reqflow.mapper.SysUserMapper;
import com.reqflow.mapper.SysUserRoleMapper;
import com.reqflow.service.support.LookupService;
import com.reqflow.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final AuditLogMapper auditLogMapper;
    private final LookupService lookupService;
    private final PasswordEncoder passwordEncoder;

    public List<UserVO> users() {
        Map<Long, String> depts = lookupService.deptNames();
        return userMapper.selectList(new QueryWrapper<SysUser>().orderByAsc("id")).stream().map(user -> {
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
            vo.setDeptId(user.getDeptId());
            vo.setDeptName(depts.get(user.getDeptId()));
            vo.setEmail(user.getEmail());
            vo.setPhone(user.getPhone());
            vo.setStatus(user.getStatus());
            List<SysRole> roles = userMapper.selectRoles(user.getId());
            vo.setRoles(roles.stream().map(SysRole::getCode).toList());
            vo.setRoleIds(roles.stream().map(SysRole::getId).toList());
            return vo;
        }).toList();
    }

    @Transactional
    public Long createUser(UserSaveRequest request) {
        SysUser exists = userMapper.selectByUsername(request.getUsername());
        if (exists != null) {
            throw new BizException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setRealName(request.getRealName());
        user.setDeptId(request.getDeptId());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        user.setMustChangePwd(1);
        user.setFailCount(0);
        String password = StringUtils.hasText(request.getPassword()) ? request.getPassword() : "Reqflow@123";
        user.setPassword(passwordEncoder.encode(password));
        userMapper.insert(user);
        assignRoles(user.getId(), request.getRoleIds());
        return user.getId();
    }

    @Transactional
    public void updateUser(Long id, UserSaveRequest request) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (id.equals(SecurityUtils.userId()) && request.getStatus() != null && request.getStatus() == 0) {
            throw new BizException("不能停用自己的账号");
        }
        user.setRealName(request.getRealName());
        user.setDeptId(request.getDeptId());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        userMapper.updateById(user);
        if (request.getRoleIds() != null) {
            assignRoles(id, request.getRoleIds());
        }
    }

    @Transactional
    public void resetPassword(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "用户不存在");
        }
        user.setPassword(passwordEncoder.encode("Reset@12345"));
        user.setMustChangePwd(1);
        user.setFailCount(0);
        user.setLockUntil(null);
        userMapper.updateById(user);
    }

    private void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new QueryWrapper<SysUserRole>().eq("user_id", userId));
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        }
    }

    public List<Map<String, Object>> roles() {
        List<SysRole> roles = roleMapper.selectList(null);
        List<SysRolePermission> relations = rolePermissionMapper.selectList(null);
        Map<Long, List<Long>> permissionMap = relations.stream()
                .collect(Collectors.groupingBy(SysRolePermission::getRoleId,
                        Collectors.mapping(SysRolePermission::getPermissionId, Collectors.toList())));
        List<Map<String, Object>> list = new ArrayList<>();
        for (SysRole role : roles) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", role.getId());
            map.put("code", role.getCode());
            map.put("name", role.getName());
            map.put("dataScope", role.getDataScope());
            map.put("permissionIds", permissionMap.getOrDefault(role.getId(), List.of()));
            list.add(map);
        }
        return list;
    }

    public List<Map<String, Object>> permissions() {
        return permissionMapper.selectList(null).stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("code", p.getCode());
            map.put("name", p.getName());
            map.put("module", p.getModule());
            return map;
        }).toList();
    }

    public PageResult<AuditLog> auditLogs(long page, long size, String username, String module,
                                          String objectType, String objectId,
                                          LocalDateTime start, LocalDateTime end) {
        QueryWrapper<AuditLog> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like("username", username);
        }
        if (StringUtils.hasText(module)) {
            wrapper.eq("module", module);
        }
        if (StringUtils.hasText(objectType)) {
            wrapper.eq("object_type", objectType);
        }
        if (StringUtils.hasText(objectId)) {
            wrapper.eq("object_id", objectId);
        }
        if (start != null) {
            wrapper.ge("created_at", start);
        }
        if (end != null) {
            wrapper.le("created_at", end);
        }
        wrapper.orderByDesc("created_at");
        Page<AuditLog> result = auditLogMapper.selectPage(new Page<>(page, Math.min(size, 100)), wrapper);
        return PageResult.of(result.getTotal(), page, result.getSize(), result.getRecords());
    }
}
