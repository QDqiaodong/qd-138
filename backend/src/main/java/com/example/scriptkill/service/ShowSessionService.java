package com.example.scriptkill.service;

import com.example.scriptkill.dto.request.SessionAssignRequest;
import com.example.scriptkill.dto.request.SessionCreateRequest;
import com.example.scriptkill.dto.response.ShowSessionResponse;

import java.util.List;

public interface ShowSessionService {

    List<ShowSessionResponse> listSessions();

    ShowSessionResponse getSession(Long id);

    ShowSessionResponse createSession(SessionCreateRequest request);

    /** 给场次里的某个人物指定演职人员；该人物已排过人时即为换人 */
    ShowSessionResponse assign(Long sessionId, SessionAssignRequest request);

    /** 撤掉某个人物的排班；已排好的场次会因此回退为排班中 */
    ShowSessionResponse unassign(Long sessionId, Long characterRoleId);

    /** 主题里的人物全部排完才允许标为已排好 */
    ShowSessionResponse markReady(Long sessionId);
}
