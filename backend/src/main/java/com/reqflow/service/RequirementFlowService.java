package com.reqflow.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.reqflow.common.BizException;
import com.reqflow.common.ErrorCode;
import com.reqflow.common.Permissions;
import com.reqflow.common.SecurityUtils;
import com.reqflow.domain.entity.Requirement;
import com.reqflow.domain.entity.Task;
import com.reqflow.domain.enums.RequirementAction;
import com.reqflow.domain.enums.RequirementStatus;
import com.reqflow.domain.enums.TaskStatus;
import com.reqflow.domain.enums.TaskType;
import com.reqflow.mapper.TaskMapper;
import com.reqflow.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 需求状态机：状态枚举 + 流转表 + 统一校验入口。
 * <p>
 * 流转表约束的是“权限点”而不是角色码：角色只是权限点的集合，
 * 新增或调整角色授权只改数据，不改代码。
 * 前端只提交动作编码，目标状态由服务端计算，避免直接篡改状态。
 */
@Service
@RequiredArgsConstructor
public class RequirementFlowService {

    private final TaskMapper taskMapper;

    private record Transition(Set<String> from, String to, Set<String> permissions) {
    }

    private static final Map<RequirementAction, Transition> TRANSITIONS = new EnumMap<>(RequirementAction.class);

    static {
        TRANSITIONS.put(RequirementAction.SUBMIT, new Transition(
                Set.of(RequirementStatus.DRAFT.name()), RequirementStatus.PENDING_ACCEPT.name(),
                Set.of(Permissions.REQUIREMENT_SUBMIT)));
        TRANSITIONS.put(RequirementAction.ACCEPT, new Transition(
                Set.of(RequirementStatus.PENDING_ACCEPT.name()), RequirementStatus.ANALYZING.name(),
                Set.of(Permissions.REQUIREMENT_ACCEPT)));
        TRANSITIONS.put(RequirementAction.SUBMIT_REVIEW, new Transition(
                Set.of(RequirementStatus.ANALYZING.name()), RequirementStatus.PENDING_REVIEW.name(),
                Set.of(Permissions.REQUIREMENT_SUBMIT_REVIEW)));
        TRANSITIONS.put(RequirementAction.REVIEW_PASS, new Transition(
                Set.of(RequirementStatus.PENDING_REVIEW.name()), RequirementStatus.REVIEW_PASSED.name(),
                Set.of(Permissions.REQUIREMENT_REVIEW_DECIDE)));
        TRANSITIONS.put(RequirementAction.REVIEW_REJECT, new Transition(
                Set.of(RequirementStatus.PENDING_REVIEW.name()), RequirementStatus.REJECTED.name(),
                Set.of(Permissions.REQUIREMENT_REVIEW_DECIDE)));
        TRANSITIONS.put(RequirementAction.RESUBMIT, new Transition(
                Set.of(RequirementStatus.REJECTED.name()), RequirementStatus.ANALYZING.name(),
                Set.of(Permissions.REQUIREMENT_SUBMIT)));
        TRANSITIONS.put(RequirementAction.SCHEDULE, new Transition(
                Set.of(RequirementStatus.REVIEW_PASSED.name()), RequirementStatus.SCHEDULED.name(),
                Set.of(Permissions.REQUIREMENT_SCHEDULE)));
        TRANSITIONS.put(RequirementAction.START_DEV, new Transition(
                Set.of(RequirementStatus.SCHEDULED.name()), RequirementStatus.DEVELOPING.name(),
                Set.of(Permissions.REQUIREMENT_DEVELOP)));
        TRANSITIONS.put(RequirementAction.SUBMIT_TEST, new Transition(
                Set.of(RequirementStatus.DEVELOPING.name()), RequirementStatus.TESTING.name(),
                Set.of(Permissions.REQUIREMENT_SUBMIT_TEST)));
        TRANSITIONS.put(RequirementAction.SUBMIT_ACCEPTANCE, new Transition(
                Set.of(RequirementStatus.TESTING.name()), RequirementStatus.PENDING_ACCEPTANCE.name(),
                Set.of(Permissions.REQUIREMENT_SUBMIT_ACCEPTANCE)));
        TRANSITIONS.put(RequirementAction.ACCEPT_PASS, new Transition(
                Set.of(RequirementStatus.PENDING_ACCEPTANCE.name()), RequirementStatus.ACCEPTED.name(),
                Set.of(Permissions.REQUIREMENT_ACCEPT_DECIDE)));
        TRANSITIONS.put(RequirementAction.ACCEPT_REJECT, new Transition(
                Set.of(RequirementStatus.PENDING_ACCEPTANCE.name()), RequirementStatus.DEVELOPING.name(),
                Set.of(Permissions.REQUIREMENT_ACCEPT_DECIDE)));
        TRANSITIONS.put(RequirementAction.ARCHIVE, new Transition(
                Set.of(RequirementStatus.ACCEPTED.name()), RequirementStatus.ARCHIVED.name(),
                Set.of(Permissions.REQUIREMENT_ARCHIVE)));
        TRANSITIONS.put(RequirementAction.HOLD, new Transition(
                Set.of(RequirementStatus.PENDING_ACCEPT.name(), RequirementStatus.ANALYZING.name(),
                        RequirementStatus.PENDING_REVIEW.name(), RequirementStatus.REVIEW_PASSED.name(),
                        RequirementStatus.SCHEDULED.name(), RequirementStatus.DEVELOPING.name(),
                        RequirementStatus.TESTING.name(), RequirementStatus.PENDING_ACCEPTANCE.name()),
                RequirementStatus.ON_HOLD.name(), Set.of(Permissions.REQUIREMENT_HOLD)));
        TRANSITIONS.put(RequirementAction.RESUME, new Transition(
                Set.of(RequirementStatus.ON_HOLD.name()), null, Set.of(Permissions.REQUIREMENT_HOLD)));
        TRANSITIONS.put(RequirementAction.CANCEL, new Transition(
                Set.of(RequirementStatus.DRAFT.name(), RequirementStatus.PENDING_ACCEPT.name(),
                        RequirementStatus.ANALYZING.name(), RequirementStatus.PENDING_REVIEW.name(),
                        RequirementStatus.REVIEW_PASSED.name(), RequirementStatus.SCHEDULED.name(),
                        RequirementStatus.DEVELOPING.name(), RequirementStatus.TESTING.name(),
                        RequirementStatus.PENDING_ACCEPTANCE.name(), RequirementStatus.REJECTED.name()),
                RequirementStatus.CANCELLED.name(), Set.of(Permissions.REQUIREMENT_CANCEL)));
    }

