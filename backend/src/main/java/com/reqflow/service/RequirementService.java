package com.reqflow.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reqflow.audit.AuditAspect;
import com.reqflow.common.BizException;
import com.reqflow.common.ErrorCode;
import com.reqflow.common.PageResult;
import com.reqflow.common.Permissions;
import com.reqflow.common.SecurityUtils;
import com.reqflow.domain.entity.*;
import com.reqflow.domain.enums.*;
import com.reqflow.dto.ActionRequest;
import com.reqflow.dto.RequirementSaveRequest;
import com.reqflow.mapper.*;
import com.reqflow.security.LoginUser;
import com.reqflow.service.support.*;
import com.reqflow.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequirementService {

    private final RequirementMapper requirementMapper;
    private final RequirementVersionMapper versionMapper;
    private final RequirementAttachmentMapper attachmentMapper;
    private final ReviewMapper reviewMapper;
    private final ReviewOpinionMapper opinionMapper;
    private final ReviewParticipantMapper reviewParticipantMapper;
    private final ChangeRequestMapper changeMapper;
    private final TaskMapper taskMapper;
    private final DeliverableMapper deliverableMapper;
    private final AcceptanceMapper acceptanceMapper;
    private final AuditLogMapper auditLogMapper;
    private final ProjectMapper projectMapper;
    private final MilestoneMapper milestoneMapper;
    private final RequirementFlowService flowService;
    private final DataScopeService dataScopeService;
    private final BizNoGenerator bizNoGenerator;
    private final DictService dictService;
    private final LookupService lookupService;
    private final FileStorageService fileStorageService;
    private final ExcelExportService excelExportService;
    private final AuditAspect auditAspect;
    private final ObjectMapper objectMapper;

    public PageResult<RequirementVO> list(long page, long size, String keyword, String status, Long deptId,
                                          String priority, String type, Long proposerId, Boolean mine) {
        LoginUser user = SecurityUtils.currentUser();
        QueryWrapper<Requirement> wrapper = buildWrapper(keyword, status, deptId, priority, type, proposerId, mine);
        dataScopeService.apply(wrapper, user);
        wrapper.orderByDesc("updated_at");
        Page<Requirement> result = requirementMapper.selectPage(new Page<>(page, Math.min(size, 100)), wrapper);
        return PageResult.of(result.getTotal(), page, result.getSize(), enrich(result.getRecords()));
    }

    public byte[] export(String keyword, String status, Long deptId, String priority, String type, Long proposerId) {
        LoginUser user = SecurityUtils.currentUser();
        QueryWrapper<Requirement> wrapper = buildWrapper(keyword, status, deptId, priority, type, proposerId, false);
        dataScopeService.apply(wrapper, user);
        wrapper.orderByDesc("updated_at").last("limit 10000");
        return excelExportService.exportRequirements(enrich(requirementMapper.selectList(wrapper)));
    }

    private QueryWrapper<Requirement> buildWrapper(String keyword, String status, Long deptId, String priority,
                                                   String type, Long proposerId, Boolean mine) {
        QueryWrapper<Requirement> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("title", keyword.trim()).or().like("req_no", keyword.trim()));
        }
        if (StringUtils.hasText(status)) {
            wrapper.in("status", Arrays.stream(status.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList());
        }
        if (deptId != null) {
            wrapper.eq("dept_id", deptId);
        }
        if (StringUtils.hasText(priority)) {
            wrapper.eq("priority", priority);
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq("req_type", type);
        }
        if (proposerId != null) {
            wrapper.eq("proposer_id", proposerId);
        }
        if (Boolean.TRUE.equals(mine)) {
            Long userId = SecurityUtils.userId();
            wrapper.and(w -> w.eq("proposer_id", userId).or().eq("analyst_id", userId).or().eq("pm_id", userId));
        }
        return wrapper;
    }

    public Requirement getEntity(Long id) {
        Requirement requirement = requirementMapper.selectById(id);
        if (requirement == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "需求不存在");
        }
        return requirement;
    }

    public Requirement getVisible(Long id) {
        Requirement requirement = getEntity(id);
        dataScopeService.checkView(requirement, SecurityUtils.currentUser());
        return requirement;
    }

    public List<RequirementVO> enrich(List<Requirement> requirements) {
        return enrich(requirements, lookupService.snapshot());
    }

    public List<RequirementVO> enrich(List<Requirement> requirements, LookupService.Snapshot lookup) {
        if (requirements.isEmpty()) {
            return List.of();
        }
        List<Long> ids = requirements.stream().map(Requirement::getId).toList();
        Map<Long, List<Task>> taskMap = taskMapper.selectList(new QueryWrapper<Task>().in("requirement_id", ids))
                .stream().collect(Collectors.groupingBy(Task::getRequirementId));
        return requirements.stream()
                .map(r -> toRequirementVO(r, taskMap.getOrDefault(r.getId(), List.of()), lookup))
                .toList();
    }

    private RequirementVO toRequirementVO(Requirement r, List<Task> tasks, LookupService.Snapshot lookup) {
        RequirementVO vo = new RequirementVO();
        BeanUtils.copyProperties(r, vo);
        vo.setDeptName(lookup.depts().get(r.getDeptId()));
        vo.setProposerName(lookup.users().get(r.getProposerId()));
        vo.setAnalystName(lookup.users().get(r.getAnalystId()));
        vo.setPmName(lookup.users().get(r.getPmId()));
        vo.setProjectName(lookup.projects().get(r.getProjectId()));
        vo.setMilestoneName(lookup.milestones().get(r.getMilestoneId()));
        vo.setStatusLabel(dictService.statusLabel(r.getStatus()));
        vo.setTypeLabel(dictService.typeLabel(r.getType()));
        vo.setSourceLabel(dictService.sourceLabel(r.getSource()));
        vo.setPriorityLabel(dictService.priorityLabel(r.getPriority()));
        List<Task> related = tasks.stream()
                .filter(t -> !TaskStatus.CANCELLED.name().equals(t.getStatus())).toList();
        int done = (int) related.stream().filter(t -> TaskStatus.DONE.name().equals(t.getStatus())).count();
        vo.setTaskTotal(related.size());
        vo.setTaskDone(done);
        vo.setProgress(related.isEmpty() ? 0 : done * 100 / related.size());
        vo.setOverdue(r.getExpectDate() != null && r.getExpectDate().isBefore(LocalDate.now())
                && !Set.of(RequirementStatus.ACCEPTED.name(), RequirementStatus.ARCHIVED.name(),
                RequirementStatus.CANCELLED.name()).contains(r.getStatus()));
        return vo;
    }

    public RequirementDetailVO detail(Long id) {
        LoginUser user = SecurityUtils.currentUser();
        Requirement requirement = getEntity(id);
        dataScopeService.checkView(requirement, user);
        LookupService.Snapshot lookup = lookupService.snapshot();
        Map<Long, String> users = lookup.users();
        List<Task> requirementTasks = taskMapper.selectList(new QueryWrapper<Task>()
                .eq("requirement_id", id).orderByAsc("id"));
        RequirementDetailVO detail = new RequirementDetailVO();
        detail.setRequirement(toRequirementVO(requirement, requirementTasks, lookup));
        detail.setAvailableActions(flowService.availableActions(requirement, user));

        List<Review> reviews = reviewMapper.selectList(new QueryWrapper<Review>()
                .eq("requirement_id", id).orderByDesc("created_at"));
        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();
        Map<Long, List<ReviewOpinion>> opinions = reviewIds.isEmpty() ? Map.of()
                : opinionMapper.selectList(new QueryWrapper<ReviewOpinion>().in("review_id", reviewIds))
                .stream().collect(Collectors.groupingBy(ReviewOpinion::getReviewId));
        Map<Long, List<Long>> reviewParticipants = reviewIds.isEmpty() ? Map.of()
                : reviewParticipantMapper.selectList(new QueryWrapper<ReviewParticipant>().in("review_id", reviewIds))
                .stream().collect(Collectors.groupingBy(ReviewParticipant::getReviewId,
                        Collectors.mapping(ReviewParticipant::getUserId, Collectors.toList())));
        detail.setReviews(reviews.stream().map(r -> {
            ReviewVO vo = new ReviewVO();
            BeanUtils.copyProperties(r, vo);
            vo.setRequirementNo(requirement.getReqNo());
            vo.setRequirementTitle(requirement.getTitle());
            vo.setHostName(users.get(r.getHostId()));
            List<Long> reviewerIds = reviewParticipants.getOrDefault(r.getId(), List.of());
            vo.setReviewerIds(reviewerIds);
            vo.setReviewerNames(reviewerIds.stream().map(users::get)
                    .filter(Objects::nonNull).collect(Collectors.joining("、")));
            vo.setStatusLabel(dictService.reviewStatusLabel(r.getStatus()));
            vo.setOpinions(opinions.getOrDefault(r.getId(), List.of()).stream().map(o -> {
                ReviewOpinionVO ov = new ReviewOpinionVO();
                BeanUtils.copyProperties(o, ov);
                ov.setReviewerName(users.get(o.getReviewerId()));
                ov.setResultLabel(dictService.reviewResultLabel(o.getResult()));
                return ov;
            }).toList());
            return vo;
        }).toList());

        detail.setChanges(changeMapper.selectList(new QueryWrapper<ChangeRequest>()
                        .eq("requirement_id", id).orderByDesc("created_at"))
                .stream().map(c -> {
                    ChangeVO vo = new ChangeVO();
                    BeanUtils.copyProperties(c, vo);
                    vo.setRequirementNo(requirement.getReqNo());
                    vo.setRequirementTitle(requirement.getTitle());
                    vo.setApplicantName(users.get(c.getApplicantId()));
                    vo.setApproverName(users.get(c.getApproverId()));
                    vo.setStatusLabel(dictService.changeStatusLabel(c.getStatus()));
                    return vo;
                }).toList());

        detail.setTasks(requirementTasks.stream().map(t -> toTaskVO(t, users, requirement)).toList());

        detail.setDeliverables(deliverableMapper.selectList(new QueryWrapper<Deliverable>()
                        .eq("requirement_id", id).orderByAsc("id"))
                .stream().map(d -> {
                    DeliverableVO vo = new DeliverableVO();
                    BeanUtils.copyProperties(d, vo);
                    vo.setUploaderName(users.get(d.getUploaderId()));
                    vo.setTypeLabel(dictService.deliverableTypeLabel(d.getType()));
                    return vo;
                }).toList());

        detail.setAcceptances(acceptanceMapper.selectList(new QueryWrapper<Acceptance>()
                        .eq("requirement_id", id).orderByDesc("round_no"))
                .stream().map(a -> {
                    AcceptanceVO vo = new AcceptanceVO();
                    BeanUtils.copyProperties(a, vo);
                    vo.setRequirementNo(requirement.getReqNo());
                    vo.setRequirementTitle(requirement.getTitle());
                    vo.setSubmitterName(users.get(a.getSubmitterId()));
                    vo.setAcceptorName(users.get(a.getAcceptorId()));
                    vo.setStatusLabel(dictService.acceptanceStatusLabel(a.getStatus()));
                    return vo;
                }).toList());

        detail.setVersions(versionMapper.selectList(new QueryWrapper<RequirementVersion>()
                        .eq("requirement_id", id).orderByDesc("created_at"))
                .stream().map(v -> {
                    VersionVO vo = new VersionVO();
                    BeanUtils.copyProperties(v, vo);
                    vo.setOperatorName(users.get(v.getOperatorId()));
                    return vo;
                }).toList());

        detail.setAttachments(attachmentMapper.selectList(new QueryWrapper<RequirementAttachment>()
                        .eq("requirement_id", id).orderByDesc("created_at"))
                .stream().map(a -> {
                    AttachmentVO vo = new AttachmentVO();
                    BeanUtils.copyProperties(a, vo);
                    vo.setUploaderName(users.get(a.getUploaderId()));
                    return vo;
                }).toList());

        List<AuditLog> logs = auditLogMapper.selectList(new QueryWrapper<AuditLog>()
                .eq("object_type", "REQUIREMENT").eq("object_id", String.valueOf(id))
                .orderByAsc("created_at"));
        detail.setTimeline(logs.stream().map(log -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("time", log.getCreatedAt());
            item.put("user", log.getUsername());
            item.put("module", log.getModule());
            item.put("action", log.getAction());
            item.put("result", log.getResult());
            return item;
        }).toList());
        return detail;
    }

    private TaskVO toTaskVO(Task task, Map<Long, String> users, Requirement requirement) {
        TaskVO vo = new TaskVO();
        BeanUtils.copyProperties(task, vo);
        vo.setAssigneeName(users.get(task.getAssigneeId()));
        vo.setRequirementNo(requirement.getReqNo());
        vo.setRequirementTitle(requirement.getTitle());
        vo.setStatusLabel(dictService.taskStatusLabel(task.getStatus()));
        vo.setTypeLabel(dictService.taskTypeLabel(task.getType()));
        vo.setOverdue(task.getPlanDate() != null && task.getPlanDate().isBefore(LocalDate.now())
                && !Set.of(TaskStatus.DONE.name(), TaskStatus.CANCELLED.name()).contains(task.getStatus()));
        return vo;
    }

    @Transactional
    public Long create(RequirementSaveRequest request) {
        LoginUser user = SecurityUtils.currentUser();
        Requirement requirement = new Requirement();
        applyRequest(requirement, request);
        requirement.setReqNo(bizNoGenerator.next("REQ"));
        requirement.setProposerId(user.getId());
        requirement.setStatus(RequirementStatus.DRAFT.name());
        requirement.setVersion("V0.1");
        requirement.setChangeCount(0);
        requirementMapper.insert(requirement);
        return requirement.getId();
    }

    @Transactional
    public void update(Long id, RequirementSaveRequest request) {
        LoginUser user = SecurityUtils.currentUser();
        Requirement requirement = getVisible(id);
        if (RequirementStatus.ARCHIVED.name().equals(requirement.getStatus())) {
            throw BizException.of(ErrorCode.READONLY);
        }
        boolean editable = Set.of(RequirementStatus.DRAFT.name(), RequirementStatus.REJECTED.name())
                .contains(requirement.getStatus());
        if (!editable) {
            throw BizException.of(ErrorCode.ILLEGAL_TRANSITION, "只有草稿或已驳回的需求可以编辑");
        }
        boolean owner = requirement.getProposerId().equals(user.getId());
        boolean canEdit = owner || SecurityUtils.hasPermission(user, Permissions.ADMIN)
                || SecurityUtils.hasPermission(user, Permissions.REQUIREMENT_EDIT_ANY);
        if (!canEdit) {
            throw BizException.of(ErrorCode.FORBIDDEN);
        }
        applyRequest(requirement, request);
        if (RequirementStatus.REJECTED.name().equals(requirement.getStatus())) {
            requirement.setVersion(bumpMinor(requirement.getVersion()));
            snapshot(requirement, "评审驳回后修改");
        }
        persist(requirement);
    }

    /**
     * 统一的需求更新入口：依赖乐观锁拦截器，版本不一致时影响行数为 0。
     * 各业务流程必须走这里，避免遗漏并发冲突判断。
     */
    public void persist(Requirement requirement) {
        int rows = requirementMapper.updateById(requirement);
        if (rows == 0) {
            throw BizException.of(ErrorCode.CONFLICT);
        }
    }

    private void applyRequest(Requirement requirement, RequirementSaveRequest request) {
        requirement.setTitle(request.getTitle());
        requirement.setType(request.getType());
        requirement.setSource(request.getSource());
        requirement.setDeptId(request.getDeptId());
        requirement.setPriority(request.getPriority());
        requirement.setExpectDate(request.getExpectDate());
        requirement.setBackground(request.getBackground());
        requirement.setDescription(request.getDescription());
        requirement.setAcceptanceCriteria(request.getAcceptanceCriteria());
        requirement.setPlanDate(request.getPlanDate());
        requirement.setAnalystId(request.getAnalystId());
        requirement.setPmId(request.getPmId());
        requirement.setProjectId(request.getProjectId());
        requirement.setMilestoneId(request.getMilestoneId());
        requirement.setTags(request.getTags());
    }

    @Transactional
    public void delete(Long id) {
        Requirement requirement = getVisible(id);
        if (!RequirementStatus.DRAFT.name().equals(requirement.getStatus())) {
            throw BizException.of(ErrorCode.ILLEGAL_TRANSITION, "只有草稿可以删除");
        }
        LoginUser user = SecurityUtils.currentUser();
        if (!requirement.getProposerId().equals(user.getId())
                && !SecurityUtils.hasPermission(user, Permissions.ADMIN)) {
            throw BizException.of(ErrorCode.FORBIDDEN);
        }
        if (requirementMapper.deleteById(id) == 0) {
            throw BizException.of(ErrorCode.CONFLICT);
        }
    }

    @Transactional
    public String action(Long id, ActionRequest request) {
        Requirement requirement = getVisible(id);
        RequirementAction action;
        try {
            action = RequirementAction.valueOf(request.getAction());
        } catch (Exception e) {
            throw BizException.of(ErrorCode.ILLEGAL_TRANSITION, "未知动作：" + request.getAction());
        }
        if (flowService.isDocumentDriven(action) || action == RequirementAction.SUBMIT_REVIEW) {
            throw BizException.of(ErrorCode.BIZ_ERROR, "该操作需通过评审单或验收单完成");
        }
        if (action == RequirementAction.SCHEDULE) {
            if (request.getProjectId() == null || request.getMilestoneId() == null) {
                throw BizException.of(ErrorCode.VALIDATION_ERROR, "排期必须选择项目与里程碑");
            }
            Project project = projectMapper.selectById(request.getProjectId());
            Milestone milestone = milestoneMapper.selectById(request.getMilestoneId());
            if (project == null || milestone == null || !project.getId().equals(milestone.getProjectId())) {
                throw BizException.of(ErrorCode.VALIDATION_ERROR, "项目或里程碑不存在，且里程碑必须属于所选项目");
            }
            requirement.setProjectId(request.getProjectId());
            requirement.setMilestoneId(request.getMilestoneId());
            if (request.getPlanDate() != null) {
                requirement.setPlanDate(request.getPlanDate());
            }
        }
        if (action == RequirementAction.ARCHIVE) {
            checkArchiveReady(requirement);
        }
        flowService.apply(requirement, action, SecurityUtils.currentUser());
        if (action == RequirementAction.ACCEPT && requirement.getAnalystId() == null) {
            requirement.setAnalystId(SecurityUtils.userId());
        }
        if (action == RequirementAction.SCHEDULE && requirement.getPmId() == null) {
            requirement.setPmId(SecurityUtils.userId());
        }
        persist(requirement);
        String auditAction = action.label()
                + (StringUtils.hasText(request.getComment()) ? "：" + request.getComment() : "");
        auditAspect.record("需求管理", auditAction, "REQUIREMENT", String.valueOf(id));
        return requirement.getStatus();
    }

    private void checkArchiveReady(Requirement requirement) {
        Long passed = acceptanceMapper.selectCount(new QueryWrapper<Acceptance>()
                .eq("requirement_id", requirement.getId())
                .eq("status", AcceptanceStatus.PASSED.name()));
        if (passed == null || passed == 0) {
            throw BizException.of(ErrorCode.ILLEGAL_TRANSITION, "验收通过后才能归档");
        }
        Set<String> types = deliverableMapper.selectList(new QueryWrapper<Deliverable>()
                        .eq("requirement_id", requirement.getId()))
                .stream().map(Deliverable::getType).collect(Collectors.toSet());
        List<String> missing = DeliverableType.requiredTypes().stream()
                .filter(t -> !types.contains(t))
                .map(dictService::deliverableTypeLabel).toList();
        if (!missing.isEmpty()) {
            throw BizException.of(ErrorCode.ILLEGAL_TRANSITION, "必备交付物缺失：" + String.join("、", missing));
        }
    }

    public List<VersionVO> versions(Long id) {
        getVisible(id);
        Map<Long, String> users = lookupService.userNames();
        return versionMapper.selectList(new QueryWrapper<RequirementVersion>()
                        .eq("requirement_id", id).orderByDesc("created_at"))
                .stream().map(v -> {
                    VersionVO vo = new VersionVO();
                    BeanUtils.copyProperties(v, vo);
                    vo.setOperatorName(users.get(v.getOperatorId()));
                    return vo;
                }).toList();
    }

    public void snapshot(Requirement requirement, String reason) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("title", requirement.getTitle());
            map.put("type", requirement.getType());
            map.put("priority", requirement.getPriority());
            map.put("expectDate", requirement.getExpectDate());
            map.put("planDate", requirement.getPlanDate());
            map.put("description", requirement.getDescription());
            map.put("acceptanceCriteria", requirement.getAcceptanceCriteria());
            map.put("version", requirement.getVersion());
            RequirementVersion version = new RequirementVersion();
            version.setRequirementId(requirement.getId());
            version.setVersion(requirement.getVersion());
            version.setReason(reason);
            version.setSnapshotJson(objectMapper.writeValueAsString(map));
            version.setOperatorId(SecurityUtils.userId());
            versionMapper.insert(version);
        } catch (JsonProcessingException e) {
            throw new BizException("生成版本快照失败");
        }
    }

    public String bumpMinor(String version) {
        if (version == null || !version.startsWith("V")) {
            return "V0.2";
        }
        String[] parts = version.substring(1).split("\\.");
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return "V%d.%d".formatted(major, minor + 1);
        } catch (NumberFormatException e) {
            return "V0.2";
        }
    }

    public AttachmentVO uploadAttachment(Long requirementId, MultipartFile file) {
        Requirement requirement = getVisible(requirementId);
        if (RequirementStatus.ARCHIVED.name().equals(requirement.getStatus())) {
            throw BizException.of(ErrorCode.READONLY);
        }
        FileStorageService.StoredFile stored = fileStorageService.store(file);
        RequirementAttachment attachment = new RequirementAttachment();
        attachment.setRequirementId(requirementId);
        attachment.setFileName(stored.originalName());
        attachment.setStoredName(stored.storedName());
        attachment.setFileSize(stored.size());
        attachment.setContentType(stored.contentType());
        attachment.setUploaderId(SecurityUtils.userId());
        attachmentMapper.insert(attachment);
        AttachmentVO vo = new AttachmentVO();
        BeanUtils.copyProperties(attachment, vo);
        vo.setUploaderName(SecurityUtils.currentUser().getRealName());
        return vo;
    }

    public FileDownload downloadAttachment(Long attachmentId) {
        RequirementAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "附件不存在");
        }
        getVisible(attachment.getRequirementId());
        Path path = fileStorageService.load(attachment.getStoredName());
        return new FileDownload(path, attachment.getFileName(), attachment.getContentType());
    }

    public void deleteAttachment(Long attachmentId) {
        RequirementAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "附件不存在");
        }
        getVisible(attachment.getRequirementId());
        LoginUser user = SecurityUtils.currentUser();
        if (!attachment.getUploaderId().equals(user.getId())
                && !SecurityUtils.hasPermission(user, Permissions.ADMIN)) {
            throw BizException.of(ErrorCode.FORBIDDEN);
        }
        attachmentMapper.deleteById(attachmentId);
    }

    public record FileDownload(Path path, String fileName, String contentType) {
    }
}
