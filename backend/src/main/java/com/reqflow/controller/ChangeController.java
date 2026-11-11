package com.reqflow.controller;

import com.reqflow.common.PageResult;
import com.reqflow.common.Result;
import com.reqflow.dto.ApprovalRequest;
import com.reqflow.dto.ChangeCreateRequest;
import com.reqflow.service.ChangeService;
import com.reqflow.vo.ChangeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/changes")
@RequiredArgsConstructor
public class ChangeController {

    private final ChangeService changeService;

    @GetMapping
    @PreAuthorize("hasAuthority('change:view')")
    public Result<PageResult<ChangeVO>> list(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "20") long size,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) Long requirementId) {
        return Result.ok(changeService.list(page, size, status, requirementId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('change:create')")
    public Result<Long> create(@Valid @RequestBody ChangeCreateRequest request) {
        return Result.ok(changeService.create(request));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('change:approve')")
    public Result<Void> approve(@PathVariable Long id, @Valid @RequestBody ApprovalRequest request) {
        changeService.approve(id, request);
        return Result.ok();
    }

    @PostMapping("/{id}/apply")
    @PreAuthorize("hasAuthority('change:apply')")
    public Result<Void> apply(@PathVariable Long id) {
        changeService.apply(id);
        return Result.ok();
    }
}
