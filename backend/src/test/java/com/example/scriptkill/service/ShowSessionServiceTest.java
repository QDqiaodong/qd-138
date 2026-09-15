package com.example.scriptkill.service;

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
import com.example.scriptkill.service.impl.ShowSessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowSessionServiceTest {

    @Mock
    private ShowSessionRepository showSessionRepository;

    @Mock
    private SessionAssignmentRepository sessionAssignmentRepository;

    @Mock
    private ScriptThemeRepository scriptThemeRepository;

    @Mock
    private CharacterRoleRepository characterRoleRepository;

    @Mock
    private PerformerRepository performerRepository;

    @InjectMocks
    private ShowSessionServiceImpl showSessionService;

    private ScriptTheme theme;
    private CharacterRole role1;
    private CharacterRole role2;
    private Performer performer1;
    private Performer performer2;
    private ShowSession session;

    @BeforeEach
    void setUp() {
        theme = new ScriptTheme();
        theme.setId(1L);
        theme.setThemeName("民国风云");

        role1 = new CharacterRole();
        role1.setId(1L);
        role1.setRoleName("许文强");
        role1.setScriptThemeId(1L);

        role2 = new CharacterRole();
        role2.setId(2L);
        role2.setRoleName("冯程程");
        role2.setScriptThemeId(1L);

        performer1 = new Performer();
        performer1.setId(1L);
        performer1.setPerformerName("张子昂");

        performer2 = new Performer();
        performer2.setId(2L);
        performer2.setPerformerName("李慕白");

        session = new ShowSession();
        session.setId(10L);
        session.setSessionNo("CC000010");
        session.setScriptThemeId(1L);
        session.setStartTime(LocalDateTime.of(2026, 9, 16, 14, 0));
        session.setEndTime(LocalDateTime.of(2026, 9, 16, 16, 0));
        session.setStatus(ShowSession.STATUS_SCHEDULING);
    }

    private SessionAssignRequest assignRequest(Long roleId, Long performerId) {
        SessionAssignRequest req = new SessionAssignRequest();
        req.setCharacterRoleId(roleId);
        req.setPerformerId(performerId);
        return req;
    }

    private SessionAssignment assignment(Long roleId, Long performerId) {
        SessionAssignment a = new SessionAssignment();
        a.setSessionId(10L);
        a.setCharacterRoleId(roleId);
        a.setPerformerId(performerId);
        return a;
    }

    /** 排人后 toResponse 读取排班列表时返回指定内容；只桩实际会出现的人物和演员，避免严格桩报错 */
    private void stubResponseReads(List<CharacterRole> roles, List<SessionAssignment> assignments) {
        when(scriptThemeRepository.findById(1L)).thenReturn(Optional.of(theme));
        when(characterRoleRepository.findByScriptThemeId(1L)).thenReturn(roles);
        when(sessionAssignmentRepository.findBySessionId(10L)).thenReturn(assignments);
        for (SessionAssignment a : assignments) {
            CharacterRole role = a.getCharacterRoleId().equals(1L) ? role1 : role2;
            Performer performer = a.getPerformerId().equals(1L) ? performer1 : performer2;
            when(characterRoleRepository.findById(a.getCharacterRoleId())).thenReturn(Optional.of(role));
            when(performerRepository.findById(a.getPerformerId())).thenReturn(Optional.of(performer));
        }
    }

    @Test
    void 开一场成功_生成场次编号_默认排班中() {
        SessionCreateRequest req = new SessionCreateRequest();
        req.setScriptThemeId(1L);
        req.setStartTime(LocalDateTime.of(2026, 9, 16, 14, 0));
        req.setEndTime(LocalDateTime.of(2026, 9, 16, 16, 0));
        when(scriptThemeRepository.findById(1L)).thenReturn(Optional.of(theme));
        when(showSessionRepository.saveAndFlush(any(ShowSession.class))).thenAnswer(inv -> {
            ShowSession s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });
        when(showSessionRepository.save(any(ShowSession.class))).thenAnswer(inv -> inv.getArgument(0));
        stubResponseReads(List.of(role1, role2), List.of());

        ShowSessionResponse resp = showSessionService.createSession(req);

        assertEquals("CC000010", resp.getSessionNo());
        assertEquals("民国风云", resp.getThemeName());
        assertEquals(ShowSession.STATUS_SCHEDULING, resp.getStatus());
        assertEquals(2, resp.getTotalRoles());
        assertEquals(0, resp.getAssignedCount());
    }

    @Test
    void 开一场_结束时间必须晚于开演时间() {
        SessionCreateRequest req = new SessionCreateRequest();
        req.setScriptThemeId(1L);
        req.setStartTime(LocalDateTime.of(2026, 9, 16, 14, 0));
        req.setEndTime(LocalDateTime.of(2026, 9, 16, 14, 0));
        when(scriptThemeRepository.findById(1L)).thenReturn(Optional.of(theme));

        BusinessException ex = assertThrows(BusinessException.class, () -> showSessionService.createSession(req));
        assertTrue(ex.getMessage().contains("结束时间必须晚于开演时间"));
        verify(showSessionRepository, never()).saveAndFlush(any());
    }

    @Test
    void 排人成功_人物不属于本场主题被拦截() {
        when(showSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(characterRoleRepository.findById(1L)).thenReturn(Optional.of(role1));
        when(performerRepository.findById(1L)).thenReturn(Optional.of(performer1));
        when(sessionAssignmentRepository.findBySessionIdAndPerformerId(10L, 1L)).thenReturn(Optional.empty());
        when(showSessionRepository.findOverlappingSessions(eq(1L), eq(10L), any(), any())).thenReturn(List.of());
        when(sessionAssignmentRepository.findBySessionIdAndCharacterRoleId(10L, 1L)).thenReturn(Optional.empty());
        when(sessionAssignmentRepository.saveAndFlush(any(SessionAssignment.class))).thenAnswer(inv -> inv.getArgument(0));
        stubResponseReads(List.of(role1, role2), List.of(assignment(1L, 1L)));

        ShowSessionResponse resp = showSessionService.assign(10L, assignRequest(1L, 1L));

        assertEquals(1, resp.getAssignedCount());
        assertEquals("张子昂", resp.getAssignments().get(0).getPerformerName());
        verify(sessionAssignmentRepository).saveAndFlush(any(SessionAssignment.class));

        CharacterRole otherThemeRole = new CharacterRole();
        otherThemeRole.setId(9L);
        otherThemeRole.setRoleName("李逍遥");
        otherThemeRole.setScriptThemeId(2L);
        when(characterRoleRepository.findById(9L)).thenReturn(Optional.of(otherThemeRole));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> showSessionService.assign(10L, assignRequest(9L, 1L)));
        assertTrue(ex.getMessage().contains("不属于本场主题"));
    }

    @Test
    void 同一个人在同一场里不能演两个人物() {
        when(showSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(characterRoleRepository.findById(2L)).thenReturn(Optional.of(role2));
        when(performerRepository.findById(1L)).thenReturn(Optional.of(performer1));
        // 张子昂在本场已演许文强，再排他演冯程程要拦住
        when(sessionAssignmentRepository.findBySessionIdAndPerformerId(10L, 1L))
                .thenReturn(Optional.of(assignment(1L, 1L)));
        when(characterRoleRepository.findById(1L)).thenReturn(Optional.of(role1));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> showSessionService.assign(10L, assignRequest(2L, 1L)));
        assertTrue(ex.getMessage().contains("同一个人在同一场里不能演两个人物"));
        assertTrue(ex.getMessage().contains("许文强"));
        verify(sessionAssignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void 同一个人两场时间重叠被拦截_提示写出撞上的场次() {
        ShowSession other = new ShowSession();
        other.setId(20L);
        other.setSessionNo("CC000020");
        other.setScriptThemeId(1L);
        other.setStartTime(LocalDateTime.of(2026, 9, 16, 15, 0));
        other.setEndTime(LocalDateTime.of(2026, 9, 16, 17, 0));

        when(showSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(characterRoleRepository.findById(1L)).thenReturn(Optional.of(role1));
        when(performerRepository.findById(1L)).thenReturn(Optional.of(performer1));
        when(sessionAssignmentRepository.findBySessionIdAndPerformerId(10L, 1L)).thenReturn(Optional.empty());
        when(showSessionRepository.findOverlappingSessions(eq(1L), eq(10L), any(), any()))
                .thenReturn(List.of(other));
        when(scriptThemeRepository.findById(1L)).thenReturn(Optional.of(theme));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> showSessionService.assign(10L, assignRequest(1L, 1L)));
        assertTrue(ex.getMessage().contains("CC000020"));
        assertTrue(ex.getMessage().contains("民国风云"));
        assertTrue(ex.getMessage().contains("张子昂"));
        verify(sessionAssignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void 首尾相接不算重叠_可以排() {
        when(showSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(characterRoleRepository.findById(1L)).thenReturn(Optional.of(role1));
        when(performerRepository.findById(1L)).thenReturn(Optional.of(performer1));
        when(sessionAssignmentRepository.findBySessionIdAndPerformerId(10L, 1L)).thenReturn(Optional.empty());
        // 另一场 16:00 开演，与本场 14:00-16:00 首尾相接，查询返回空即不拦
        when(showSessionRepository.findOverlappingSessions(eq(1L), eq(10L), any(), any())).thenReturn(List.of());
        when(sessionAssignmentRepository.findBySessionIdAndCharacterRoleId(10L, 1L)).thenReturn(Optional.empty());
        when(sessionAssignmentRepository.saveAndFlush(any(SessionAssignment.class))).thenAnswer(inv -> inv.getArgument(0));
        stubResponseReads(List.of(role1, role2), List.of(assignment(1L, 1L)));

        ShowSessionResponse resp = showSessionService.assign(10L, assignRequest(1L, 1L));
        assertEquals(1, resp.getAssignedCount());
    }

    @Test
    void 人物没排完不能标已排好_排完才可以() {
        when(showSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(characterRoleRepository.findByScriptThemeId(1L)).thenReturn(List.of(role1, role2));
        when(sessionAssignmentRepository.countBySessionId(10L)).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> showSessionService.markReady(10L));
        assertTrue(ex.getMessage().contains("还有 1 个人物未排人"));
        assertEquals(ShowSession.STATUS_SCHEDULING, session.getStatus());

        when(sessionAssignmentRepository.countBySessionId(10L)).thenReturn(2L);
        when(showSessionRepository.save(any(ShowSession.class))).thenAnswer(inv -> inv.getArgument(0));
        stubResponseReads(List.of(role1, role2), List.of(assignment(1L, 1L), assignment(2L, 2L)));

        ShowSessionResponse resp = showSessionService.markReady(10L);
        assertEquals(ShowSession.STATUS_READY, resp.getStatus());
    }

    @Test
    void 排错了可以换_换完返回新的人() {
        when(showSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(characterRoleRepository.findById(1L)).thenReturn(Optional.of(role1));
        when(performerRepository.findById(2L)).thenReturn(Optional.of(performer2));
        when(sessionAssignmentRepository.findBySessionIdAndPerformerId(10L, 2L)).thenReturn(Optional.empty());
        when(showSessionRepository.findOverlappingSessions(eq(2L), eq(10L), any(), any())).thenReturn(List.of());
        // 许文强原来排的是张子昂，换成李慕白
        SessionAssignment existing = assignment(1L, 1L);
        when(sessionAssignmentRepository.findBySessionIdAndCharacterRoleId(10L, 1L))
                .thenReturn(Optional.of(existing));
        when(sessionAssignmentRepository.saveAndFlush(any(SessionAssignment.class))).thenAnswer(inv -> inv.getArgument(0));
        stubResponseReads(List.of(role1, role2), List.of(assignment(1L, 2L)));

        ShowSessionResponse resp = showSessionService.assign(10L, assignRequest(1L, 2L));

        assertEquals("李慕白", resp.getAssignments().get(0).getPerformerName());
        assertEquals(2L, existing.getPerformerId());
    }

    @Test
    void 已排好的场次撤人后回退为排班中() {
        session.setStatus(ShowSession.STATUS_READY);
        when(showSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(sessionAssignmentRepository.findBySessionIdAndCharacterRoleId(10L, 1L))
                .thenReturn(Optional.of(assignment(1L, 1L)));
        when(showSessionRepository.save(any(ShowSession.class))).thenAnswer(inv -> inv.getArgument(0));
        stubResponseReads(List.of(role1, role2), List.of(assignment(2L, 2L)));

        ShowSessionResponse resp = showSessionService.unassign(10L, 1L);

        assertEquals(ShowSession.STATUS_SCHEDULING, resp.getStatus());
        verify(sessionAssignmentRepository).delete(any(SessionAssignment.class));
    }

    @Test
    void 停演人员不能排进场次_提示写出停演原因() {
        performer1.setSuspended(true);
        performer1.setSuspendReason("突发高烧，临时无法上场");
        when(showSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(characterRoleRepository.findById(1L)).thenReturn(Optional.of(role1));
        when(performerRepository.findById(1L)).thenReturn(Optional.of(performer1));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> showSessionService.assign(10L, assignRequest(1L, 1L)));
        assertTrue(ex.getMessage().contains("张子昂"));
        assertTrue(ex.getMessage().contains("突发高烧，临时无法上场"));
        verify(sessionAssignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void 停演后未开演场次名单标待换_开演中不标() {
        performer1.setSuspended(true);
        performer1.setSuspendReason("家里有事请假");
        // 已排好（未开演）：名单里张子昂标待换并带原因原文
        session.setStatus(ShowSession.STATUS_READY);
        when(showSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        stubResponseReads(List.of(role1, role2), List.of(assignment(1L, 1L), assignment(2L, 2L)));

        ShowSessionResponse ready = showSessionService.getSession(10L);
        assertEquals(1, ready.getPendingReplacementCount());
        ShowSessionResponse.AssignmentView pending = ready.getAssignments().get(0);
        assertTrue(pending.isPendingReplacement());
        assertEquals("张子昂", pending.getPerformerName());
        assertEquals("许文强", pending.getRoleName());
        assertEquals("家里有事请假", pending.getSuspendReason());
        assertFalse(ready.getAssignments().get(1).isPendingReplacement());

        // 开演中：名单里不再出现待换
        session.setStatus(ShowSession.STATUS_RUNNING);
        ShowSessionResponse running = showSessionService.getSession(10L);
        assertEquals(0, running.getPendingReplacementCount());
        assertTrue(running.getAssignments().stream().noneMatch(ShowSessionResponse.AssignmentView::isPendingReplacement));
    }

    @Test
    void 开演被待换拦住_写出演员名所演人物和原因原文() {
        session.setStatus(ShowSession.STATUS_READY);
        performer1.setSuspended(true);
        performer1.setSuspendReason("突发高烧，临时无法上场");
        when(showSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(sessionAssignmentRepository.findBySessionId(10L))
                .thenReturn(List.of(assignment(1L, 1L), assignment(2L, 2L)));
        when(performerRepository.findById(1L)).thenReturn(Optional.of(performer1));
        when(characterRoleRepository.findById(1L)).thenReturn(Optional.of(role1));

        BusinessException ex = assertThrows(BusinessException.class, () -> showSessionService.start(10L));
        assertTrue(ex.getMessage().contains("张子昂"), "要写出停演演员名：" + ex.getMessage());
        assertTrue(ex.getMessage().contains("许文强"), "要写出所演人物：" + ex.getMessage());
        assertTrue(ex.getMessage().contains("突发高烧，临时无法上场"), "要写出原因原文：" + ex.getMessage());
        // 拦下后场次仍是已排好，不能转成开演中
        assertEquals(ShowSession.STATUS_READY, session.getStatus());
        verify(showSessionRepository, never()).save(any());
    }

    @Test
    void 开演成功_已排好转开演中_名单不再有待换() {
        session.setStatus(ShowSession.STATUS_READY);
        when(showSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(showSessionRepository.save(any(ShowSession.class))).thenAnswer(inv -> inv.getArgument(0));
        stubResponseReads(List.of(role1, role2), List.of(assignment(1L, 1L), assignment(2L, 2L)));

        ShowSessionResponse resp = showSessionService.start(10L);

        assertEquals(ShowSession.STATUS_RUNNING, resp.getStatus());
        assertEquals(0, resp.getPendingReplacementCount());
        assertTrue(resp.getAssignments().stream().noneMatch(ShowSessionResponse.AssignmentView::isPendingReplacement));
    }

    @Test
    void 没排好不能开演_重复开演被拦() {
        when(showSessionRepository.findById(10L)).thenReturn(Optional.of(session));

        BusinessException notReady = assertThrows(BusinessException.class, () -> showSessionService.start(10L));
        assertTrue(notReady.getMessage().contains("还没排好"));
        assertEquals(ShowSession.STATUS_SCHEDULING, session.getStatus());

        session.setStatus(ShowSession.STATUS_RUNNING);
        BusinessException again = assertThrows(BusinessException.class, () -> showSessionService.start(10L));
        assertTrue(again.getMessage().contains("已在开演中"));
    }
}
