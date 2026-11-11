package com.reqflow.controller;

import com.reqflow.common.PageResult;
import com.reqflow.common.Result;
import com.reqflow.dto.AcceptanceConclusionRequest;
import com.reqflow.dto.AcceptanceCreateRequest;
import com.reqflow.service.DeliveryService;
import com.reqflow.vo.AcceptanceVO;
import com.reqflow.vo.DeliverableVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/api/requirements/{requirementId}/deliverables")
    @PreAuthorize("hasAuthority('delivery:view')")
    public Result<List<DeliverableVO>> deliverables(@PathVariable Long requirementId) {
        return Result.ok(deliveryService.deliverables(requirementId));
    }

    @PostMapping("/api/requirements/{requirementId}/deliverables")
    @PreAuthorize("hasAuthority('delivery:manage')")
    public Result<Long> createDeliverable(@PathVariable Long requirementId,
                                          @RequestParam String type,
                                          @RequestParam(required = false) String name,
                                          @RequestParam(required = false) String description,
                                          @RequestParam(required = false) MultipartFile file) {
        return Result.ok(deliveryService.createDeliverable(requirementId, type, name, description, file));
    }

    @DeleteMapping("/api/deliverables/{id}")
    @PreAuthorize("hasAuthority('delivery:manage')")
    public Result<Void> deleteDeliverable(@PathVariable Long id) {
        deliveryService.deleteDeliverable(id);
        return Result.ok();
    }

    @GetMapping("/api/deliverables/{id}/download")
    @PreAuthorize("hasAuthority('delivery:view')")
    public ResponseEntity<Resource> downloadDeliverable(@PathVariable Long id) {
        return RequirementController.fileResponse(deliveryService.downloadDeliverable(id));
    }

    @GetMapping("/api/acceptances")
    @PreAuthorize("hasAuthority('delivery:view')")
    public Result<PageResult<AcceptanceVO>> acceptances(@RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "20") long size,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) Long requirementId,
                                                        @RequestParam(required = false) Boolean mine) {
        return Result.ok(deliveryService.acceptances(page, size, status, requirementId, mine));
    }

    @PostMapping("/api/acceptances")
    @PreAuthorize("hasAuthority('delivery:manage')")
    public Result<Long> createAcceptance(@Valid @RequestBody AcceptanceCreateRequest request) {
        return Result.ok(deliveryService.createAcceptance(request));
    }

    @PostMapping("/api/acceptances/{id}/conclusion")
    @PreAuthorize("hasAuthority('delivery:accept')")
    public Result<Void> conclude(@PathVariable Long id,
                                 @Valid @RequestBody AcceptanceConclusionRequest request) {
        deliveryService.concludeAcceptance(id, request);
        return Result.ok();
    }
}
