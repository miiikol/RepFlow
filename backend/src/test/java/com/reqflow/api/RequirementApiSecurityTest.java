package com.reqflow.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接口级回归：认证、接口权限、数据权限、状态机与只读约束。
 * 每个用例在测试事务中执行，结束后回滚，不污染演示数据。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RequirementApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String login(String username) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"Reqflow@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).path("data").path("token").asText();
    }

    private String bearer(String username) throws Exception {
        return "Bearer " + login(username);
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/requirements"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10001));
    }

    @Test
    void businessUserOnlySeesOwnRequirements() throws Exception {
        mockMvc.perform(get("/api/requirements").header("Authorization", bearer("biz")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(8))
                .andExpect(jsonPath("$.data.records[*].proposerId", everyItem(is(2))));
    }

    @Test
    void detailOfOthersRequirementReturnsDataForbidden() throws Exception {
        mockMvc.perform(get("/api/requirements/5").header("Authorization", bearer("biz")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(20002));
    }

    @Test
    void analystCanViewRequirementInCharge() throws Exception {
        mockMvc.perform(get("/api/requirements/8").header("Authorization", bearer("ra")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void reviewListIsFilteredByDataScope() throws Exception {
        mockMvc.perform(get("/api/reviews").header("Authorization", bearer("dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/reviews").header("Authorization", bearer("pm")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3));
    }

    @Test
    void illegalTransitionIsRejectedAndStatusKept() throws Exception {
        mockMvc.perform(post("/api/requirements/9/action")
                        .header("Authorization", bearer("biz"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"SUBMIT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40002));

        mockMvc.perform(get("/api/requirements/9").header("Authorization", bearer("biz")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requirement.status").value("REJECTED"));
    }

    @Test
    void archivedRequirementIsReadonly() throws Exception {
        String body = """
                {"title":"贷款进度短信提醒","type":"NEW_FEATURE","source":"BUSINESS_DEPT","deptId":2,
                 "priority":"P2","expectDate":"2026-10-20","background":"背景","description":"描述",
                 "acceptanceCriteria":"标准"}
                """;
        mockMvc.perform(put("/api/requirements/6")
                        .header("Authorization", bearer("biz"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40005));
    }

    @Test
    void userWithoutPermissionIsForbidden() throws Exception {
        String body = """
                {"title":"越权新建","type":"NEW_FEATURE","source":"BUSINESS_DEPT","deptId":2,
                 "priority":"P2","expectDate":"2026-12-20","background":"背景","description":"描述",
                 "acceptanceCriteria":"标准"}
                """;
        mockMvc.perform(post("/api/requirements")
                        .header("Authorization", bearer("dev"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(20001));
    }

    @Test
    void accountIsLockedAfterFiveFailedAttempts() throws Exception {
        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"qa\",\"password\":\"wrong-password\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(10003));
        }
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"qa\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10002));
    }
}
