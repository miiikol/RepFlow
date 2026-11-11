package com.reqflow.controller;

import com.reqflow.audit.Audit;
import com.reqflow.common.Result;
import com.reqflow.dto.MilestoneCreateRequest;
import com.reqflow.dto.ProjectCreateRequest;
import com.reqflow.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @PreAuthorize("hasAuthority('project:view')")
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(projectService.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('project:manage')")
    @Audit(module = "计划任务", action = "新建项目", objectType = "PROJECT")
    public Result<Long> create(@Valid @RequestBody ProjectCreateRequest request) {
        return Result.ok(projectService.create(request));
    }

    @PostMapping("/milestones")
    @PreAuthorize("hasAuthority('project:manage')")
    @Audit(module = "计划任务", action = "新建里程碑", objectType = "PROJECT")
    public Result<Long> createMilestone(@Valid @RequestBody MilestoneCreateRequest request) {
        return Result.ok(projectService.createMilestone(request));
    }
}
