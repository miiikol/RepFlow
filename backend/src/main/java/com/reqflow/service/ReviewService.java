package com.reqflow.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reqflow.audit.AuditAspect;
import com.reqflow.common.BizException;
import com.reqflow.common.ErrorCode;
import com.reqflow.common.PageResult;
import com.reqflow.common.Permissions;
import com.reqflow.common.SecurityUtils;
import com.reqflow.domain.entity.Requirement;
import com.reqflow.domain.entity.Review;
import com.reqflow.domain.entity.ReviewOpinion;
import com.reqflow.domain.entity.ReviewParticipant;
import com.reqflow.domain.enums.RequirementAction;
import com.reqflow.domain.enums.RequirementStatus;
import com.reqflow.domain.enums.ReviewResult;
import com.reqflow.domain.enums.ReviewStatus;
import com.reqflow.dto.OpinionRequest;
import com.reqflow.dto.ReviewConclusionRequest;
import com.reqflow.dto.ReviewCreateRequest;
import com.reqflow.mapper.RequirementMapper;
import com.reqflow.mapper.ReviewMapper;
import com.reqflow.mapper.ReviewOpinionMapper;
import com.reqflow.mapper.ReviewParticipantMapper;
import com.reqflow.security.LoginUser;
import com.reqflow.service.support.BizNoGenerator;
import com.reqflow.service.support.DataScopeService;
import com.reqflow.service.support.DictService;
import com.reqflow.service.support.LookupService;
import com.reqflow.vo.ReviewOpinionVO;
import com.reqflow.vo.ReviewVO;
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
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final ReviewOpinionMapper opinionMapper;
    private final ReviewParticipantMapper participantMapper;
    private final RequirementMapper requirementMapper;
    private final RequirementService requirementService;
    private final RequirementFlowService flowService;
    private final DataScopeService dataScopeService;
    private final BizNoGenerator bizNoGenerator;
    private final DictService dictService;
    private final LookupService lookupService;
    private final AuditAspect auditAspect;
    private final ObjectMapper objectMapper;

    public PageResult<ReviewVO> list(long page, long size, String status, Long requirementId, Boolean mine) {
        LoginUser user = SecurityUtils.currentUser();
        QueryWrapper<Review> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        if (requirementId != null) {
            requirementService.getVisible(requirementId);
            wrapper.eq("requirement_id", requirementId);
        } else {
            dataScopeService.applyByRequirement(wrapper, "requirement_id", user);
        }
        if (Boolean.TRUE.equals(mine)) {
            List<Long> participating = participantMapper.selectList(new QueryWrapper<ReviewParticipant>()
                            .eq("user_id", user.getId()))
                    .stream().map(ReviewParticipant::getReviewId).toList();
            wrapper.and(w -> {
                w.eq("host_id", user.getId());
                if (!participating.isEmpty()) {
                    w.or().in("id", participating);
                }
            });
        }
        wrapper.orderByDesc("created_at");
        Page<Review> result = reviewMapper.selectPage(new Page<>(page, Math.min(size, 100)), wrapper);
        return PageResult.of(result.getTotal(), page, result.getSize(), enrich(result.getRecords()));
    }

    public ReviewVO detail(Long id) {
        Review review = getEntity(id);
        Requirement requirement = requirementService.getVisible(review.getRequirementId());
        return enrich(List.of(review)).get(0);
    }

    public List<ReviewVO> listByRequirement(Long requirementId) {
        return enrich(reviewMapper.selectList(new QueryWrapper<Review>()
                .eq("requirement_id", requirementId).orderByDesc("created_at")));
    }

    private Review getEntity(Long id) {
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "评审单不存在");
        }
        return review;
    }

    private List<ReviewVO> enrich(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return List.of();
        }
        Map<Long, String> users = lookupService.userNames();
        List<Long> ids = reviews.stream().map(Review::getId).toList();
        Map<Long, List<ReviewOpinion>> opinions = opinionMapper
                .selectList(new QueryWrapper<ReviewOpinion>().in("review_id", ids))
                .stream().collect(Collectors.groupingBy(ReviewOpinion::getReviewId));
        Map<Long, List<ReviewParticipant>> participants = participantMapper
                .selectList(new QueryWrapper<ReviewParticipant>().in("review_id", ids))
                .stream().collect(Collectors.groupingBy(ReviewParticipant::getReviewId));
        Map<Long, Requirement> requirements = requirementMapper
                .selectList(new QueryWrapper<Requirement>().in("id",
                        reviews.stream().map(Review::getRequirementId).distinct().toList()))
                .stream().collect(Collectors.toMap(Requirement::getId, r -> r));
        List<ReviewVO> result = new ArrayList<>();
        for (Review review : reviews) {
            ReviewVO vo = new ReviewVO();
            BeanUtils.copyProperties(review, vo);
            Requirement requirement = requirements.get(review.getRequirementId());
            if (requirement != null) {
                vo.setRequirementNo(requirement.getReqNo());
                vo.setRequirementTitle(requirement.getTitle());
            }
            vo.setHostName(users.get(review.getHostId()));
            List<Long> reviewerIds = participants.getOrDefault(review.getId(), List.of()).stream()
                    .map(ReviewParticipant::getUserId).toList();
            vo.setReviewerIds(reviewerIds);
            vo.setReviewerNames(reviewerIds.stream().map(users::get)
                    .filter(Objects::nonNull).collect(Collectors.joining("、")));
            vo.setStatusLabel(dictService.reviewStatusLabel(review.getStatus()));
            vo.setOpinions(opinions.getOrDefault(review.getId(), List.of()).stream().map(o -> {
                ReviewOpinionVO ov = new ReviewOpinionVO();
                BeanUtils.copyProperties(o, ov);
                ov.setReviewerName(users.get(o.getReviewerId()));
                ov.setResultLabel(dictService.reviewResultLabel(o.getResult()));
                return ov;
            }).toList());
            result.add(vo);
        }
        return result;
    }

    @Transactional
    public Long create(ReviewCreateRequest request) {
        Requirement requirement = requirementService.getVisible(request.getRequirementId());
        if (!RequirementStatus.ANALYZING.name().equals(requirement.getStatus())) {
            throw BizException.of(ErrorCode.ILLEGAL_TRANSITION, "只有分析中的需求可以发起评审");
        }
        Long pending = reviewMapper.selectCount(new QueryWrapper<Review>()
                .eq("requirement_id", requirement.getId())
                .eq("status", ReviewStatus.PENDING.name()));
        if (pending != null && pending > 0) {
            throw BizException.of(ErrorCode.ILLEGAL_TRANSITION, "该需求已有进行中的评审");
        }
        LoginUser user = SecurityUtils.currentUser();
        Review review = new Review();
        review.setReviewNo(bizNoGenerator.next("RV"));
        review.setRequirementId(requirement.getId());
        review.setType(request.getType());
        review.setMethod(request.getMethod());
        review.setHostId(request.getHostId() == null ? user.getId() : request.getHostId());
        review.setDeadline(request.getDeadline() == null ? LocalDateTime.now().plusDays(3) : request.getDeadline());
        review.setStatus(ReviewStatus.PENDING.name());
        review.setComment(request.getComment());
        review.setSnapshotJson(snapshot(requirement));
        reviewMapper.insert(review);
        request.getReviewerIds().stream().distinct().forEach(reviewerId -> {
            ReviewParticipant participant = new ReviewParticipant();
            participant.setReviewId(review.getId());
            participant.setUserId(reviewerId);
            participant.setParticipantType(ReviewParticipant.TYPE_REVIEWER);
            participantMapper.insert(participant);
        });

        flowService.apply(requirement, RequirementAction.SUBMIT_REVIEW, user);
        requirementService.persist(requirement);
        auditAspect.record("评审管理", "发起评审", "REQUIREMENT", String.valueOf(requirement.getId()));
        return review.getId();
    }

    @Transactional
    public void addOpinion(Long id, OpinionRequest request) {
        Review review = getEntity(id);
        if (!ReviewStatus.PENDING.name().equals(review.getStatus())) {
            throw BizException.of(ErrorCode.ILLEGAL_TRANSITION, "评审已结束，不能继续提交意见");
        }
        LoginUser user = SecurityUtils.currentUser();
        boolean reviewer = participantMapper.selectCount(new QueryWrapper<ReviewParticipant>()
                .eq("review_id", id).eq("user_id", user.getId())) > 0;
        if (!reviewer && !SecurityUtils.hasPermission(user, Permissions.ADMIN)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "只有评审人才能提交意见");
        }
        ReviewResult.valueOf(request.getResult());
        ReviewOpinion opinion = new ReviewOpinion();
        opinion.setReviewId(id);
        opinion.setReviewerId(user.getId());
        opinion.setResult(request.getResult());
        opinion.setContent(request.getContent());
        opinionMapper.insert(opinion);
        auditAspect.record("评审管理", "提交评审意见", "REQUIREMENT", String.valueOf(review.getRequirementId()));
    }

    @Transactional
    public void conclude(Long id, ReviewConclusionRequest request) {
        Review review = getEntity(id);
        if (!ReviewStatus.PENDING.name().equals(review.getStatus())) {
            throw BizException.of(ErrorCode.ILLEGAL_TRANSITION, "评审单已结束");
        }
        LoginUser user = SecurityUtils.currentUser();
        if (!SecurityUtils.hasPermission(user, Permissions.ADMIN) && !Objects.equals(review.getHostId(), user.getId())) {
            throw BizException.of(ErrorCode.FORBIDDEN, "只有主持人才可以提交评审结论");
        }
        Requirement requirement = requirementService.getVisible(review.getRequirementId());
        review.setStatus(request.getPass() ? ReviewStatus.PASSED.name() : ReviewStatus.REJECTED.name());
        review.setConclusion(request.getPass() ? "PASS" : "REJECT");
        review.setComment(request.getComment());
        review.setClosedAt(LocalDateTime.now());
        reviewMapper.updateById(review);

        if (request.getPass()) {
            flowService.apply(requirement, RequirementAction.REVIEW_PASS, user);
            requirement.setRejectReason(null);
            requirementService.persist(requirement);
            requirementService.snapshot(requirement, "评审通过");
            auditAspect.record("评审管理", "评审通过", "REQUIREMENT", String.valueOf(requirement.getId()));
        } else {
            flowService.apply(requirement, RequirementAction.REVIEW_REJECT, user);
            requirement.setRejectReason(request.getComment());
            requirementService.persist(requirement);
            auditAspect.record("评审管理", "评审驳回", "REQUIREMENT", String.valueOf(requirement.getId()));
        }
    }

    private String snapshot(Requirement requirement) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("title", requirement.getTitle());
            map.put("description", requirement.getDescription());
            map.put("acceptanceCriteria", requirement.getAcceptanceCriteria());
            map.put("version", requirement.getVersion());
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}
