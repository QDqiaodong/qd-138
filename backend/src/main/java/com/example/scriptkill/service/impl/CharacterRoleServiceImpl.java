package com.example.scriptkill.service.impl;

import com.example.scriptkill.dto.response.CharacterRoleResponse;
import com.example.scriptkill.entity.CharacterRole;
import com.example.scriptkill.entity.ScriptTheme;
import com.example.scriptkill.repository.CharacterRoleRepository;
import com.example.scriptkill.repository.ScriptThemeRepository;
import com.example.scriptkill.service.CharacterRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CharacterRoleServiceImpl implements CharacterRoleService {

    @Autowired
    private CharacterRoleRepository characterRoleRepository;

    @Autowired
    private ScriptThemeRepository scriptThemeRepository;

    @Override
    public List<CharacterRoleResponse> listAll() {
        return toResponses(characterRoleRepository.findAll());
    }

    @Override
    public List<CharacterRoleResponse> listByTheme(Long themeId) {
        return toResponses(characterRoleRepository.findByScriptThemeId(themeId));
    }

    // 所属剧本名一律在读取时按 ID 实时取当前主题，不存冗余快照，
    // 这样剧本改名后人物列表刷新即为新名
    private List<CharacterRoleResponse> toResponses(List<CharacterRole> roles) {
        List<CharacterRoleResponse> responses = new ArrayList<>();
        for (CharacterRole role : roles) {
            ScriptTheme theme = role.getScriptThemeId() != null
                    ? scriptThemeRepository.findById(role.getScriptThemeId()).orElse(null)
                    : null;
            responses.add(CharacterRoleResponse.fromEntity(role, theme));
        }
        return responses;
    }
}
