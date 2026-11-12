package com.reqflow.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.reqflow.common.Permissions;
import com.reqflow.common.SecurityUtils;
import com.reqflow.domain.entity.*;
import com.reqflow.domain.enums.*;
import com.reqflow.mapper.*;
import com.reqflow.security.LoginUser;
import com.reqflow.service.support.DataScopeService;
import com.reqflow.service.support.LookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 看板统计：分组计数交给数据库完成，只把聚合结果带回内存，
 * 避免把全部可见需求加载到 JVM 再聚合（数据量增长后不可用）。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    /** 待办只关心在途与可操作状态，归档/撤销数据不参与内存计算 */
    private static final Set<String> TODO_STATUSES = Set.of(
            RequirementStatus.DRAFT.name(), RequirementStatus.PENDING_ACCEPT.name(),
            RequirementStatus.ANALYZING.name(), RequirementStatus.PENDING_REVIEW.name(),
            RequirementStatus.REVIEW_PASSED.name(), RequirementStatus.SCHEDULED.name(),
            RequirementStatus.DEVELOPING.name(), RequirementStatus.TESTING.name(),
            RequirementStatus.PENDING_ACCEPTANCE.name(), RequirementStatus.REJECTED.name());

    private static final Set<String> IN_FLIGHT = Set.of(
            RequirementStatus.PENDING_ACCEPT.name(), RequirementStatus.ANALYZING.name(),
            RequirementStatus.PENDING_REVIEW.name(), RequirementStatus.REVIEW_PASSED.name(),
            RequirementStatus.SCHEDULED.name(), RequirementStatus.DEVELOPING.name(),
            RequirementStatus.TESTING.name(), RequirementStatus.PENDING_ACCEPTANCE.name(),
            RequirementStatus.ON_HOLD.name());

    private final RequirementMapper requirementMapper;
    private final TaskMapper taskMapper;
    private final ReviewMapper reviewMapper;
    private final ReviewParticipantMapper reviewParticipantMapper;
    private final ChangeRequestMapper changeMapper;
    private final AcceptanceMapper acceptanceMapper;
    private final DataScopeService dataScopeService;
    private final LookupService lookupService;

    public Map<String, Object> summary() {
        LoginUser user = SecurityUtils.currentUser();
        Map<String, Long> byStatus = countByStatus(user);
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long inFlight = IN_FLIGHT.stream().mapToLong(s -> byStatus.getOrDefault(s, 0L)).sum();
        long pendingAcceptance = byStatus.getOrDefault(RequirementStatus.PENDING_ACCEPTANCE.name(), 0L);
        long archived = byStatus.getOrDefault(RequirementStatus.ARCHIVED.name(), 0L);
        Set<String> finished = Set.of(RequirementStatus.ACCEPTED.name(), RequirementStatus.ARCHIVED.name(),
                RequirementStatus.CANCELLED.name());
        long overdue = countOverdue(user, finished);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("inFlight", inFlight);
        result.put("pendingAcceptance", pendingAcceptance);
        result.put("overdue", overdue);
        result.put("myTodo", todo().size());
        result.put("archived", archived);
        return result;
    }

    public List<Map<String, Object>> statusDistribution() {
        Map<String, Long> counts = countByStatus(SecurityUtils.currentUser());
        List<Map<String, Object>> list = new ArrayList<>();
        for (RequirementStatus status : RequirementStatus.values()) {
            Long count = counts.get(status.name());
            if (count != null && count > 0) {
                list.add(Map.of("name", status.label(), "value", count));
            }
        }
        return list;
    }

    public List<Map<String, Object>> deptDistribution() {
        LoginUser user = SecurityUtils.currentUser();
        QueryWrapper<Requirement> wrapper = new QueryWrapper<>();
        wrapper.select("dept_id", "count(*) as cnt");
        dataScopeService.apply(wrapper, user);
        wrapper.groupBy("dept_id");
        Map<Long, String> depts = lookupService.deptNames();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Map<String, Object> row : requirementMapper.selectMaps(wrapper)) {
            Object deptId = column(row, "dept_id");
            String name = deptId == null ? "未分配"
                    : depts.getOrDefault(((Number) deptId).longValue(), "未分配");
            counts.merge(name, countOf(row), Long::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> Map.<String, Object>of("name", e.getKey(), "value", e.getValue()))
                .toList();
    }

    public List<Map<String, Object>> trend() {
        LoginUser user = SecurityUtils.currentUser();
        YearMonth firstMonth = YearMonth.now().minusMonths(5);
        QueryWrapper<Requirement> wrapper = new QueryWrapper<>();
        wrapper.select("YEAR(created_at) as y", "MONTH(created_at) as m", "count(*) as cnt");
        wrapper.ge("created_at", firstMonth.atDay(1).atStartOfDay());
        dataScopeService.apply(wrapper, user);
        wrapper.groupBy("YEAR(created_at)", "MONTH(created_at)");
        Map<String, Long> counts = new HashMap<>();
        for (Map<String, Object> row : requirementMapper.selectMaps(wrapper)) {
            Object y = column(row, "y");
            Object m = column(row, "m");
            if (y instanceof Number year && m instanceof Number month) {
                counts.put("%04d-%02d".formatted(year.intValue(), month.intValue()), countOf(row));
            }
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            list.add(Map.of("month", month.format(formatter),
                    "value", counts.getOrDefault(month.format(formatter), 0L)));
        }
        return list;
    }

    public List<Map<String, Object>> todo() {
        LoginUser user = SecurityUtils.currentUser();
        List<Map<String, Object>> items = new ArrayList<>();
        List<Requirement> visible = todoRequirements(user);

        if (SecurityUtils.hasPermission(user, Permissions.REQUIREMENT_ACCEPT)) {
            visible.stream().filter(r -> RequirementStatus.PENDING_ACCEPT.name().equals(r.getStatus())).limit(4)
                    .forEach(r -> items.add(requirementTodo("待受理", r, "待受理")));
            visible.stream().filter(r -> RequirementStatus.ANALYZING.name().equals(r.getStatus())).limit(3)
                    .forEach(r -> items.add(requirementTodo("分析中", r, "待编写规格")));
        }
        if (SecurityUtils.hasPermission(user, Permissions.REQUIREMENT_SCHEDULE)) {
            visible.stream().filter(r -> RequirementStatus.REVIEW_PASSED.name().equals(r.getStatus())).limit(4)
                    .forEach(r -> items.add(requirementTodo("待排期", r, "评审通过待排期")));
        }
        if (SecurityUtils.hasPermission(user, Permissions.REQUIREMENT_SUBMIT)) {
            visible.stream().filter(r -> RequirementStatus.REJECTED.name().equals(r.getStatus())
                    && r.getProposerId().equals(user.getId())).limit(3)
                    .forEach(r -> items.add(requirementTodo("待修改", r, "评审驳回")));
            visible.stream().filter(r -> RequirementStatus.DRAFT.name().equals(r.getStatus())
                    && r.getProposerId().equals(user.getId())).limit(3)
                    .forEach(r -> items.add(requirementTodo("草稿", r, "待提交")));
        }

        List<Review> pendingReviews = reviewMapper.selectList(new QueryWrapper<Review>()
                .eq("status", ReviewStatus.PENDING.name()).orderByAsc("deadline"));
        Set<Long> myReviewIds = pendingReviews.isEmpty() ? Set.of()
                : reviewParticipantMapper.selectList(new QueryWrapper<ReviewParticipant>()
                        .in("review_id", pendingReviews.stream().map(Review::getId).toList())
                        .eq("user_id", user.getId()))
                .stream().map(ReviewParticipant::getReviewId).collect(java.util.stream.Collectors.toSet());
        Map<Long, Requirement> requirementMap = visible.stream()
                .collect(java.util.stream.Collectors.toMap(Requirement::getId, r -> r));
        for (Review review : pendingReviews) {
            boolean reviewer = myReviewIds.contains(review.getId());
            if ((reviewer || Objects.equals(review.getHostId(), user.getId()))
                    && requirementMap.containsKey(review.getRequirementId())) {
                Requirement requirement = requirementMap.get(review.getRequirementId());
                items.add(todoItem("待评审", review.getReviewNo() + " " + requirement.getTitle(),
                        "REVIEW", review.getId(), review.getReviewNo(), "PENDING", review.getDeadline()));
            }
        }

        if (SecurityUtils.hasPermission(user, Permissions.CHANGE_APPROVE)) {
            changeMapper.selectList(new QueryWrapper<ChangeRequest>()
                            .eq("status", ChangeStatus.PENDING.name()).orderByDesc("created_at").last("limit 4"))
                    .stream().filter(c -> requirementMap.containsKey(c.getRequirementId()))
                    .forEach(c -> {
                        Requirement requirement = requirementMap.get(c.getRequirementId());
                        items.add(todoItem("待审批", c.getChangeNo() + " " + requirement.getTitle(),
                                "CHANGE", c.getId(), c.getChangeNo(), "PENDING", c.getCreatedAt()));
                    });
        }

        acceptanceMapper.selectList(new QueryWrapper<Acceptance>()
                        .eq("status", AcceptanceStatus.PENDING.name())
                        .eq("acceptor_id", user.getId()).orderByDesc("submitted_at").last("limit 5"))
                .stream().filter(a -> requirementMap.containsKey(a.getRequirementId()))
                .forEach(a -> {
                    Requirement requirement = requirementMap.get(a.getRequirementId());
                    items.add(todoItem("待验收", a.getAcceptanceNo() + " " + requirement.getTitle(),
                            "ACCEPTANCE", a.getId(), a.getAcceptanceNo(), "PENDING", a.getSubmittedAt()));
                });

        taskMapper.selectList(new QueryWrapper<Task>()
                        .eq("assignee_id", user.getId())
                        .notIn("status", List.of(TaskStatus.DONE.name(), TaskStatus.CANCELLED.name()))
                        .orderByAsc("plan_date").last("limit 5"))
                .stream().filter(t -> requirementMap.containsKey(t.getRequirementId()))
                .forEach(t -> items.add(todoItem("任务", t.getTaskNo() + " " + t.getName(),
                        "TASK", t.getId(), t.getTaskNo(), t.getStatus(), t.getPlanDate() == null
                                ? null : t.getPlanDate().atStartOfDay())));

        return items.stream().limit(12).toList();
    }

    private Map<String, Long> countByStatus(LoginUser user) {
        QueryWrapper<Requirement> wrapper = new QueryWrapper<>();
        wrapper.select("status", "count(*) as cnt");
        dataScopeService.apply(wrapper, user);
        wrapper.groupBy("status");
        Map<String, Long> counts = new HashMap<>();
        for (Map<String, Object> row : requirementMapper.selectMaps(wrapper)) {
            Object status = column(row, "status");
            if (status != null) {
                counts.put(String.valueOf(status), countOf(row));
            }
        }
        return counts;
    }

    private long countOverdue(LoginUser user, Set<String> finishedStatuses) {
        QueryWrapper<Requirement> wrapper = new QueryWrapper<>();
        wrapper.isNotNull("expect_date");
        wrapper.lt("expect_date", LocalDate.now());
        wrapper.notIn("status", finishedStatuses);
        dataScopeService.apply(wrapper, user);
        return requirementMapper.selectCount(wrapper);
    }

    private List<Requirement> todoRequirements(LoginUser user) {
        QueryWrapper<Requirement> wrapper = new QueryWrapper<>();
        wrapper.in("status", TODO_STATUSES);
        wrapper.last("limit 200");
        dataScopeService.apply(wrapper, user);
        return requirementMapper.selectList(wrapper);
    }

    private Object column(Map<String, Object> row, String name) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private long countOf(Map<String, Object> row) {
        Object value = column(row, "cnt");
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private Map<String, Object> requirementTodo(String type, Requirement requirement, String statusLabel) {
        return todoItem(type, requirement.getReqNo() + " " + requirement.getTitle(),
                "REQUIREMENT", requirement.getId(), requirement.getReqNo(), statusLabel, requirement.getUpdatedAt());
    }

    private Map<String, Object> todoItem(String type, String title, String refType, Long refId,
                                         String refNo, String status, java.time.LocalDateTime time) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", type);
        item.put("title", title);
        item.put("refType", refType);
        item.put("refId", refId);
        item.put("refNo", refNo);
        item.put("status", status);
        item.put("time", time);
        return item;
    }
}
