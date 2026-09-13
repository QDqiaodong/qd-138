package com.example.scriptkill.service.impl;

import com.example.scriptkill.dto.request.PropBindRequest;
import com.example.scriptkill.dto.request.PropChangeRequest;
import com.example.scriptkill.dto.response.RolePropsResponse;
import com.example.scriptkill.dto.response.RolePropResponse;
import com.example.scriptkill.dto.response.ScriptThemeDetailResponse;
import com.example.scriptkill.entity.*;
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
        for (Long propId : request.getPropIds()) {
            PropChangeRecord record = new PropChangeRecord();
            record.setPropId(propId);
            record.setCharacterRoleId(request.getCharacterRoleId());
            record.setScriptThemeId(request.getScriptThemeId());
            record.setChangeType("绑定");
            record.setChangeReason(request.getReason());
            record.setOperator(request.getOperator());
            
            Prop prop = propRepository.findById(propId).orElse(null);
            CharacterRole role = characterRoleRepository.findById(request.getCharacterRoleId()).orElse(null);
            
            if (prop != null) {
                record.setAfterValue("角色: " + (role != null ? role.getRoleName() : "未知"));
            }
            
            changeRecordRepository.save(record);
            
            RoleProp roleProp = new RoleProp();
            roleProp.setCharacterRoleId(request.getCharacterRoleId());
            roleProp.setPropId(propId);
            roleProp.setScriptThemeId(request.getScriptThemeId());
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
        Prop prop = propRepository.findById(request.getPropId()).orElse(null);
        CharacterRole fromRole = characterRoleRepository.findById(request.getFromRoleId()).orElse(null);
        CharacterRole toRole = characterRoleRepository.findById(request.getToRoleId()).orElse(null);
        
        PropChangeRecord record = new PropChangeRecord();
        record.setPropId(request.getPropId());
        record.setCharacterRoleId(request.getToRoleId());
        record.setScriptThemeId(request.getScriptThemeId());
        record.setChangeType("更换");
        record.setBeforeValue("原角色: " + (fromRole != null ? fromRole.getRoleName() : "未知"));
        record.setAfterValue("新角色: " + (toRole != null ? toRole.getRoleName() : "未知"));
        record.setChangeReason(request.getReason());
        record.setOperator(request.getOperator());
        changeRecordRepository.save(record);
        
        List<RoleProp> existingRoleProps = rolePropRepository.findByPropId(request.getPropId());
        for (RoleProp rp : existingRoleProps) {
            if (rp.getCharacterRoleId().equals(request.getFromRoleId())) {
                rp.setCharacterRoleId(request.getToRoleId());
                rolePropRepository.save(rp);
                break;
            }
        }
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