    /** 由评审单/验收单驱动的动作，禁止通过通用动作接口直接调用 */
    private static final Set<RequirementAction> DOCUMENT_DRIVEN = EnumSet.of(
            RequirementAction.SUBMIT_REVIEW, RequirementAction.REVIEW_PASS, RequirementAction.REVIEW_REJECT,
            RequirementAction.SUBMIT_ACCEPTANCE, RequirementAction.ACCEPT_PASS, RequirementAction.ACCEPT_REJECT);

    public static boolean isDocumentDriven(RequirementAction action) {
        return DOCUMENT_DRIVEN.contains(action);
    }

    public TransitionResult apply(Requirement requirement, RequirementAction action, LoginUser user) {
        validate(requirement, action, user);
        String from = requirement.getStatus();
        if (action == RequirementAction.HOLD) {
            requirement.setHoldFromStatus(requirement.getStatus());
            requirement.setStatus(RequirementStatus.ON_HOLD.name());
        } else if (action == RequirementAction.RESUME) {
            if (requirement.getHoldFromStatus() == null) {
                throw BizException.of(ErrorCode.ILLEGAL_TRANSITION, "缺少挂起前状态，无法恢复");
            }
            requirement.setStatus(requirement.getHoldFromStatus());
            requirement.setHoldFromStatus(null);
        } else {
            requirement.setStatus(TRANSITIONS.get(action).to());
        }
        if (action == RequirementAction.REVIEW_PASS
                && ("V0.1".equals(requirement.getVersion()) || requirement.getVersion() == null)) {
            requirement.setVersion("V1.0");
        }
        if (action == RequirementAction.ARCHIVE) {
            requirement.setArchivedAt(LocalDateTime.now());
            requirement.setArchivedBy(user.getId());
        }
        return new TransitionResult(from, requirement.getStatus());
    }

