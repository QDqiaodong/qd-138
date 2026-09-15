package com.example.scriptkill.service;

import com.example.scriptkill.dto.response.CharacterRoleResponse;

import java.util.List;

public interface CharacterRoleService {

    /** 全部人物，所属剧本名读取时实时关联当前主题 */
    List<CharacterRoleResponse> listAll();

    /** 指定剧本下的人物 */
    List<CharacterRoleResponse> listByTheme(Long themeId);

    /**
     * 默认删除：人物只要还排在任意一场就拦住，并在提示里写出卡在哪几场。
     * 不会顺带撤排班，也不会删人物。
     */
    void delete(Long id);

    /**
     * 撤下并删除：先把各场里这个人物的排班全部撤下，再连人带档一起拿掉。
     * 撤完后该人物没排齐的场次不能再挂「已排好」，统一打回「排班中」。
     */
    void unassignAndDelete(Long id);
}
