package com.reqflow.controller;

import com.reqflow.common.PageResult;
import com.reqflow.common.Result;
import com.reqflow.domain.entity.AuditLog;
import com.reqflow.dto.UserSaveRequest;
import com.reqflow.service.AdminService;
import com.reqflow.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('admin:user')")
    public Result<List<UserVO>> users() {
        return Result.ok(adminService.users());
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('admin:user')")
    public Result<Long> createUser(@Valid @RequestBody UserSaveRequest request) {
        return Result.ok(adminService.createUser(request));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('admin:user')")
    public Result<Void> updateUser(@PathVariable Long id, @Valid @RequestBody UserSaveRequest request) {
        adminService.updateUser(id, request);
        return Result.ok();
    }

    @PostMapping("/users/{id}/reset-password")
    @PreAuthorize("hasAuthority('admin:user')")
    public Result<Void> resetPassword(@PathVariable Long id) {
        adminService.resetPassword(id);
        return Result.ok();
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('admin:role')")
    public Result<List<Map<String, Object>>> roles() {
        return Result.ok(adminService.roles());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('admin:role')")
    public Result<List<Map<String, Object>>> permissions() {
        return Result.ok(adminService.permissions());
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAuthority('admin:audit')")
    public Result<PageResult<AuditLog>> auditLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) String objectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return Result.ok(adminService.auditLogs(page, size, username, module, objectType, objectId, start, end));
    }
}