    public void validate(Requirement requirement, RequirementAction action, LoginUser user) {
        if (RequirementStatus.ARCHIVED.name().equals(requirement.getStatus())) {
            throw BizException.of(ErrorCode.READONLY);
        }
        Transition transition = TRANSITIONS.get(action);
        if (transition == null) {
            throw BizException.of(ErrorCode.ILLEGAL_TRANSITION, "未知动作：" + action.name());
        }
        if (!transition.from().contains(requirement.getStatus())) {
            throw BizException.of(ErrorCode.ILLEGAL_TRANSITION,
                    "当前状态【%s】不允许执行【%s】".formatted(
                            RequirementStatus.labelOf(requirement.getStatus()), action.label()));
        }
        boolean permissionAllowed = transition.permissions().stream()
                .anyMatch(p -> SecurityUtils.hasPermission(user, p));
        if (!permissionAllowed) {
            throw BizException.of(ErrorCode.FORBIDDEN, "当前角色无权执行【" + action.label() + "】");
        }
        boolean ownerOnly = action == RequirementAction.SUBMIT || action == RequirementAction.RESUBMIT;
        if (ownerOnly && !SecurityUtils.hasPermission(user, Permissions.ADMIN)
                && !requirement.getProposerId().equals(user.getId())) {
            throw BizException.of(ErrorCode.FORBIDDEN, "只有需求提出人可以执行该操作");
        }
        if (action == RequirementAction.SUBMIT) {
            List<String> missing = new ArrayList<>();
            if (isBlank(requirement.getTitle())) missing.add("需求标题");
            if (isBlank(requirement.getType())) missing.add("需求类型");
            if (isBlank(requirement.getSource())) missing.add("需求来源");
            if (requirement.getDeptId() == null) missing.add("业务部门");
            if (isBlank(requirement.getPriority())) missing.add("优先级");
            if (requirement.getExpectDate() == null) missing.add("期望交付日期");
            if (isBlank(requirement.getBackground())) missing.add("业务背景");
            if (isBlank(requirement.getDescription())) missing.add("需求描述");
            if (isBlank(requirement.getAcceptanceCriteria())) missing.add("验收标准");
            if (!missing.isEmpty()) {
                throw new BizException(ErrorCode.VALIDATION_ERROR.getCode(),
                        "提交前请补全：" + String.join("、", missing));
            }
        }
        if (action == RequirementAction.START_DEV) {
            Long devTasks = taskMapper.selectCount(new QueryWrapper<Task>()
                    .eq("requirement_id", requirement.getId())
                    .eq("task_type", TaskType.DEV.name())
                    .ne("status", TaskStatus.CANCELLED.name()));
            if (devTasks == null || devTasks == 0) {
                throw BizException.of(ErrorCode.ILLEGAL_TRANSITION, "请先拆分至少一个开发任务");
            }
        }
        if (action == RequirementAction.SUBMIT_TEST) {
            Long unfinished = taskMapper.selectCount(new QueryWrapper<Task>()
                    .eq("requirement_id", requirement.getId())
                    .eq("task_type", TaskType.DEV.name())
                    .ne("status", TaskStatus.DONE.name())
                    .ne("status", TaskStatus.CANCELLED.name()));
            if (unfinished != null && unfinished > 0) {
                throw BizException.of(ErrorCode.ILLEGAL_TRANSITION,
                        "仍有 " + unfinished + " 个开发任务未完成，不能提交测试");
            }
        }
    }

    public List<String> availableActions(Requirement requirement, LoginUser user) {
        if (RequirementStatus.ARCHIVED.name().equals(requirement.getStatus())) {
            return List.of();
        }
        List<String> actions = new ArrayList<>();
        for (RequirementAction action : RequirementAction.values()) {
            if (DOCUMENT_DRIVEN.contains(action) || action == RequirementAction.SUBMIT_REVIEW) {
                continue;
            }
            Transition transition = TRANSITIONS.get(action);
            if (transition == null || !transition.from().contains(requirement.getStatus())) {
                continue;
            }
            boolean permissionAllowed = transition.permissions().stream()
                    .anyMatch(p -> SecurityUtils.hasPermission(user, p));
            if (!permissionAllowed) {
                continue;
            }
            if ((action == RequirementAction.SUBMIT || action == RequirementAction.RESUBMIT)
                    && !SecurityUtils.hasPermission(user, Permissions.ADMIN)
                    && !requirement.getProposerId().equals(user.getId())) {
                continue;
            }
            actions.add(action.name());
        }
        return actions;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record TransitionResult(String from, String to) {
    }
}
