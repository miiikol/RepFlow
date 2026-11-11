package com.reqflow.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reqflow.audit.AuditAspect;
import com.reqflow.common.BizException;
import com.reqflow.common.ErrorCode;
import com.reqflow.common.PageResult;
import com.reqflow.common.SecurityUtils;
import com.reqflow.domain.entity.Milestone;
import com.reqflow.domain.entity.Requirement;
import com.reqflow.domain.entity.Task;
import com.reqflow.domain.enums.RequirementStatus;
import com.reqflow.domain.enums.TaskStatus;
import com.reqflow.domain.enums.TaskType;
import com.reqflow.dto.TaskCreateRequest;
import com.reqflow.dto.TaskStatusRequest;
import com.reqflow.mapper.MilestoneMapper;
import com.reqflow.mapper.RequirementMapper;
import com.reqflow.mapper.TaskMapper;
import com.reqflow.security.LoginUser;
import com.reqflow.service.support.BizNoGenerator;
import com.reqflow.service.support.DataScopeService;
import com.reqflow.service.support.DictService;
import com.reqflow.service.support.LookupService;
import com.reqflow.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final Map<String, Set<String>> TASK_FLOW = Map.of(
            TaskStatus.TODO.name(), Set.of(TaskStatus.IN_PROGRESS.name(), TaskStatus.BLOCKED.name(),
                    TaskStatus.CANCELLED.name()),
            TaskStatus.IN_PROGRESS.name(), Set.of(TaskStatus.DONE.name(), TaskStatus.BLOCKED.name(),
                    TaskStatus.CANCELLED.name()),
            TaskStatus.BLOCKED.name(), Set.of(TaskStatus.IN_PROGRESS.name(), TaskStatus.CANCELLED.name())
    );

    private final TaskMapper taskMapper;
    private final RequirementMapper requirementMapper;
    private final MilestoneMapper milestoneMapper;
    private final RequirementService requirementService;
    private final DataScopeService dataScopeService;
    private final BizNoGenerator bizNoGenerator;
    private final DictService dictService;
    private final LookupService lookupService;
    private final AuditAspect auditAspect;

    public PageResult<TaskVO> list(long page, long size, Long requirementId, Long assigneeId, String status,
                                   Long projectId, Boolean mine) {
        LoginUser user = SecurityUtils.currentUser();
        QueryWrapper<Task> wrapper = new QueryWrapper<>();
        if (requirementId != null) {
            requirementService.getVisible(requirementId);
            wrapper.eq("requirement_id", requirementId);
        } else {
            dataScopeService.applyByRequirement(wrapper, "requirement_id", user);
        }
        if (assigneeId != null) {
            wrapper.eq("assignee_id", assigneeId);
        }
        if (Boolean.TRUE.equals(mine)) {
            wrapper.eq("assignee_id", user.getId());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        if (projectId != null) {
            wrapper.eq("project_id", projectId);
        }
        wrapper.orderByAsc("status").orderByAsc("plan_date");
        Page<Task> result = taskMapper.selectPage(new Page<>(page, Math.min(size, 100)), wrapper);
        return PageResult.of(result.getTotal(), page, result.getSize(), enrich(result.getRecords()));
    }

    public List<TaskVO> enrich(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return List.of();
        }
        Map<Long, String> users = lookupService.userNames();
        Map<Long, Requirement> requirements = requirementMapper.selectList(new QueryWrapper<Requirement>()
                        .in("id", tasks.stream().map(Task::getRequirementId).distinct().toList()))
                .stream().collect(Collectors.toMap(Requirement::getId, r -> r));
        List<TaskVO> result = new ArrayList<>();
        for (Task task : tasks) {
            TaskVO vo = new TaskVO();
            BeanUtils.copyProperties(task, vo);
            vo.setAssigneeName(users.get(task.getAssigneeId()));
            Requirement requirement = requirements.get(task.getRequirementId());
            if (requirement != null) {
                vo.setRequirementNo(requirement.getReqNo());
                vo.setRequirementTitle(requirement.getTitle());
            }
            vo.setStatusLabel(dictService.taskStatusLabel(task.getStatus()));
            vo.setTypeLabel(dictService.taskTypeLabel(task.getType()));
            vo.setOverdue(task.getPlanDate() != null && task.getPlanDate().isBefore(LocalDate.now())
                    && !Set.of(TaskStatus.DONE.name(), TaskStatus.CANCELLED.name()).contains(task.getStatus()));
            result.add(vo);
        }
        return result;
    }

    @Transactional
    public Long create(TaskCreateRequest request) {
        Requirement requirement = requirementService.getVisible(request.getRequirementId());
        if (RequirementStatus.ARCHIVED.name().equals(requirement.getStatus())
                || RequirementStatus.CANCELLED.name().equals(requirement.getStatus())) {
            throw BizException.of(ErrorCode.READONLY);
        }
        TaskType.valueOf(request.getType());
        Long milestoneId = request.getMilestoneId() == null ? requirement.getMilestoneId() : request.getMilestoneId();
        if (milestoneId != null) {
            Milestone milestone = milestoneMapper.selectById(milestoneId);
            if (milestone == null || (requirement.getProjectId() != null
                    && !milestone.getProjectId().equals(requirement.getProjectId()))) {
                throw BizException.of(ErrorCode.VALIDATION_ERROR, "里程碑不属于需求所在项目");
            }
        }
        Task task = new Task();
        task.setTaskNo(bizNoGenerator.next("TSK"));
        task.setRequirementId(requirement.getId());
        task.setProjectId(requirement.getProjectId());
        task.setMilestoneId(milestoneId);
        task.setParentId(request.getParentId());
        task.setName(request.getName());
        task.setType(request.getType());
        task.setAssigneeId(request.getAssigneeId());
        task.setEstimateHours(request.getEstimateHours());
        task.setActualHours(BigDecimal.ZERO);
        task.setPlanDate(request.getPlanDate());
        task.setStatus(TaskStatus.TODO.name());
        taskMapper.insert(task);
        auditAspect.record("计划任务", "新建任务", "REQUIREMENT", String.valueOf(requirement.getId()));
        return task.getId();
    }

    @Transactional
    public void updateStatus(Long id, TaskStatusRequest request) {
        Task task = getEntity(id);
        requirementService.getVisible(task.getRequirementId());
        TaskStatus target;
        try {
            target = TaskStatus.valueOf(request.getStatus());
        } catch (Exception e) {
            throw BizException.of(ErrorCode.VALIDATION_ERROR, "未知任务状态");
        }
        Set<String> allowed = TASK_FLOW.getOrDefault(task.getStatus(), Set.of());
        if (!allowed.contains(target.name())) {
            throw BizException.of(ErrorCode.ILLEGAL_TRANSITION,
                    "任务状态不能从【" + dictService.taskStatusLabel(task.getStatus())
                            + "】变更为【" + dictService.taskStatusLabel(target.name()) + "】");
        }
        if (target == TaskStatus.DONE && !StringUtils.hasText(request.getFinishNote())) {
            throw BizException.of(ErrorCode.VALIDATION_ERROR, "完成任务必须填写完成说明");
        }
        if (target == TaskStatus.BLOCKED && !StringUtils.hasText(request.getBlockReason())) {
            throw BizException.of(ErrorCode.VALIDATION_ERROR, "阻塞任务必须填写阻塞原因");
        }
        task.setStatus(target.name());
        if (request.getActualHours() != null) {
            task.setActualHours(request.getActualHours());
        }
        if (target == TaskStatus.DONE) {
            task.setFinishNote(request.getFinishNote());
            task.setFinishedAt(LocalDateTime.now());
        }
        if (target == TaskStatus.BLOCKED) {
            task.setBlockReason(request.getBlockReason());
        }
        taskMapper.updateById(task);
        auditAspect.record("计划任务", "任务状态变更为" + dictService.taskStatusLabel(target.name()),
                "REQUIREMENT", String.valueOf(task.getRequirementId()));
    }

    private Task getEntity(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "任务不存在");
        }
        return task;
    }
}
