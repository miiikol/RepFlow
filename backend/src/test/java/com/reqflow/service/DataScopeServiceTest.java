package com.reqflow.service;

import com.reqflow.security.LoginUser;
import com.reqflow.service.support.DataScopeService;
import com.reqflow.support.SecurityTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据权限回归：列表过滤与详情校验必须使用同一套谓词。
 * 断言基于 V2__seed.sql 的固定演示数据。
 */
@SpringBootTest
@ActiveProfiles("test")
class DataScopeServiceTest {

    @Autowired
    private DataScopeService dataScopeService;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ChangeService changeService;

    @AfterEach
    void tearDown() {
        SecurityTestSupport.clear();
    }

    @Test
    void selfScopeSeesOnlyOwnRequirements() {
        LoginUser biz = SecurityTestSupport.user(2L, "biz", 2L, "SELF", "BUSINESS");
        assertTrue(dataScopeService.canView(1L, biz));
        assertTrue(dataScopeService.canView(9L, biz));
        assertFalse(dataScopeService.canView(5L, biz));
    }

    @Test
    void deptScopeCoversDeptAndAnalystButNotOthersDraft() {
        LoginUser ra = SecurityTestSupport.user(3L, "ra", 1L, "DEPT", "RA");
        assertTrue(dataScopeService.canView(1L, ra), "分析人可见");
        assertTrue(dataScopeService.canView(5L, ra), "跨部门但本人分析");
        assertFalse(dataScopeService.canView(7L, ra), "他人草稿不可见");
    }

    @Test
    void projectScopeCoversTaskAssignee() {
        LoginUser dev = SecurityTestSupport.user(5L, "dev", 1L, "PROJECT", "DEV");
        assertTrue(dataScopeService.canView(3L, dev), "任务负责人可见");
        assertFalse(dataScopeService.canView(1L, dev));
        assertFalse(dataScopeService.canView(7L, dev));
    }

    @Test
    void adminSeesEverything() {
        LoginUser admin = SecurityTestSupport.user(1L, "admin", 1L, "ALL", "ADMIN");
        assertTrue(dataScopeService.canView(7L, admin));
    }

    @Test
    void reviewListIsScopedByRequirementVisibility() {
        SecurityTestSupport.authenticate(SecurityTestSupport.user(5L, "dev", 1L, "PROJECT", "DEV"));
        assertEquals(0, reviewService.list(1, 20, null, null, false).getTotal());

        SecurityTestSupport.authenticate(SecurityTestSupport.user(4L, "pm", 1L, "PROJECT", "PM"));
        assertEquals(3, reviewService.list(1, 20, null, null, false).getTotal());
    }

    @Test
    void changeListIsScopedByRequirementVisibility() {
        SecurityTestSupport.authenticate(SecurityTestSupport.user(7L, "accept", 4L, "PROJECT", "ACCEPT"));
        assertEquals(0, changeService.list(1, 20, null, null).getTotal());

        SecurityTestSupport.authenticate(SecurityTestSupport.user(2L, "biz", 2L, "SELF", "BUSINESS"));
        assertEquals(2, changeService.list(1, 20, null, null).getTotal());
    }
}
