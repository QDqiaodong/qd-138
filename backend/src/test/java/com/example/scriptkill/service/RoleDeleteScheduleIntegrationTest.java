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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 人物删除联动排班的真实 HTTP(Controller -> Service -> JPA/H2) 回归测试。
 * 验证两条路：
 * 1. 默认删除遇排班必拦，且响应里写出卡在哪几场，人物仍在；
 * 2. 撤下并删除后，重新打开排班页（重新 GET）进度回退、已排好打回排班中，人物消失。
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoleDeleteScheduleIntegrationTest {

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

    private boolean roleExists(long roleId) throws Exception {
        String resp = mockMvc.perform(get("/api/role")).andReturn().getResponse().getContentAsString();
        for (JsonNode r : data(resp)) {
            if (r.path("id").asLong() == roleId) {
                return true;
            }
        }
        return false;
    }

    @Test
    void 默认删除遇排班拦住并写出场次_撤下并删除后排班进度与已排好正确回退() throws Exception {
        long themeId = createTheme("删除联动主题");
        long roleA = createRole("联动角色甲", themeId);
        long roleB = createRole("联动角色乙", themeId);
        long performerA = createPerformer("联动演员甲");
        long performerB = createPerformer("联动演员乙");
        long sessionId = createSession(themeId);

        // 两个人物排齐，标为已排好
        assign(sessionId, roleA, performerA);
        assign(sessionId, roleB, performerB);
        mockMvc.perform(post("/api/session/" + sessionId + "/ready")).andExpect(status().isOk());
        JsonNode ready = getSession(sessionId);
        assertEquals(2, ready.path("totalRoles").asInt());
        assertEquals(2, ready.path("assignedCount").asInt());
        assertEquals("已排好", ready.path("status").asText());

        // 默认删除已排好的人物：必须 400 拦住，并写出卡在哪一场
        String blocked = mockMvc.perform(delete("/api/role/" + roleA))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        String blockedMsg = objectMapper.readTree(blocked).path("message").asText();
        assertTrue(blockedMsg.contains("联动角色甲"), "拦截提示要写出人物：" + blockedMsg);
        assertTrue(blockedMsg.contains("CC"), "拦截提示要写出卡点场次编号：" + blockedMsg);
        // 关掉人物页/排班页再打开：人物还在，场次仍是已排好、进度 2/2
        assertTrue(roleExists(roleA), "被拦的人物必须仍在");
        JsonNode unchanged = getSession(sessionId);
        assertEquals("已排好", unchanged.path("status").asText());
        assertEquals(2, unchanged.path("assignedCount").asInt());

        // 走另一条路：撤下并删除
        mockMvc.perform(post("/api/role/" + roleA + "/unassign-and-delete"))
                .andExpect(status().isOk());

        // 重新打开（重新 GET，相当于关掉页面再进）：人物消失、进度 1/2、状态打回排班中
        assertFalse(roleExists(roleA), "撤下并删除后人物应消失");
        JsonNode after = getSession(sessionId);
        assertEquals(1, after.path("assignedCount").asInt(), "进度要随撤下回退");
        assertEquals(1, after.path("totalRoles").asInt(), "人物被删掉后总人数随之减为 1");
        assertEquals("排班中", after.path("status").asText(), "已排好必须打回排班中");
        // 排班视图里也不能再挂着被删人物
        for (JsonNode a : after.path("assignments")) {
            assertFalse(a.path("characterRoleId").asLong() == roleA, "被删人物的排班记录必须清掉");
        }
    }
}
