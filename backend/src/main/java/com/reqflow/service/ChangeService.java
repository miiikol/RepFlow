package com.reqflow.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reqflow.audit.AuditAspect;
import com.reqflow.common.BizException;
import com.reqflow.common.ErrorCode;
import com.reqflow.common.PageResult;
import com.reqflow.common.SecurityUtils;
import com.reqflow.domain.entity.ChangeRequest;
import com.reqflow.domain.entity.Requirement;
import com.reqflow.domain.enums.ChangeStatus;
import com.reqflow.domain.enums.RequirementStatus;
import com.reqflow.dto.ApprovalRequest;
import com.reqflow.dto.ChangeCreateRequest;
import com.reqflow.mapper.ChangeRequestMapper;
import com.reqflow.mapper.RequirementMapper;
import com.reqflow.security.LoginUser;
import com.reqflow.service.support.BizNoGenerator;
import com.reqflow.service.support.DataScopeService;
import com.reqflow.service.support.DictService;
import com.reqflow.service.support.LookupService;
import com.reqflow.vo.ChangeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChangeService {

    private static final Set<String> CHANGEABLE = Set.of(
            RequirementStatus.REVIEW_PASSED.name(), RequirementStatus.SCHEDULED.name(),
            RequirementStatus.DEVELOPING.name(), RequirementStatus.TESTING.name(),
            RequirementStatus.PENDING_ACCEPTANCE.name());

    private final ChangeRequestMapper changeMapper;
    private final RequirementMapper requirementMapper;
    private final RequirementService requirementService;
    private final RequirementFlowService flowService;
    private final DataScopeService dataScopeService;
    private final BizNoGenerator bizNoGenerator;
    private final DictService dictService;
    private final LookupService lookupService;
    private final AuditAspect auditAspect;

    public PageResult<ChangeVO> list(long page, long size, String status, Long requirementId) {
        LoginUser user = SecurityUtils.currentUser();
        QueryWrapper<ChangeRequest> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        if (requirementId != null) {
            requirementService.getVisible(requirementId);
            wrapper.eq("requirement_id", requirementId);
        } else {
            dataScopeService.applyByRequirement(wrapper, "requirement_id", user);
        }
        wrapper.orderByDesc("created_at");
        Page<ChangeRequest> result = changeMapper.selectPage(new Page<>(page, Math.min(size, 100)), wrapper);
        return PageResult.of(result.getTotal(), page, result.getSize(), enrich(result.getRecords()));
    }

    private List<ChangeVO> enrich(List<ChangeRequest> changes) {
        if (changes.isEmpty()) {
            return List.of();
        }
        Map<Long, String> users = lookupService.userNames();
        Map<Long, Requirement> requirements = requirementMapper.selectList(new QueryWrapper<Requirement>()
                        .in("id", changes.stream().map(ChangeRequest::getRequirementId).distinct().toList()))
                .stream().collect(Collectors.toMap(Requirement::getId, r -> r));
        return changes.stream().map(c -> {
            ChangeVO vo = new ChangeVO();
            BeanUtils.copyProperties(c, vo);
            Requirement requirement = requirements.get(c.getRequirementId());
            if (requirement != null) {
                vo.setRequirementNo(requirement.getReqNo());
                vo.setRequirementTitle(requirement.getTitle());
            }
            vo.setApplicantName(users.get(c.getApplicantId()));
            vo.setApproverName(users.get(c.getApproverId()));
            vo.setStatusLabel(dictService.changeStatusLabel(c.getStatus()));
            return vo;
        }).toList();
    }

    @Transactional
    public Long create(ChangeCreateRequest request) {
        Requirement requirement = requirementService.getVisible(request.getRequirementId());
        if (!CHANGEABLE.contains(requirement.getStatus())) {
            throw BizException.of(ErrorCode.ILLEGAL_TRANSITION, "当前状态不允许发起变更");
        }
        ChangeRequest change = new ChangeRequest();
        change.setChangeNo(bizNoGenerator.next("CHG"));
        change.setRequirementId(requirement.getId());
        change.setReason(request.getReason());
        change.setContent(request.getContent());
        change.setImpactHours(request.getImpactHours());
        change.setImpactDesc(request.getImpactDesc());
        change.setStatus(ChangeStatus.PENDING.name());
        change.setApplicantId(SecurityUtils.userId());
        changeMapper.insert(change);
        auditAspect.record("变更管理", "发起变更申请", "REQUIREMENT", String.valueOf(requirement.getId()));
        return change.getId();
    }

    @Transactional
    public void approve(Long id, ApprovalRequest request) {
        ChangeRequest change = getEntity(id);
        requirementService.getVisible(change.getRequirementId());
        if (!ChangeStatus.PENDING.name().equals(change.getStatus())) {
            throw BizException.of(ErrorCode.ILLEGAL_TRANSITION, "变更单已审批");
        }
        change.setStatus(request.getApproved() ? ChangeStatus.APPROVED.name() : ChangeStatus.REJECTED.name());
        change.setApproverId(SecurityUtils.userId());
        change.setApproveComment(request.getComment());
        change.setApprovedAt(LocalDateTime.now());
        changeMapper.updateById(change);
        auditAspect.record("变更管理", request.getApproved() ? "变更审批通过" : "变更审批驳回",
                "REQUIREMENT", String.valueOf(change.getRequirementId()));
    }

    @Transactional
    public void apply(Long id) {
        ChangeRequest change = getEntity(id);
        if (!ChangeStatus.APPROVED.name().equals(change.getStatus())) {
            throw BizException.of(ErrorCode.ILLEGAL_TRANSITION, "只有已批准的变更可以实施");
        }
        Requirement requirement = requirementService.getVisible(change.getRequirementId());
        if (RequirementStatus.ARCHIVED.name().equals(requirement.getStatus())) {
            throw BizException.of(ErrorCode.READONLY);
        }
        change.setStatus(ChangeStatus.APPLIED.name());
        change.setAppliedAt(LocalDateTime.now());
        changeMapper.updateById(change);

        requirement.setVersion(requirementService.bumpMinor(requirement.getVersion()));
        requirement.setChangeCount((requirement.getChangeCount() == null ? 0 : requirement.getChangeCount()) + 1);
        requirementService.persist(requirement);
        requirementService.snapshot(requirement, "变更生效：" + change.getReason());
        auditAspect.record("变更管理", "变更生效", "REQUIREMENT", String.valueOf(requirement.getId()));
    }

    private ChangeRequest getEntity(Long id) {
        ChangeRequest change = changeMapper.selectById(id);
        if (change == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "变更单不存在");
        }
        return change;
    }
}
