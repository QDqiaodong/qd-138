package com.example.scriptkill.service.impl;

import com.example.scriptkill.dto.response.CharacterRoleResponse;
import com.example.scriptkill.entity.CharacterRole;
import com.example.scriptkill.entity.ScriptTheme;
import com.example.scriptkill.entity.SessionAssignment;
import com.example.scriptkill.entity.ShowSession;
import com.example.scriptkill.exception.BusinessException;
import com.example.scriptkill.repository.CharacterRoleRepository;
import com.example.scriptkill.repository.RolePropRepository;
import com.example.scriptkill.repository.ScriptThemeRepository;
import com.example.scriptkill.repository.SessionAssignmentRepository;
import com.example.scriptkill.repository.ShowSessionRepository;
import com.example.scriptkill.service.CharacterRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CharacterRoleServiceImpl implements CharacterRoleService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private CharacterRoleRepository characterRoleRepository;

    @Autowired
    private ScriptThemeRepository scriptThemeRepository;

    @Autowired
    private SessionAssignmentRepository sessionAssignmentRepository;

    @Autowired
    private ShowSessionRepository showSessionRepository;

    @Autowired
    private RolePropRepository rolePropRepository;

    @Override
    public List<CharacterRoleResponse> listAll() {
        return toResponses(characterRoleRepository.findAll());
    }

    @Override
    public List<CharacterRoleResponse> listByTheme(Long themeId) {
        return toResponses(characterRoleRepository.findByScriptThemeId(themeId));
    }

    // 默认删除：这个人物只要还排在任意一场，就必须拦住，
    // 不能直接删掉让排班页进度和已排好标记挂在一个不存在的人物上
    @Override
    @Transactional
    public void delete(Long id) {
        CharacterRole role = characterRoleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("人物不存在，请刷新后重试"));
        List<SessionAssignment> assignments = sessionAssignmentRepository.findByCharacterRoleId(id);
        if (!assignments.isEmpty()) {
            throw new BusinessException("人物「" + role.getRoleName() + "」还排在 "
                    + assignments.size() + " 场演出里，默认删除已拦住："
                    + describeBlockedSessions(assignments)
                    + "。请先到对应场次撤下排班；如要连人带档一起拿掉，请用「撤下并删除」");
        }
        characterRoleRepository.deleteById(id);
    }

    // 撤下并删除：与默认删除是两条路——先把各场里这个人物的排班逐条撤下，
    // 再删人物；撤过后这些场次人物不再齐，已排好的场次一律打回排班中
    @Override
    @Transactional
    public void unassignAndDelete(Long id) {
        CharacterRole role = characterRoleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("人物不存在，请刷新后重试"));
        List<SessionAssignment> assignments = sessionAssignmentRepository.findByCharacterRoleId(id);
        if (!assignments.isEmpty()) {
            // 去重收集被撤排班的场次，逐场撤下；已排好的场次撤完不再齐，打回排班中
            Set<Long> affectedSessionIds = new HashSet<>();
            for (SessionAssignment assignment : assignments) {
                affectedSessionIds.add(assignment.getSessionId());
            }
            sessionAssignmentRepository.deleteByCharacterRoleId(id);
            // 先落库撤排班，再动场次状态和人物，删除顺序不依赖 Hibernate 刷新队列
            sessionAssignmentRepository.flush();
            for (Long sessionId : affectedSessionIds) {
                ShowSession session = showSessionRepository.findById(sessionId).orElse(null);
                if (session != null && ShowSession.STATUS_READY.equals(session.getStatus())) {
                    session.setStatus(ShowSession.STATUS_SCHEDULING);
                    showSessionRepository.save(session);
                }
            }
        }
        // 道具绑定随人物一并清掉，不依赖数据库外键级联，确保 H2 等环境也清干净
        if (!rolePropRepository.findByCharacterRoleId(id).isEmpty()) {
            rolePropRepository.deleteByCharacterRoleId(id);
            rolePropRepository.flush();
        }
        characterRoleRepository.delete(role);
    }

    // 写出这个人物还卡在哪几场：场次编号 + 主题名 + 开演时间，便于场务逐场去撤
    private String describeBlockedSessions(List<SessionAssignment> assignments) {
        Set<Long> sessionIds = new HashSet<>();
        List<String> parts = new ArrayList<>();
        for (SessionAssignment assignment : assignments) {
            if (!sessionIds.add(assignment.getSessionId())) {
                continue;
            }
            ShowSession session = showSessionRepository.findById(assignment.getSessionId()).orElse(null);
            if (session == null) {
                continue;
            }
            ScriptTheme theme = session.getScriptThemeId() != null
                    ? scriptThemeRepository.findById(session.getScriptThemeId()).orElse(null)
                    : null;
            parts.add("场次「" + session.getSessionNo()
                    + (theme != null ? " " + theme.getThemeName() : "") + "」（"
                    + session.getStartTime().format(TIME_FMT) + " - "
                    + session.getEndTime().format(TIME_FMT) + "）");
        }
        return String.join("、", parts);
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
