package com.reqflow.controller;

import com.reqflow.audit.Audit;
import com.reqflow.common.PageResult;
import com.reqflow.common.Result;
import com.reqflow.dto.ActionRequest;
import com.reqflow.dto.RequirementSaveRequest;
import com.reqflow.service.RequirementService;
import com.reqflow.vo.AttachmentVO;
import com.reqflow.vo.RequirementDetailVO;
import com.reqflow.vo.RequirementVO;
import com.reqflow.vo.VersionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/requirements")
@RequiredArgsConstructor
public class RequirementController {

    private final RequirementService requirementService;

    @GetMapping
    @PreAuthorize("hasAuthority('requirement:view')")
    public Result<PageResult<RequirementVO>> list(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "20") long size,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) Long deptId,
                                                  @RequestParam(required = false) String priority,
                                                  @RequestParam(required = false) String type,
                                                  @RequestParam(required = false) Long proposerId,
                                                  @RequestParam(required = false) Boolean mine) {
        return Result.ok(requirementService.list(page, size, keyword, status, deptId, priority, type, proposerId, mine));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('requirement:view')")
    public ResponseEntity<Resource> export(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) Long deptId,
                                           @RequestParam(required = false) String priority,
                                           @RequestParam(required = false) String type,
                                           @RequestParam(required = false) Long proposerId) {
        byte[] data = requirementService.export(keyword, status, deptId, priority, type, proposerId);
        String filename = URLEncoder.encode("需求清单.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new org.springframework.core.io.ByteArrayResource(data));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('requirement:view')")
    public Result<RequirementDetailVO> detail(@PathVariable Long id) {
        return Result.ok(requirementService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('requirement:create')")
    @Audit(module = "需求管理", action = "新建需求", objectType = "REQUIREMENT")
    public Result<Long> create(@Valid @RequestBody RequirementSaveRequest request) {
        return Result.ok(requirementService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('requirement:edit')")
    @Audit(module = "需求管理", action = "编辑需求", objectType = "REQUIREMENT", objectIdParam = "id")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RequirementSaveRequest request) {
        requirementService.update(id, request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('requirement:delete')")
    @Audit(module = "需求管理", action = "删除需求", objectType = "REQUIREMENT", objectIdParam = "id")
    public Result<Void> delete(@PathVariable Long id) {
        requirementService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/action")
    @PreAuthorize("hasAuthority('requirement:action')")
    public Result<String> action(@PathVariable Long id, @Valid @RequestBody ActionRequest request) {
        return Result.ok(requirementService.action(id, request));
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('requirement:view')")
    public Result<List<VersionVO>> versions(@PathVariable Long id) {
        return Result.ok(requirementService.versions(id));
    }

    @PostMapping("/{id}/attachments")
    @PreAuthorize("hasAuthority('requirement:edit')")
    @Audit(module = "需求管理", action = "上传附件", objectType = "REQUIREMENT", objectIdParam = "id")
    public Result<AttachmentVO> upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return Result.ok(requirementService.uploadAttachment(id, file));
    }

    @GetMapping("/attachments/{id}/download")
    @PreAuthorize("hasAuthority('requirement:view')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        RequirementService.FileDownload download = requirementService.downloadAttachment(id);
        return fileResponse(download);
    }

    @DeleteMapping("/attachments/{id}")
    @PreAuthorize("hasAuthority('requirement:edit')")
    public Result<Void> deleteAttachment(@PathVariable Long id) {
        requirementService.deleteAttachment(id);
        return Result.ok();
    }

    public static ResponseEntity<Resource> fileResponse(RequirementService.FileDownload download) {
        String filename = URLEncoder.encode(download.fileName() == null ? "file" : download.fileName(),
                StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(download.path()));
    }
}
