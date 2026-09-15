package com.example.scriptkill.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 演员停演联动开演的真实 HTTP(Controller -> Service -> JPA/H2) 回归测试。
 * 走通整条链路：档案标停演（带原因）-> 未开演场次名单标待换 -> 开演被拦并写出
 * 演员名/所演人物/原因原文 -> 换掉待换的人 -> 开演成功转开演中 -> 刷新后名单无待换。
 */
@SpringBootTest
@AutoConfigureMockMvc
class PerformerSuspendStartIntegrationTest {

    @TestConfiguration
    static class FakeRedisConfig {
        @Bean
        @Primary
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> redisTemplate() {
            RedisTemplate<String, Object> template = mock(RedisTemplate.class);
            ValueOperations<String, Object> ops = mock(ValueOperations.class);
            when(template.opsForValue()).thenReturn(ops);
            return template;
        }

        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            return mock(RedisConnectionFactory.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode data(String content) throws Exception {
        return objectMapper.readTree(content).path("data");
    }

    private long createTheme(String name) throws Exception {
        String body = "{\"themeName\":\"" + name + "\",\"era\":\"民国\",\"difficulty\":\"中等\",\"description\":\"d\"}";
        String created = mockMvc.perform(post("/api/theme").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return data(created).path("id").asLong();
    }

    private long createRole(String roleName, long themeId) throws Exception {
        String body = "{\"roleName\":\"" + roleName + "\",\"scriptThemeId\":" + themeId
                + ",\"gender\":\"男\",\"age\":28,\"description\":\"x\"}";
        String created = mockMvc.perform(post("/api/role").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return data(created).path("id").asLong();
    }

    private long createPerformer(String name) throws Exception {
        String created = mockMvc.perform(post("/api/performer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"performerName\":\"" + name + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return data(created).path("id").asLong();
    }

    private long createSession(long themeId) throws Exception {
        String body = "{\"scriptThemeId\":" + themeId
                + ",\"startTime\":\"2026-10-01T14:00:00\",\"endTime\":\"2026-10-01T16:00:00\"}";
        String created = mockMvc.perform(post("/api/session").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return data(created).path("id").asLong();
    }

    private void assign(long sessionId, long roleId, long performerId) throws Exception {
        String body = "{\"characterRoleId\":" + roleId + ",\"performerId\":" + performerId + "}";
        mockMvc.perform(post("/api/session/" + sessionId + "/assign")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private JsonNode getSession(long sessionId) throws Exception {
        String resp = mockMvc.perform(get("/api/session/" + sessionId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return data(resp);
    }

    private JsonNode assignmentOf(JsonNode session, long roleId) {
        for (JsonNode a : session.path("assignments")) {
            if (a.path("characterRoleId").asLong() == roleId) {
                return a;
            }
        }
        return null;
    }

    @Test
    void 标停演后未开演场次标待换_开演被拦写出原文_换人后开演成功转开演中() throws Exception {
        long themeId = createTheme("停演联动主题");
        long roleA = createRole("停演角色甲", themeId);
        long roleB = createRole("停演角色乙", themeId);
        long performerA = createPerformer("停演演员甲");
        long performerB = createPerformer("停演演员乙");
        long performerC = createPerformer("救场演员丙");
        long sessionId = createSession(themeId);

        // 两个人物排齐，标为已排好
        assign(sessionId, roleA, performerA);
        assign(sessionId, roleB, performerB);
        mockMvc.perform(post("/api/session/" + sessionId + "/ready")).andExpect(status().isOk());
        assertEquals("已排好", getSession(sessionId).path("status").asText());

        // 演员临时不能上场：档案标停演必须写原因，空原因被拦
        mockMvc.perform(post("/api/performer/" + performerA + "/suspend")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/performer/" + performerA + "/suspend")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"突发高烧，临时无法上场\"}"))
                .andExpect(status().isOk());

        // 未开演场次的名单里这个人标成待换，并带停演原因原文
        JsonNode suspended = getSession(sessionId);
        assertEquals(1, suspended.path("pendingReplacementCount").asInt());
        JsonNode pending = assignmentOf(suspended, roleA);
        assertTrue(pending.path("pendingReplacement").asBoolean(), "未开演场次名单里此人要标成待换");
        assertEquals("停演演员甲", pending.path("performerName").asText());
        assertEquals("停演角色甲", pending.path("roleName").asText());
        assertEquals("突发高烧，临时无法上场", pending.path("suspendReason").asText());
        assertFalse(assignmentOf(suspended, roleB).path("pendingReplacement").asBoolean());

        // 点开演：只要还有待换就必须拦住，写出演员名、所演人物和原因原文
        String blocked = mockMvc.perform(post("/api/session/" + sessionId + "/start"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        String blockedMsg = objectMapper.readTree(blocked).path("message").asText();
        assertTrue(blockedMsg.contains("停演演员甲"), "拦截提示要写出停演演员名：" + blockedMsg);
        assertTrue(blockedMsg.contains("停演角色甲"), "拦截提示要写出所演人物：" + blockedMsg);
        assertTrue(blockedMsg.contains("突发高烧，临时无法上场"), "拦截提示要写出原因原文：" + blockedMsg);
        // 拦下后仍是已排好，没有顺着「已排好就能演」放过去
        assertEquals("已排好", getSession(sessionId).path("status").asText());

        // 停演人员也不能再排进别的场次
        String assignSuspended = mockMvc.perform(post("/api/session/" + sessionId + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterRoleId\":" + roleB + ",\"performerId\":" + performerA + "}"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertTrue(objectMapper.readTree(assignSuspended).path("message").asText().contains("突发高烧，临时无法上场"));

        // 要开演就得先换掉待换的人：甲换成丙后待换消失
        assign(sessionId, roleA, performerC);
        JsonNode replaced = getSession(sessionId);
        assertEquals(0, replaced.path("pendingReplacementCount").asInt());
        assertEquals("救场演员丙", assignmentOf(replaced, roleA).path("performerName").asText());

        // 开演成功：已排好转开演中
        String started = mockMvc.perform(post("/api/session/" + sessionId + "/start"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals("开演中", data(started).path("status").asText());

        // 刷新（重新 GET）后这场名单里不得再出现待换
        JsonNode running = getSession(sessionId);
        assertEquals("开演中", running.path("status").asText());
        assertEquals(0, running.path("pendingReplacementCount").asInt());
        for (JsonNode a : running.path("assignments")) {
            assertFalse(a.path("pendingReplacement").asBoolean(), "开演中名单里不得再出现待换");
        }

        // 即使开演后又有人标停演，开演中的名单也不再生出待换
        mockMvc.perform(post("/api/performer/" + performerB + "/suspend")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"临时有事\"}"))
                .andExpect(status().isOk());
        JsonNode runningAfter = getSession(sessionId);
        assertEquals(0, runningAfter.path("pendingReplacementCount").asInt());
        for (JsonNode a : runningAfter.path("assignments")) {
            assertFalse(a.path("pendingReplacement").asBoolean(), "开演中名单里不得再出现待换");
        }
    }

    @Test
    void 恢复演出后待换消失_可以直接开演() throws Exception {
        long themeId = createTheme("恢复联动主题");
        long roleA = createRole("恢复角色甲", themeId);
        long performerA = createPerformer("恢复演员甲");
        long sessionId = createSession(themeId);

        assign(sessionId, roleA, performerA);
        mockMvc.perform(post("/api/session/" + sessionId + "/ready")).andExpect(status().isOk());
        mockMvc.perform(post("/api/performer/" + performerA + "/suspend")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"嗓子哑了\"}"))
                .andExpect(status().isOk());
        assertEquals(1, getSession(sessionId).path("pendingReplacementCount").asInt());

        // 恢复演出后待换消失，开演放行
        mockMvc.perform(post("/api/performer/" + performerA + "/resume")).andExpect(status().isOk());
        JsonNode resumed = getSession(sessionId);
        assertEquals(0, resumed.path("pendingReplacementCount").asInt());
        assertFalse(assignmentOf(resumed, roleA).path("pendingReplacement").asBoolean());

        String started = mockMvc.perform(post("/api/session/" + sessionId + "/start"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals("开演中", data(started).path("status").asText());
    }
}
