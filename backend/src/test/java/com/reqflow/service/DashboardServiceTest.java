package com.reqflow.service;

import com.reqflow.support.SecurityTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 看板统计回归：确认分组聚合在 H2（MySQL 模式）与 MySQL 下都可执行。
 */
@SpringBootTest
@ActiveProfiles("test")
class DashboardServiceTest {

    @Autowired
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        SecurityTestSupport.authenticate(SecurityTestSupport.user(1L, "admin", 1L, "ALL", "ADMIN"));
    }

    @AfterEach
    void tearDown() {
        SecurityTestSupport.clear();
    }

    @Test
    void summaryCountsAllSeedRequirements() {
        Map<String, Object> summary = dashboardService.summary();
        assertEquals(10L, ((Number) summary.get("total")).longValue());
        assertTrue(((Number) summary.get("inFlight")).longValue() > 0);
    }

    @Test
    void statusAndDeptDistributionAreAggregatedInDatabase() {
        List<Map<String, Object>> status = dashboardService.statusDistribution();
        long total = status.stream().mapToLong(item -> ((Number) item.get("value")).longValue()).sum();
        assertEquals(10L, total);

        List<Map<String, Object>> depts = dashboardService.deptDistribution();
        assertEquals(3, depts.size(), "演示数据覆盖 3 个业务部门");
    }

    @Test
    void trendReturnsSixMonths() {
        List<Map<String, Object>> trend = dashboardService.trend();
        assertEquals(6, trend.size());
        assertFalse(trend.isEmpty());
    }
}
