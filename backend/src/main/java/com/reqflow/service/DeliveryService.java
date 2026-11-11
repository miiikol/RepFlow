package com.reqflow.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reqflow.audit.AuditAspect;
import com.reqflow.common.BizException;
import com.reqflow.common.ErrorCode;
import com.reqflow.common.PageResult;
import com.reqflow.common.SecurityUtils;
import com.reqflow.domain.entity.*;
import com.reqflow.domain.enums.*;
import com.reqflow.dto.AcceptanceConclusionRequest;
import com.reqflow.dto.AcceptanceCreateRequest;
import com.reqflow.mapper.*;
import com.reqflow.security.LoginUser;
import com.reqflow.service.support.BizNoGenerator;
import com.reqflow.service.support.DataScopeService;
import com.reqflow.service.support.DictService;
import com.reqflow.service.support.FileStorageService;
import com.reqflow.service.support.LookupService;
import com.reqflow.vo.AcceptanceVO;
import com.reqflow.vo.DeliverableVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliverableMapper deliverableMapper;
    private final AcceptanceMapper acceptanceMapper;
    private final RequirementMapper requirementMapper;
    private final TaskMapper taskMapper;
    private final RequirementService requirementService;
    private final RequirementFlowService flowService;
    private final DataScopeService dataScopeService;
    private final BizNoGenerator bizNoGenerator;
    private final DictService dictService;
    private final LookupService lookupService;
    private final FileStorageService fileStorageService;
    private final AuditAspect auditAspect;

    public List<DeliverableVO> deliverables(Long requirementId) {
        requirementService.getVisible(requirementId);
        Map<Long, String> users = lookupService.userNames();
        return deliverableMapper.selectList(new QueryWrapper<Deliverable>()
                        .eq("requirement_id", requirementId).orderByAsc("id"))
                .stream().map(d -> {
                    DeliverableVO vo = new DeliverableVO();
                    BeanUtils.copyProperties(d, vo);
                    vo.setUploaderName(users.get(d.getUploaderId()));
                    vo.setTypeLabel(dictService.deliverableTypeLabel(d.getType()));
                    return vo;
                }).toList();
    }

    @Transactional
    public Long createDeliverable(Long requirementId, String type, String name, String description,
                                  MultipartFile file) {
        Requirement requirement = requirementService.getVisible(requirementId);
        if (RequirementStatus.ARCHIVED.name().equals(requirement.getStatus())) {
            throw BizException.of(ErrorCode.READONLY);
        }
        try {
            DeliverableType.valueOf(type);
        } catch (Exception e) {
            throw BizException.of(ErrorCode.VALIDATION_ERROR, "未知交付物类型");
        }
        Deliverable deliverable = new Deliverable();
        deliverable.setDeliverableNo(bizNoGenerator.next("DLV"));
        deliverable.setRequirementId(requirementId);
        deliverable.setType(type);
        deliverable.setName(StringUtils.hasText(name) ? name : dictService.deliverableTypeLabel(type));
        deliverable.setDescription(description);
        if (file != null && !file.isEmpty()) {
            FileStorageService.StoredFile stored = fileStorageService.store(file);
            deliverable.setFileName(stored.originalName());
            deliverable.setStoredName(stored.storedName());
            deliverable.setFileSize(stored.size());
        }
        deliverable.setUploaderId(SecurityUtils.userId());
        deliverableMapper.insert(deliverable);
        auditAspect.record("交付验收", "登记交付物", "REQUIREMENT", String.valueOf(requirementId));
        return deliverable.getId();
    }

    @Transactional
    public void deleteDeliverable(Long id) {
        Deliverable deliverable = deliverableMapper.selectById(id);
        if (deliverable == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "交付物不存在");
        }
        Requirement requirement = requirementService.getVisible(deliverable.getRequirementId());
        if (RequirementStatus.ARCHIVED.name().equals(requirement.getStatus())) {
            throw BizException.of(ErrorCode.READONLY);
        }
        LoginUser user = SecurityUtils.currentUser();
        if (!deliverable.getUploaderId().equals(user.getId()) && !user.getRoles().contains("ADMIN")) {
            throw BizException.of(ErrorCode.FORBIDDEN);
        }
        deliverableMapper.deleteById(id);
    }

    public RequirementService.FileDownload downloadDeliverable(Long id) {
        Deliverable deliverable = deliverableMapper.selectById(id);
        if (deliverable == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "交付物不存在");
        }
        requirementService.getVisible(deliverable.getRequirementId());
        if (!StringUtils.hasText(deliverable.getStoredName())) {
            throw new BizException("该交付物仅登记了名称，没有上传文件");
        }
        return new RequirementService.FileDownload(fileStorageService.load(deliverable.getStoredName()),
                deliverable.getFileName(), null);
    }

    public PageResult<AcceptanceVO> acceptances(long page, long size, String status, Long requirementId,
                                                Boolean mine) {
        LoginUser user = SecurityUtils.currentUser();
        QueryWrapper<Acceptance> wrapper = new QueryWrapper<>();
        if (requirementId != null) {
            requirementService.getVisible(requirementId);
            wrapper.eq("requirement_id", requirementId);
        } else {
            dataScopeService.applyByRequirement(wrapper, "requirement_id", user);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        if (Boolean.TRUE.equals(mine)) {
            wrapper.eq("acceptor_id", user.getId());
        }
        wrapper.orderByDesc("submitted_at");
        Page<Acceptance> result = acceptanceMapper.selectPage(new Page<>(page, Math.min(size, 100)), wrapper);
        return PageResult.of(result.getTotal(), page, result.getSize(), enrichAcceptances(result.getRecords()));
    }

    private List<AcceptanceVO> enrichAcceptances(List<Acceptance> acceptances) {
        if (acceptances.isEmpty()) {
            return List.of();
        }
        Map<Long, String> users = lookupService.userNames();
        Map<Long, Requirement> requirements = requirementMapper.selectList(new QueryWrapper<Requirement>()
                        .in("id", acceptances.stream().map(Acceptance::getRequirementId).distinct().toList()))
                .stream().collect(Collectors.toMap(Requirement::getId, r -> r));
        return acceptances.stream().map(a -> {
            AcceptanceVO vo = new AcceptanceVO();
            BeanUtils.copyProperties(a, vo);
            Requirement requirement = requirements.get(a.getRequirementId());
            if (requirement != null) {
                vo.setRequirementNo(requirement.getReqNo());
                vo.setRequirementTitle(requirement.getTitle());
            }
            vo.setSubmitterName(users.get(a.getSubmitterId()));
            vo.setAcceptorName(users.get(a.getAcceptorId()));
            vo.setStatusLabel(dictService.acceptanceStatusLabel(a.getStatus()));
            return vo;
        }).toList();
    }

    @Transactional
    public Long createAcceptance(AcceptanceCreateRequest request) {
        Requirement requirement = requirementService.getVisible(request.getRequirementId());
        if (!RequirementStatus.TESTING.name().equals(requirement.getStatus())) {
            throw BizException.of(ErrorCode.ILLEGAL_TRANSITION, "只有测试中的需求可以提交验收");
        }
        List<Deliverable> deliverables = deliverableMapper.selectList(new QueryWrapper<Deliverable>()
                .eq("requirement_id", requirement.getId()));
        Set<String> types = deliverables.stream().map(Deliverable::getType).collect(Collectors.toSet());
        List<String> missing = DeliverableType.requiredTypes().stream()
                .filter(t -> !types.contains(t))
                .map(dictService::deliverableTypeLabel).toList();
        if (!missing.isEmpty()) {
            throw BizException.of(ErrorCode.ILLEGAL_TRANSITION, "必备交付物缺失：" + String.join("、", missing));
        }
        Long pending = acceptanceMapper.selectCount(new QueryWrapper<Acceptance>()
                .eq("requirement_id", requirement.getId())
                .eq("status", AcceptanceStatus.PENDING.name()));
        if (pending != null && pending > 0) {
            throw BizException.of(ErrorCode.ILLEGAL_TRANSITION, "已有待处理的验收单");
        }
        List<Acceptance> history = acceptanceMapper.selectList(new QueryWrapper<Acceptance>()
                .eq("requirement_id", requirement.getId()).orderByDesc("round_no"));
        int round = history.isEmpty() ? 1 : history.get(0).getRound() + 1;

        Acceptance acceptance = new Acceptance();
        acceptance.setAcceptanceNo(bizNoGenerator.next("ACC"));
        acceptance.setRequirementId(requirement.getId());
        acceptance.setRound(round);
        acceptance.setSubmitterId(SecurityUtils.userId());
        acceptance.setAcceptorId(request.getAcceptorId());
        acceptance.setDeliverableSnapshot(deliverables.stream()
                .map(Deliverable::getName).collect(Collectors.joining("；")));
        acceptance.setStatus(AcceptanceStatus.PENDING.name());
        acceptance.setComment(request.getComment());
        acceptance.setSubmittedAt(LocalDateTime.now());
        acceptanceMapper.insert(acceptance);

        flowService.apply(requirement, RequirementAction.SUBMIT_ACCEPTANCE, SecurityUtils.currentUser());
        requirementService.persist(requirement);
        auditAspect.record("交付验收", "提交验收", "REQUIREMENT", String.valueOf(requirement.getId()));
        return acceptance.getId();
    }

    @Transactional
    public void concludeAcceptance(Long id, AcceptanceConclusionRequest request) {
        Acceptance acceptance = acceptanceMapper.selectById(id);
        if (acceptance == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "验收单不存在");
        }
        if (!AcceptanceStatus.PENDING.name().equals(acceptance.getStatus())) {
            throw BizException.of(ErrorCode.ILLEGAL_TRANSITION, "验收单已处理");
        }
        LoginUser user = SecurityUtils.currentUser();
        if (!user.getRoles().contains("ADMIN") && !Objects.equals(acceptance.getAcceptorId(), user.getId())) {
            throw BizException.of(ErrorCode.FORBIDDEN, "只有指定验收人可以提交结论");
        }
        Requirement requirement = requirementService.getVisible(acceptance.getRequirementId());
        acceptance.setConclusion(request.getPass() ? "PASS" : "FAIL");
        acceptance.setComment(request.getComment());
        acceptance.setConfirmedAt(LocalDateTime.now());
        acceptance.setStatus(request.getPass() ? AcceptanceStatus.PASSED.name() : AcceptanceStatus.FAILED.name());
        if (!request.getPass()) {
            if (!StringUtils.hasText(request.getReworkRequirement())) {
                throw BizException.of(ErrorCode.VALIDATION_ERROR, "验收不通过时必须填写返工要求");
            }
            acceptance.setReworkRequirement(request.getReworkRequirement());
        }
        acceptanceMapper.updateById(acceptance);

        if (request.getPass()) {
            flowService.apply(requirement, RequirementAction.ACCEPT_PASS, user);
            requirementService.persist(requirement);
            auditAspect.record("交付验收", "验收通过", "REQUIREMENT", String.valueOf(requirement.getId()));
        } else {
            flowService.apply(requirement, RequirementAction.ACCEPT_REJECT, user);
            requirementService.persist(requirement);
            createReworkTask(requirement, request.getReworkRequirement());
            auditAspect.record("交付验收", "验收不通过并生成返工任务", "REQUIREMENT",
                    String.valueOf(requirement.getId()));
        }
    }

    private void createReworkTask(Requirement requirement, String reworkRequirement) {
        Task task = new Task();
        task.setTaskNo(bizNoGenerator.next("TSK"));
        task.setRequirementId(requirement.getId());
        task.setProjectId(requirement.getProjectId());
        task.setMilestoneId(requirement.getMilestoneId());
        task.setName("返工：" + (reworkRequirement.length() > 50
                ? reworkRequirement.substring(0, 50) : reworkRequirement));
        task.setType(TaskType.DEV.name());
        task.setAssigneeId(requirement.getPmId() != null ? requirement.getPmId() : requirement.getProposerId());
        task.setEstimateHours(BigDecimal.ZERO);
        task.setActualHours(BigDecimal.ZERO);
        task.setPlanDate(LocalDate.now().plusDays(5));
        task.setStatus(TaskStatus.TODO.name());
        taskMapper.insert(task);
    }
}
