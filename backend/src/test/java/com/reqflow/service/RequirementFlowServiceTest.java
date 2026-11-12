package com.reqflow.service;

import com.reqflow.common.BizException;
import com.reqflow.common.ErrorCode;
import com.reqflow.domain.entity.Requirement;
import com.reqflow.domain.enums.RequirementAction;
import com.reqflow.domain.enums.RequirementStatus;
import com.reqflow.mapper.TaskMapper;
import com.reqflow.security.LoginUser;
import com.reqflow.support.SecurityTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequirementFlowServiceTest {

    private TaskMapper taskMapper;
    private RequirementFlowService flowService;

    @BeforeEach
    void setUp() {
        taskMapper = mock(TaskMapper.class);
        flowService = new RequirementFlowService(taskMapper);
    }

    private Requirement requirement(String status, long proposerId) {
        Requirement r = new Requirement();
        r.setId(1L);
        r.setReqNo("REQ-202611-0001");
        r.setTitle("测试需求");
        r.setType("NEW_FEATURE");
        r.setSource("BUSINESS_DEPT");
        r.setDeptId(2L);
        r.setProposerId(proposerId);
        r.setPriority("P1");
        r.setExpectDate(LocalDate.now().plusDays(10));
        r.setBackground("背景");
        r.setDescription("描述");
        r.setAcceptanceCriteria("标准");
        r.setStatus(status);
        r.setVersion("V0.1");
        r.setChangeCount(0);
        return r;
    }

    private LoginUser user(long id, String... roles) {
        LoginUser user = new LoginUser();
        user.setId(id);
        user.setUsername("u" + id);
        user.setRealName("用户" + id);
        user.setDeptId(1L);
        user.setStatus(1);
        user.setRoles(List.of(roles));
        user.setPermissions(SecurityTestSupport.permissionsOf(roles));
        user.setDataScope("ALL");
        return user;
    }

    @Test
    void submitDraftSuccess() {
        Requirement r = requirement(RequirementStatus.DRAFT.name(), 2L);
        flowService.apply(r, RequirementAction.SUBMIT, user(2L, "BUSINESS"));
        assertEquals(RequirementStatus.PENDING_ACCEPT.name(), r.getStatus());
    }

    @Test
    void submitByOtherProposerForbidden() {
        Requirement r = requirement(RequirementStatus.DRAFT.name(), 2L);
        BizException e = assertThrows(BizException.class,
                () -> flowService.apply(r, RequirementAction.SUBMIT, user(3L, "BUSINESS")));
        assertEquals(ErrorCode.FORBIDDEN.getCode(), e.getCode());
    }

    @Test
    void acceptByWrongRoleForbidden() {
        Requirement r = requirement(RequirementStatus.PENDING_ACCEPT.name(), 2L);
        BizException e = assertThrows(BizException.class,
                () -> flowService.apply(r, RequirementAction.ACCEPT, user(2L, "BUSINESS")));
        assertEquals(ErrorCode.FORBIDDEN.getCode(), e.getCode());
    }

    @Test
    void acceptByAnalystSuccess() {
        Requirement r = requirement(RequirementStatus.PENDING_ACCEPT.name(), 2L);
        flowService.apply(r, RequirementAction.ACCEPT, user(3L, "RA"));
        assertEquals(RequirementStatus.ANALYZING.name(), r.getStatus());
    }

    @Test
    void illegalTransitionKeepsStatus() {
        Requirement r = requirement(RequirementStatus.ANALYZING.name(), 2L);
        BizException e = assertThrows(BizException.class,
                () -> flowService.apply(r, RequirementAction.SCHEDULE, user(4L, "PM")));
        assertEquals(ErrorCode.ILLEGAL_TRANSITION.getCode(), e.getCode());
        assertEquals(RequirementStatus.ANALYZING.name(), r.getStatus());
    }

    @Test
    void archivedIsReadonly() {
        Requirement r = requirement(RequirementStatus.ARCHIVED.name(), 2L);
        BizException e = assertThrows(BizException.class,
                () -> flowService.apply(r, RequirementAction.CANCEL, user(1L, "ADMIN")));
        assertEquals(ErrorCode.READONLY.getCode(), e.getCode());
    }

    @Test
    void holdAndResumeRestoresPreviousStatus() {
        Requirement r = requirement(RequirementStatus.DEVELOPING.name(), 2L);
        flowService.apply(r, RequirementAction.HOLD, user(4L, "PM"));
        assertEquals(RequirementStatus.ON_HOLD.name(), r.getStatus());
        assertEquals(RequirementStatus.DEVELOPING.name(), r.getHoldFromStatus());
        flowService.apply(r, RequirementAction.RESUME, user(4L, "PM"));
        assertEquals(RequirementStatus.DEVELOPING.name(), r.getStatus());
        assertNull(r.getHoldFromStatus());
    }

    @Test
    void startDevRequiresAtLeastOneDevTask() {
        Requirement r = requirement(RequirementStatus.SCHEDULED.name(), 2L);
        when(taskMapper.selectCount(any())).thenReturn(0L);
        BizException e = assertThrows(BizException.class,
                () -> flowService.apply(r, RequirementAction.START_DEV, user(5L, "DEV")));
        assertEquals(ErrorCode.ILLEGAL_TRANSITION.getCode(), e.getCode());
    }

    @Test
    void startDevWithTaskSuccess() {
        Requirement r = requirement(RequirementStatus.SCHEDULED.name(), 2L);
        when(taskMapper.selectCount(any())).thenReturn(1L);
        flowService.apply(r, RequirementAction.START_DEV, user(5L, "DEV"));
        assertEquals(RequirementStatus.DEVELOPING.name(), r.getStatus());
    }

    @Test
    void submitTestRejectedWhenDevTaskUnfinished() {
        Requirement r = requirement(RequirementStatus.DEVELOPING.name(), 2L);
        when(taskMapper.selectCount(any())).thenReturn(2L);
        BizException e = assertThrows(BizException.class,
                () -> flowService.apply(r, RequirementAction.SUBMIT_TEST, user(6L, "QA")));
        assertEquals(ErrorCode.ILLEGAL_TRANSITION.getCode(), e.getCode());
    }

    @Test
    void submitTestSuccessWhenAllDevTasksDone() {
        Requirement r = requirement(RequirementStatus.DEVELOPING.name(), 2L);
        when(taskMapper.selectCount(any())).thenReturn(0L);
        flowService.apply(r, RequirementAction.SUBMIT_TEST, user(6L, "QA"));
        assertEquals(RequirementStatus.TESTING.name(), r.getStatus());
    }

    @Test
    void availableActionsFilteredByRoleAndStatus() {
        Requirement r = requirement(RequirementStatus.REVIEW_PASSED.name(), 2L);
        List<String> pmActions = flowService.availableActions(r, user(4L, "PM"));
        assertTrue(pmActions.contains(RequirementAction.SCHEDULE.name()));
        List<String> devActions = flowService.availableActions(r, user(5L, "DEV"));
        assertFalse(devActions.contains(RequirementAction.SCHEDULE.name()));
    }

    @Test
    void documentDrivenActionsCannotBeCalledDirectly() {
        assertTrue(RequirementFlowService.isDocumentDriven(RequirementAction.REVIEW_PASS));
        assertTrue(RequirementFlowService.isDocumentDriven(RequirementAction.ACCEPT_PASS));
        assertFalse(RequirementFlowService.isDocumentDriven(RequirementAction.SCHEDULE));
    }
}
