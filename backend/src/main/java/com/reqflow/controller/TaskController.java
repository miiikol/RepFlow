package com.reqflow.controller;

import com.reqflow.common.PageResult;
import com.reqflow.common.Result;
import com.reqflow.dto.TaskCreateRequest;
import com.reqflow.dto.TaskStatusRequest;
import com.reqflow.service.TaskService;
import com.reqflow.vo.TaskVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @PreAuthorize("hasAuthority('task:view')")
    public Result<PageResult<TaskVO>> list(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "20") long size,
                                           @RequestParam(required = false) Long requirementId,
                                           @RequestParam(required = false) Long assigneeId,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) Long projectId,
                                           @RequestParam(required = false) Boolean mine) {
        return Result.ok(taskService.list(page, size, requirementId, assigneeId, status, projectId, mine));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('task:manage')")
    public Result<Long> create(@Valid @RequestBody TaskCreateRequest request) {
        return Result.ok(taskService.create(request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('task:manage')")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody TaskStatusRequest request) {
        taskService.updateStatus(id, request);
        return Result.ok();
    }
}
