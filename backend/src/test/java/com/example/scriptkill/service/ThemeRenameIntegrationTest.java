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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 剧本改名后人物列表「所属剧本」回归测试。
 * 走真实 HTTP(Controller -> Service -> JPA) 链路：
 * 建主题、建角色、改名后刷新，GET /role 的 themeName 必须是新名。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ThemeRenameIntegrationTest {

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

    private void createRole(String roleName, long themeId) throws Exception {
        String body = "{\"roleName\":\"" + roleName + "\",\"scriptThemeId\":" + themeId
                + ",\"gender\":\"男\",\"age\":28,\"description\":\"x\"}";
        mockMvc.perform(post("/api/role").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private String getRoleThemeName(String roleName) throws Exception {
        String roles = mockMvc.perform(get("/api/role")).andReturn().getResponse().getContentAsString();
        for (JsonNode r : data(roles)) {
            if (r.path("roleName").asText().equals(roleName)) {
                return r.path("themeName").asText();
            }
        }
        return null;
    }

    @Test
    void 改名刷新后人物列表所属剧本显示新名() throws Exception {
        long themeId = createTheme("民国风云");
        createRole("许文强", themeId);

        // 改名后刷新：主题列表与人物列表都应读到新名
        String renameBody = "{\"themeName\":\"民国风云录\",\"era\":\"民国\",\"difficulty\":\"中等\",\"description\":\"d\"}";
        mockMvc.perform(put("/api/theme/" + themeId).contentType(MediaType.APPLICATION_JSON).content(renameBody))
                .andExpect(status().isOk());

        String themes = mockMvc.perform(get("/api/theme")).andReturn().getResponse().getContentAsString();
        String themeListName = null;
        for (JsonNode t : data(themes)) {
            if (t.path("id").asLong() == themeId) {
                themeListName = t.path("themeName").asText();
            }
        }

        assertEquals("民国风云录", themeListName);
        assertEquals("民国风云录", getRoleThemeName("许文强"));
    }
}
