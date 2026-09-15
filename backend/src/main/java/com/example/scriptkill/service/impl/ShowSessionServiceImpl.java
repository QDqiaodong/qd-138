package com.example.scriptkill.service.impl;

import com.example.scriptkill.dto.request.SessionAssignRequest;
import com.example.scriptkill.dto.request.SessionCreateRequest;
import com.example.scriptkill.dto.response.ShowSessionResponse;
import com.example.scriptkill.entity.CharacterRole;
import com.example.scriptkill.entity.Performer;
import com.example.scriptkill.entity.ScriptTheme;
import com.example.scriptkill.entity.SessionAssignment;
import com.example.scriptkill.entity.ShowSession;
import com.example.scriptkill.exception.BusinessException;
import com.example.scriptkill.repository.CharacterRoleRepository;
import com.example.scriptkill.repository.PerformerRepository;
import com.example.scriptkill.repository.ScriptThemeRepository;
import com.example.scriptkill.repository.SessionAssignmentRepository;
import com.example.scriptkill.repository.ShowSessionRepository;
import com.example.scriptkill.service.ShowSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ShowSessionServiceImpl implements ShowSessionService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private ShowSessionRepository showSessionRepository;

    @Autowired
    private SessionAssignmentRepository sessionAssignmentRepository;

    @Autowired
    private ScriptThemeRepository scriptThemeRepository;

    @Autowired
    private CharacterRoleRepository characterRoleRepository;

    @Autowired
    private PerformerRepository performerRepository;

    @Override
    public List<ShowSessionResponse> listSessions() {
        List<ShowSessionResponse> responses = new ArrayList<>();
        for (ShowSession session : showSessionRepository.findAllByOrderByStartTimeDesc()) {
            responses.add(toResponse(session));
        }
        return responses;
    }

    @Override
    public ShowSessionResponse getSession(Long id) {
        ShowSession session = showSessionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("场次不存在，请刷新后重试"));
        return toResponse(session);
    }

    @Override
    @Transactional
    public ShowSessionResponse createSession(SessionCreateRequest request) {
        if (request.getScriptThemeId() == null) {
            throw new BusinessException("请选择本场演出的剧本主题");
        }
        ScriptTheme theme = scriptThemeRepository.findById(request.getScriptThemeId())
                .orElseThrow(() -> new BusinessException("所选剧本主题不存在，请刷新后重选"));
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new BusinessException("请填写本场开演和结束时间");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开演时间");
        }

        ShowSession session = new ShowSession();
        session.setScriptThemeId(theme.getId());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setStatus(ShowSession.STATUS_SCHEDULING);
        // 落库拿到自增ID后生成场次编号
        ShowSession saved = showSessionRepository.saveAndFlush(session);
        saved.setSessionNo(String.format("CC%06d", saved.getId()));
        return toResponse(showSessionRepository.save(saved));
    }

    @Override
    @Transactional
    public ShowSessionResponse assign(Long sessionId, SessionAssignRequest request) {
        ShowSession session = showSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("场次不存在，请刷新后重试"));
        if (request.getCharacterRoleId() == null || request.getPerformerId() == null) {
            throw new BusinessException("请选择要排班的人物和演职人员");
        }
        CharacterRole role = characterRoleRepository.findById(request.getCharacterRoleId())
                .orElseThrow(() -> new BusinessException("所选人物不存在，请刷新后重试"));
        if (!role.getScriptThemeId().equals(session.getScriptThemeId())) {
            throw new BusinessException("人物「" + role.getRoleName() + "」不属于本场主题，不能排进这一场");
        }
        Performer performer = performerRepository.findById(request.getPerformerId())
                .orElseThrow(() -> new BusinessException("所选演职人员不存在，请刷新名册后重选"));
        // 停演人员不能排进场次：停演立即生效，要上场先恢复演出或换别人
        if (Boolean.TRUE.equals(performer.getSuspended())) {
            throw new BusinessException("「" + performer.getPerformerName() + "」已标停演，停演原因："
                    + performer.getSuspendReason() + "；不能排进场次，请换人或在名册中恢复演出");
        }

        // 同一个人在同一场里不能演两个人物
        Optional<SessionAssignment> samePerformer = sessionAssignmentRepository
                .findBySessionIdAndPerformerId(sessionId, performer.getId());
        if (samePerformer.isPresent() && !samePerformer.get().getCharacterRoleId().equals(role.getId())) {
            CharacterRole otherRole = characterRoleRepository.findById(samePerformer.get().getCharacterRoleId()).orElse(null);
            String otherRoleName = otherRole != null ? otherRole.getRoleName() : "另一个人物";
            throw new BusinessException("「" + performer.getPerformerName() + "」在本场已饰演「" + otherRoleName
                    + "」，同一个人在同一场里不能演两个人物");
        }

        // 同一个人的两场时间叠在一起要拦住，并写出跟哪一场撞了
        List<ShowSession> conflicts = showSessionRepository.findOverlappingSessions(
                performer.getId(), sessionId, session.getStartTime(), session.getEndTime());
        if (!conflicts.isEmpty()) {
            List<String> parts = new ArrayList<>();
            for (ShowSession conflict : conflicts) {
                ScriptTheme conflictTheme = scriptThemeRepository.findById(conflict.getScriptThemeId()).orElse(null);
                parts.add("场次「" + conflict.getSessionNo()
                        + (conflictTheme != null ? " " + conflictTheme.getThemeName() : "") + "」（"
                        + conflict.getStartTime().format(TIME_FMT) + " - " + conflict.getEndTime().format(TIME_FMT) + "）");
            }
            throw new BusinessException("「" + performer.getPerformerName() + "」与本场时间重叠，撞上了"
                    + String.join("、", parts));
        }

        // 该人物已排过人就是换人：直接改排班记录，换完刷新还是新的人
        SessionAssignment assignment = sessionAssignmentRepository
                .findBySessionIdAndCharacterRoleId(sessionId, role.getId())
                .orElseGet(SessionAssignment::new);
        assignment.setSessionId(sessionId);
        assignment.setCharacterRoleId(role.getId());
        assignment.setPerformerId(performer.getId());
        try {
            sessionAssignmentRepository.saveAndFlush(assignment);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("「" + performer.getPerformerName() + "」在本场已有其他人物的排班，"
                    + "同一个人在同一场里不能演两个人物");
        }
        return toResponse(session);
    }

    @Override
    @Transactional
    public ShowSessionResponse unassign(Long sessionId, Long characterRoleId) {
        ShowSession session = showSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("场次不存在，请刷新后重试"));
        SessionAssignment assignment = sessionAssignmentRepository
                .findBySessionIdAndCharacterRoleId(sessionId, characterRoleId)
                .orElseThrow(() -> new BusinessException("该人物本场还没有排班，无需撤下"));
        sessionAssignmentRepository.delete(assignment);
        // 撤下后人物不再齐，已排好的场次回退为排班中
        if (ShowSession.STATUS_READY.equals(session.getStatus())) {
            session.setStatus(ShowSession.STATUS_SCHEDULING);
            showSessionRepository.save(session);
        }
        return toResponse(session);
    }

    @Override
    @Transactional
    public ShowSessionResponse markReady(Long sessionId) {
        ShowSession session = showSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("场次不存在，请刷新后重试"));
        if (ShowSession.STATUS_RUNNING.equals(session.getStatus())) {
            throw new BusinessException("本场已在开演中，不能再标为已排好");
        }
        long total = characterRoleRepository.findByScriptThemeId(session.getScriptThemeId()).size();
        long assigned = sessionAssignmentRepository.countBySessionId(sessionId);
        if (total == 0) {
            throw new BusinessException("该主题下还没有人物，不能标为已排好");
        }
        // 主题里的人物没排完，这一场不能标成已排好
        if (assigned < total) {
            throw new BusinessException("还有 " + (total - assigned) + " 个人物未排人，人物排完才能标为已排好");
        }
        session.setStatus(ShowSession.STATUS_READY);
        return toResponse(showSessionRepository.save(session));
    }

    @Override
    @Transactional
    public ShowSessionResponse start(Long sessionId) {
        ShowSession session = showSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("场次不存在，请刷新后重试"));
        if (ShowSession.STATUS_RUNNING.equals(session.getStatus())) {
            throw new BusinessException("本场已在开演中，无需重复开演");
        }
        if (!ShowSession.STATUS_READY.equals(session.getStatus())) {
            throw new BusinessException("本场还没排好，人物排完并标为已排好才能开演");
        }
        // 只要这场还有待换就拦住开演，写出停演演员、所演人物和停演原因原文
        List<String> blocked = findPendingReplacements(session);
        if (!blocked.isEmpty()) {
            throw new BusinessException("本场还有演员待换，不能开演："
                    + String.join("；", blocked)
                    + "。请先换掉待换的人再开演");
        }
        session.setStatus(ShowSession.STATUS_RUNNING);
        return toResponse(showSessionRepository.save(session));
    }

    /**
     * 汇总本场未开演时名单里的待换项：停演演员名、所演人物、停演原因原文。
     * 仅未开演（排班中/已排好）的场次有待换一说。
     */
    private List<String> findPendingReplacements(ShowSession session) {
        List<String> blocked = new ArrayList<>();
        if (ShowSession.STATUS_RUNNING.equals(session.getStatus())) {
            return blocked;
        }
        for (SessionAssignment assignment : sessionAssignmentRepository.findBySessionId(session.getId())) {
            Performer performer = performerRepository.findById(assignment.getPerformerId()).orElse(null);
            if (performer == null || !Boolean.TRUE.equals(performer.getSuspended())) {
                continue;
            }
            CharacterRole role = characterRoleRepository.findById(assignment.getCharacterRoleId()).orElse(null);
            blocked.add("「" + performer.getPerformerName() + "」饰演「"
                    + (role != null ? role.getRoleName() : "未知人物")
                    + "」，停演原因：" + performer.getSuspendReason());
        }
        return blocked;
    }

    /**
     * 主题名、人物名、演职人员名一律在读取时按 ID 实时关联，不存冗余快照，
     * 改名后刷新即为新名。
     */
    private ShowSessionResponse toResponse(ShowSession session) {
        ShowSessionResponse response = new ShowSessionResponse();
        response.setId(session.getId());
        response.setSessionNo(session.getSessionNo());
        response.setScriptThemeId(session.getScriptThemeId());
        ScriptTheme theme = session.getScriptThemeId() != null
                ? scriptThemeRepository.findById(session.getScriptThemeId()).orElse(null)
                : null;
        response.setThemeName(theme != null ? theme.getThemeName() : "");
        response.setStartTime(session.getStartTime());
        response.setEndTime(session.getEndTime());
        response.setStatus(session.getStatus());

        List<CharacterRole> roles = session.getScriptThemeId() != null
                ? characterRoleRepository.findByScriptThemeId(session.getScriptThemeId())
                : new ArrayList<>();
        response.setTotalRoles(roles.size());

        List<SessionAssignment> assignments = sessionAssignmentRepository.findBySessionId(session.getId());
        response.setAssignedCount(assignments.size());

        // 待换只挂在未开演的场次名单上；开演中及以后不再出现待换
        boolean unstarted = !ShowSession.STATUS_RUNNING.equals(session.getStatus());
        int pendingCount = 0;
        List<ShowSessionResponse.AssignmentView> views = new ArrayList<>();
        for (SessionAssignment assignment : assignments) {
            ShowSessionResponse.AssignmentView view = new ShowSessionResponse.AssignmentView();
            view.setCharacterRoleId(assignment.getCharacterRoleId());
            CharacterRole role = characterRoleRepository.findById(assignment.getCharacterRoleId()).orElse(null);
            view.setRoleName(role != null ? role.getRoleName() : "");
            view.setPerformerId(assignment.getPerformerId());
            Performer performer = performerRepository.findById(assignment.getPerformerId()).orElse(null);
            view.setPerformerName(performer != null ? performer.getPerformerName() : "");
            if (unstarted && performer != null && Boolean.TRUE.equals(performer.getSuspended())) {
                view.setPendingReplacement(true);
                view.setSuspendReason(performer.getSuspendReason());
                pendingCount++;
            }
            views.add(view);
        }
        response.setPendingReplacementCount(pendingCount);
        response.setAssignments(views);
        return response;
    }
}
