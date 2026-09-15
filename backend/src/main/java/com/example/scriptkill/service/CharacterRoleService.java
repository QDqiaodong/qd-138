package com.example.scriptkill.service;

import com.example.scriptkill.dto.response.CharacterRoleResponse;

import java.util.List;

public interface CharacterRoleService {

    /** 全部人物，所属剧本名读取时实时关联当前主题 */
    List<CharacterRoleResponse> listAll();

    /** 指定剧本下的人物 */
    List<CharacterRoleResponse> listByTheme(Long themeId);
}
