package com.example.scriptkill.service.impl;

import com.example.scriptkill.dto.response.ScanParseResponse;
import com.example.scriptkill.entity.Prop;
import com.example.scriptkill.exception.BusinessException;
import com.example.scriptkill.repository.PropRepository;
import com.example.scriptkill.service.PropService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PropServiceImpl implements PropService {

    private static final String ERA_STYLE_TEMPLATE_KEY = "prop:era_style_template";

    /** 道具编号最大长度，与 prop 表 prop_code 列一致 */
    private static final int PROP_CODE_MAX_LENGTH = 50;

    /** 条码AIM符号体系标识符，如 ]C1、]Q3、]E0 */
    private static final Pattern AIM_PREFIX = Pattern.compile("^\\][A-Za-z][0-9A-Za-z]");

    /** 键值对形式的码值前缀，如 PROP:XXXX、code=XXXX */
    private static final Pattern CODE_KEY_PREFIX = Pattern.compile("^(?i)(?:prop|propcode|code)[:=](.+)$");

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
    public ScanParseResponse parseScannedCode(String rawCode) {
        if (rawCode == null || rawCode.trim().isEmpty()) {
            throw new BusinessException("未获取到条码内容，请重新扫描或手动录入编号");
        }
        String propCode = normalizeScannedCode(rawCode);
        if (propCode.isEmpty()) {
            throw new BusinessException("条码内容无法解析为道具编号，请手动录入");
        }
        if (propCode.length() > PROP_CODE_MAX_LENGTH) {
            throw new BusinessException("解析出的道具编号超过" + PROP_CODE_MAX_LENGTH
                    + "个字符，请确认扫描的是道具编号条码");
        }

        ScanParseResponse response = new ScanParseResponse();
        response.setRawCode(rawCode);
        response.setPropCode(propCode);
        Prop existing = propRepository.findByPropCode(propCode).orElse(null);
        response.setExists(existing != null);
        response.setProp(existing);
        return response;
    }

    /**
     * 归一化扫码原始内容：去控制字符、AIM标识符、URL包装和键值前缀，
     * 得到可直接建档使用的道具编号。
     */
    private String normalizeScannedCode(String rawCode) {
        String code = rawCode.trim().replaceAll("[\\p{Cntrl}]", "").trim();
        if (code.isEmpty()) {
            return code;
        }

        // 部分扫码枪会输出AIM符号体系标识符（如 ]C1 表示Code128），需剥离
        if (AIM_PREFIX.matcher(code).find()) {
            code = code.substring(3);
        }

        // 二维码内容为URL时，优先取 code/propCode 参数，否则取路径最后一段
        if (code.startsWith("http://") || code.startsWith("https://")) {
            code = extractCodeFromUrl(code);
        }

        // 形如 PROP:XXXX、code=XXXX 的键值形式，取键对应的值
        Matcher keyValue = CODE_KEY_PREFIX.matcher(code);
        if (keyValue.matches()) {
            code = keyValue.group(1).trim();
        }
        return code;
    }

    private String extractCodeFromUrl(String url) {
        String noFragment = url.split("#", 2)[0];
        String path = noFragment;
        int queryIndex = noFragment.indexOf('?');
        if (queryIndex >= 0) {
            path = noFragment.substring(0, queryIndex);
            String query = noFragment.substring(queryIndex + 1);
            for (String pair : query.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && (kv[0].equalsIgnoreCase("code")
                        || kv[0].equalsIgnoreCase("propCode")
                        || kv[0].equalsIgnoreCase("prop"))) {
                    return URLDecoder.decode(kv[1], StandardCharsets.UTF_8).trim();
                }
            }
        }
        String[] segments = path.split("/");
        for (int i = segments.length - 1; i >= 0; i--) {
            if (!segments[i].isEmpty() && !segments[i].contains(":")) {
                return URLDecoder.decode(segments[i], StandardCharsets.UTF_8).trim();
            }
        }
        return "";
    }

    @Override
    public Prop createProp(Prop prop) {
        if (prop.getPropCode() == null || prop.getPropCode().trim().isEmpty()) {
            throw new BusinessException("道具编号不能为空");
        }
        prop.setPropCode(prop.getPropCode().trim());
        if (propRepository.findByPropCode(prop.getPropCode()).isPresent()) {
            throw new BusinessException("道具编号「" + prop.getPropCode() + "」已建档，请勿重复录入");
        }
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