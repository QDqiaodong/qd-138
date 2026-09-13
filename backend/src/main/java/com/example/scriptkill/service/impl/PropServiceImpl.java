package com.example.scriptkill.service.impl;

import com.example.scriptkill.entity.Prop;
import com.example.scriptkill.repository.PropRepository;
import com.example.scriptkill.service.PropService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PropServiceImpl implements PropService {

    private static final String ERA_STYLE_TEMPLATE_KEY = "prop:era_style_template";

    @Autowired
    private PropRepository propRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        cacheEraStyleTemplate();
    }

    @Override
    public List<Prop> getAllProps() {
        return propRepository.findAll();
    }

    @Override
    public Prop getPropById(Long id) {
        return propRepository.findById(id).orElse(null);
    }

    @Override
    public Prop getPropByCode(String code) {
        return propRepository.findByPropCode(code).orElse(null);
    }

    @Override
    public Prop createProp(Prop prop) {
        return propRepository.save(prop);
    }

    @Override
    public Prop updateProp(Long id, Prop prop) {
        Optional<Prop> existing = propRepository.findById(id);
        if (existing.isPresent()) {
            Prop p = existing.get();
            p.setPropName(prop.getPropName());
            p.setEra(prop.getEra());
            p.setPropType(prop.getPropType());
            p.setDescription(prop.getDescription());
            p.setStatus(prop.getStatus());
            return propRepository.save(p);
        }
        return null;
    }

    @Override
    public void deleteProp(Long id) {
        propRepository.deleteById(id);
    }

    @Override
    public List<Prop> getPropsByEra(String era) {
        return propRepository.findByEra(era);
    }

    @Override
    public List<Prop> getPropsByType(String type) {
        return propRepository.findByPropType(type);
    }

    @Override
    public Map<String, List<String>> getEraStyleTemplate() {
        Object cached = redisTemplate.opsForValue().get(ERA_STYLE_TEMPLATE_KEY);
        if (cached != null) {
            try {
                String json = objectMapper.writeValueAsString(cached);
                return objectMapper.readValue(json, new TypeReference<Map<String, List<String>>>() {});
            } catch (JsonProcessingException e) {
                return buildEraStyleTemplate();
            }
        }
        Map<String, List<String>> template = buildEraStyleTemplate();
        cacheEraStyleTemplate();
        return template;
    }

    @Override
    public void cacheEraStyleTemplate() {
        Map<String, List<String>> template = buildEraStyleTemplate();
        redisTemplate.opsForValue().set(ERA_STYLE_TEMPLATE_KEY, template);
    }

    private Map<String, List<String>> buildEraStyleTemplate() {
        Map<String, List<String>> template = new HashMap<>();
        template.put("民国", List.of("服装", "配饰", "道具", "武器"));
        template.put("古代", List.of("服装", "配饰", "道具", "武器", "乐器"));
        template.put("未来", List.of("服装", "配饰", "道具", "电子设备"));
        template.put("现代", List.of("服装", "配饰", "道具", "电子产品"));
        template.put("古风", List.of("服装", "配饰", "道具", "武器", "乐器"));
        template.put("仙侠", List.of("服装", "配饰", "道具", "法器", "丹药"));
        template.put("悬疑", List.of("服装", "道具", "线索物品"));
        return template;
    }
}