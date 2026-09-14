package com.example.scriptkill.service.impl;

import com.example.scriptkill.dto.request.PropBindRequest;
import com.example.scriptkill.dto.request.PropChangeRequest;
import com.example.scriptkill.dto.response.RolePropsResponse;
import com.example.scriptkill.dto.response.RolePropResponse;
import com.example.scriptkill.dto.response.ScriptThemeDetailResponse;
import com.example.scriptkill.entity.*;
import com.example.scriptkill.exception.BusinessException;
import com.example.scriptkill.repository.*;
import com.example.scriptkill.service.RolePropService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RolePropServiceImpl implements RolePropService {

    @Autowired
    private RolePropRepository rolePropRepository;

    @Autowired
    private CharacterRoleRepository characterRoleRepository;

    @Autowired
    private PropRepository propRepository;

    @Autowired
    private ScriptThemeRepository scriptThemeRepository;

    @Autowired
    private PropChangeRecordRepository changeRecordRepository;

    @Override
    public List<RolePropResponse> getAllRoleProps() {
        List<RoleProp> roleProps = rolePropRepository.findAll();
        return convertToResponseList(roleProps);
    }

    @Override
    public List<RolePropResponse> getRolePropsByRoleId(Long roleId) {
        List<RoleProp> roleProps = rolePropRepository.findByCharacterRoleId(roleId);
        return convertToResponseList(roleProps);
    }

    @Override
    public List<RolePropResponse> getRolePropsByPropId(Long propId) {
        List<RoleProp> roleProps = rolePropRepository.findByPropId(propId);
        return convertToResponseList(roleProps);
    }

    @Override
    public List<RolePropResponse> getRolePropsByThemeId(Long themeId) {
        List<RoleProp> roleProps = rolePropRepository.findByScriptThemeId(themeId);
        return convertToResponseList(roleProps);
    }

    @Override
    public RolePropsResponse getRolePropsByRoleName(String roleName) {
        List<CharacterRole> roles = characterRoleRepository.findByRoleNameContaining(roleName);
        if (roles.isEmpty()) {
            return null;
        }
        CharacterRole role = roles.get(0);
        ScriptTheme theme = scriptThemeRepository.findById(role.getScriptThemeId()).orElse(null);
        
        RolePropsResponse response = new RolePropsResponse();
        response.setRoleId(role.getId());
        response.setRoleName(role.getRoleName());
        response.setScriptThemeId(role.getScriptThemeId());
        response.setThemeName(theme != null ? theme.getThemeName() : null);
        
        List<RoleProp> roleProps = rolePropRepository.findByCharacterRoleId(role.getId());
        List<RolePropsResponse.PropDetail> propDetails = new ArrayList<>();
        for (RoleProp rp : roleProps) {
            Prop prop = propRepository.findById(rp.getPropId()).orElse(null);
            if (prop != null) {
                RolePropsResponse.PropDetail detail = new RolePropsResponse.PropDetail();
                detail.setId(prop.getId());
                detail.setPropCode(prop.getPropCode());
                detail.setPropName(prop.getPropName());
                detail.setPropType(prop.getPropType());
                detail.setEra(prop.getEra());
                detail.setStatus(prop.getStatus());
                detail.setDescription(prop.getDescription());
                propDetails.add(detail);
            }
        }
        response.setProps(propDetails);
        return response;
    }

    @Override
    public ScriptThemeDetailResponse getThemeDetail(Long themeId) {
        Optional<ScriptTheme> themeOpt = scriptThemeRepository.findById(themeId);
        if (themeOpt.isEmpty()) {
            return null;
        }
        ScriptTheme theme = themeOpt.get();
        
        ScriptThemeDetailResponse response = new ScriptThemeDetailResponse();
        response.setId(theme.getId());
        response.setThemeName(theme.getThemeName());
        response.setDescription(theme.getDescription());
        response.setEra(theme.getEra());
        response.setDifficulty(theme.getDifficulty());
        
        List<CharacterRole> roles = characterRoleRepository.findByScriptThemeId(themeId);
        List<ScriptThemeDetailResponse.RoleDetail> roleDetails = new ArrayList<>();
        
        for (CharacterRole role : roles) {
            ScriptThemeDetailResponse.RoleDetail roleDetail = new ScriptThemeDetailResponse.RoleDetail();
            roleDetail.setId(role.getId());
            roleDetail.setRoleName(role.getRoleName());
            roleDetail.setGender(role.getGender());
            roleDetail.setAge(role.getAge());
            roleDetail.setDescription(role.getDescription());
            
            List<RoleProp> roleProps = rolePropRepository.findByCharacterRoleId(role.getId());
            List<ScriptThemeDetailResponse.PropDetail> propDetails = new ArrayList<>();
            
            for (RoleProp rp : roleProps) {
                Prop prop = propRepository.findById(rp.getPropId()).orElse(null);
                if (prop != null) {
                    ScriptThemeDetailResponse.PropDetail propDetail = new ScriptThemeDetailResponse.PropDetail();
                    propDetail.setId(prop.getId());
                    propDetail.setPropCode(prop.getPropCode());
                    propDetail.setPropName(prop.getPropName());
                    propDetail.setPropType(prop.getPropType());
                    propDetail.setEra(prop.getEra());
                    propDetail.setStatus(prop.getStatus());
                    propDetails.add(propDetail);
                }
            }
            roleDetail.setProps(propDetails);
            roleDetails.add(roleDetail);
        }
        response.setRoles(roleDetails);
        return response;
    }

    @Override
    @Transactional
    public void bindPropsToRole(PropBindRequest request) {
        if (request.getCharacterRoleId() == null) {
            throw new BusinessException("请选择要绑定的人物角色");
        }
        if (request.getPropIds() == null || request.getPropIds().isEmpty()) {
            throw new BusinessException("请选择要绑定的道具");
        }

        CharacterRole role = characterRoleRepository.findById(request.getCharacterRoleId())
                .orElseThrow(() -> new BusinessException("所选人物角色不存在，请刷新后重试"));
        // 人物所属剧本的时代是核对基准
        ScriptTheme theme = scriptThemeRepository.findById(role.getScriptThemeId())
                .orElseThrow(() -> new BusinessException("人物所属剧本不存在，请刷新后重试"));
        if (isBlankEra(theme.getEra())) {
            throw new BusinessException("剧本「" + theme.getThemeName() + "」未设定时代，无法核对道具时代");
        }

        // 绑定前先整批核对：剧本时代与道具时代不一致即视为时代不符，全部拦截不落库
        List<String> mismatchNames = new ArrayList<>();
        for (Long propId : request.getPropIds()) {
            Prop prop = propRepository.findById(propId).orElse(null);
            if (prop == null) {
                throw new BusinessException("存在已失效的道具，请刷新道具列表后重选");
            }
            if (!eraMatches(theme.getEra(), prop.getEra())) {
                mismatchNames.add(prop.getPropCode() + " " + prop.getPropName()
                        + "（道具时代：" + propEraText(prop.getEra()) + "）");
            }
        }
        if (!mismatchNames.isEmpty()) {
            throw new BusinessException("跨时代绑定被拦截：剧本「" + theme.getThemeName() + "」的时代为「"
                    + theme.getEra() + "」，以下道具与剧本时代不符，请改选同代道具：\n"
                    + String.join("；", mismatchNames));
        }

        for (Long propId : request.getPropIds()) {
            Prop prop = propRepository.findById(propId).orElse(null);

            PropChangeRecord record = new PropChangeRecord();
            record.setPropId(propId);
            record.setCharacterRoleId(role.getId());
            record.setScriptThemeId(role.getScriptThemeId());
            record.setChangeType("绑定");
            record.setChangeReason(request.getReason());
            record.setOperator(request.getOperator());
            record.setAfterValue("角色: " + role.getRoleName());
            changeRecordRepository.save(record);

            RoleProp roleProp = new RoleProp();
            roleProp.setCharacterRoleId(role.getId());
            roleProp.setPropId(propId);
            roleProp.setScriptThemeId(role.getScriptThemeId());
            rolePropRepository.save(roleProp);
        }
    }

    @Override
    @Transactional
    public void unbindPropsFromRole(Long roleId, List<Long> propIds) {
        CharacterRole role = characterRoleRepository.findById(roleId).orElse(null);
        
        for (Long propId : propIds) {
            Prop prop = propRepository.findById(propId).orElse(null);
            
            PropChangeRecord record = new PropChangeRecord();
            record.setPropId(propId);
            record.setCharacterRoleId(roleId);
            record.setChangeType("解绑");
            record.setBeforeValue("角色: " + (role != null ? role.getRoleName() : "未知"));
            changeRecordRepository.save(record);
            
            List<RoleProp> roleProps = rolePropRepository.findByCharacterRoleIdAndScriptThemeId(
                roleId, role != null ? role.getScriptThemeId() : null);
            for (RoleProp rp : roleProps) {
                if (rp.getPropId().equals(propId)) {
                    rolePropRepository.delete(rp);
                    break;
                }
            }
        }
    }

    @Override
    @Transactional
    public void changePropRole(PropChangeRequest request) {
        if (request.getPropId() == null || request.getFromRoleId() == null || request.getToRoleId() == null) {
            throw new BusinessException("请完整填写剧本、原角色、新角色和道具");
        }

        Prop prop = propRepository.findById(request.getPropId())
                .orElseThrow(() -> new BusinessException("所选道具不存在，请刷新后重试"));
        CharacterRole fromRole = characterRoleRepository.findById(request.getFromRoleId())
                .orElseThrow(() -> new BusinessException("原角色不存在，请刷新后重试"));
        CharacterRole toRole = characterRoleRepository.findById(request.getToRoleId())
                .orElseThrow(() -> new BusinessException("新角色不存在，请刷新后重试"));

        // 更换所属人物前核对：道具时代与新角色所属剧本的时代必须一致
        ScriptTheme toTheme = scriptThemeRepository.findById(toRole.getScriptThemeId())
                .orElseThrow(() -> new BusinessException("新角色所属剧本不存在，请刷新后重试"));
        if (isBlankEra(toTheme.getEra())) {
            throw new BusinessException("剧本「" + toTheme.getThemeName() + "」未设定时代，无法核对道具时代");
        }
        if (!eraMatches(toTheme.getEra(), prop.getEra())) {
            throw new BusinessException("跨时代更换被拦截：道具「" + prop.getPropCode() + " " + prop.getPropName()
                    + "」的时代为「" + propEraText(prop.getEra()) + "」，新角色「" + toRole.getRoleName()
                    + "」所属剧本「" + toTheme.getThemeName() + "」的时代为「" + toTheme.getEra()
                    + "」，两者时代不符，请改选同代道具或同代角色");
        }

        // 确认道具确实绑定在原角色名下，校验通过后再落库
        List<RoleProp> existingRoleProps = rolePropRepository.findByPropId(request.getPropId());
        RoleProp targetBinding = null;
        for (RoleProp rp : existingRoleProps) {
            if (rp.getCharacterRoleId().equals(request.getFromRoleId())) {
                targetBinding = rp;
                break;
            }
        }
        if (targetBinding == null) {
            throw new BusinessException("道具「" + prop.getPropName() + "」当前未绑定在原角色「"
                    + fromRole.getRoleName() + "」名下，请刷新后重试");
        }

        PropChangeRecord record = new PropChangeRecord();
        record.setPropId(request.getPropId());
        record.setCharacterRoleId(toRole.getId());
        record.setScriptThemeId(toRole.getScriptThemeId());
        record.setChangeType("更换");
        record.setBeforeValue("原角色: " + fromRole.getRoleName());
        record.setAfterValue("新角色: " + toRole.getRoleName());
        record.setChangeReason(request.getReason());
        record.setOperator(request.getOperator());
        changeRecordRepository.save(record);

        targetBinding.setCharacterRoleId(toRole.getId());
        targetBinding.setScriptThemeId(toRole.getScriptThemeId());
        rolePropRepository.save(targetBinding);
    }

    /**
     * 时代核对：剧本时代与道具时代去空白后比较，必须完全一致；
     * 任一方未填写时代也视为无法核对（不符），避免跨时代绑定漏网。
     */
    private boolean eraMatches(String themeEra, String propEra) {
        if (isBlankEra(themeEra) || isBlankEra(propEra)) {
            return false;
        }
        return themeEra.trim().equals(propEra.trim());
    }

    private boolean isBlankEra(String era) {
        return era == null || era.trim().isEmpty();
    }

    private String propEraText(String propEra) {
        return isBlankEra(propEra) ? "未设定" : propEra.trim();
    }

    private List<RolePropResponse> convertToResponseList(List<RoleProp> roleProps) {
        List<RolePropResponse> responses = new ArrayList<>();
        for (RoleProp rp : roleProps) {
            CharacterRole role = characterRoleRepository.findById(rp.getCharacterRoleId()).orElse(null);
            ScriptTheme theme = scriptThemeRepository.findById(rp.getScriptThemeId()).orElse(null);
            Prop prop = propRepository.findById(rp.getPropId()).orElse(null);
            responses.add(RolePropResponse.fromEntity(rp, role, theme, prop));
        }
        return responses;
    }
}