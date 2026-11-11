package com.reqflow.controller;

import com.reqflow.common.Result;
import com.reqflow.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('requirement:view')")
    public Result<Map<String, Object>> summary() {
        return Result.ok(dashboardService.summary());
    }

    @GetMapping("/status-distribution")
    @PreAuthorize("hasAuthority('requirement:view')")
    public Result<List<Map<String, Object>>> statusDistribution() {
        return Result.ok(dashboardService.statusDistribution());
    }

    @GetMapping("/dept-distribution")
    @PreAuthorize("hasAuthority('requirement:view')")
    public Result<List<Map<String, Object>>> deptDistribution() {
        return Result.ok(dashboardService.deptDistribution());
    }

    @GetMapping("/trend")
    @PreAuthorize("hasAuthority('requirement:view')")
    public Result<List<Map<String, Object>>> trend() {
        return Result.ok(dashboardService.trend());
    }

    @GetMapping("/todo")
    @PreAuthorize("hasAuthority('requirement:view')")
    public Result<List<Map<String, Object>>> todo() {
        return Result.ok(dashboardService.todo());
    }
}
