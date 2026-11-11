package com.reqflow.controller;

import com.reqflow.common.PageResult;
import com.reqflow.common.Result;
import com.reqflow.dto.OpinionRequest;
import com.reqflow.dto.ReviewConclusionRequest;
import com.reqflow.dto.ReviewCreateRequest;
import com.reqflow.service.ReviewService;
import com.reqflow.vo.ReviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @PreAuthorize("hasAuthority('review:view')")
    public Result<PageResult<ReviewVO>> list(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "20") long size,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) Long requirementId,
                                             @RequestParam(required = false) Boolean mine) {
        return Result.ok(reviewService.list(page, size, status, requirementId, mine));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('review:view')")
    public Result<ReviewVO> detail(@PathVariable Long id) {
        return Result.ok(reviewService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('review:create')")
    public Result<Long> create(@Valid @RequestBody ReviewCreateRequest request) {
        return Result.ok(reviewService.create(request));
    }

    @PostMapping("/{id}/opinions")
    @PreAuthorize("hasAuthority('review:opinion')")
    public Result<Void> opinion(@PathVariable Long id, @Valid @RequestBody OpinionRequest request) {
        reviewService.addOpinion(id, request);
        return Result.ok();
    }

    @PostMapping("/{id}/conclusion")
    @PreAuthorize("hasAuthority('review:decide')")
    public Result<Void> conclusion(@PathVariable Long id, @Valid @RequestBody ReviewConclusionRequest request) {
        reviewService.conclude(id, request);
        return Result.ok();
    }
}
