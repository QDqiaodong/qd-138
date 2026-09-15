package com.example.scriptkill.service;

import com.example.scriptkill.entity.CharacterRole;
import com.example.scriptkill.entity.RoleProp;
import com.example.scriptkill.entity.ScriptTheme;
import com.example.scriptkill.entity.SessionAssignment;
import com.example.scriptkill.entity.ShowSession;
import com.example.scriptkill.exception.BusinessException;
import com.example.scriptkill.repository.CharacterRoleRepository;
import com.example.scriptkill.repository.RolePropRepository;
import com.example.scriptkill.repository.ScriptThemeRepository;
import com.example.scriptkill.repository.SessionAssignmentRepository;
import com.example.scriptkill.repository.ShowSessionRepository;
import com.example.scriptkill.service.impl.CharacterRoleServiceImpl;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharacterRoleServiceTest {

    @Mock
    private CharacterRoleRepository characterRoleRepository;

    @Mock
    private ScriptThemeRepository scriptThemeRepository;

    @Mock
    private SessionAssignmentRepository sessionAssignmentRepository;

    @Mock
    private ShowSessionRepository showSessionRepository;

    @Mock
    private RolePropRepository rolePropRepository;

    @InjectMocks
    private CharacterRoleServiceImpl characterRoleService;

    private ScriptTheme theme;
    private CharacterRole role;
    private ShowSession session10;
    private ShowSession session20;

    @BeforeEach
    void setUp() {
        theme = new ScriptTheme();
        theme.setId(1L);
        theme.setThemeName("民国风云");

        role = new CharacterRole();
        role.setId(1L);
        role.setRoleName("许文强");
        role.setScriptThemeId(1L);

        session10 = new ShowSession();
        session10.setId(10L);
        session10.setSessionNo("CC000010");
        session10.setScriptThemeId(1L);
        session10.setStartTime(LocalDateTime.of(2026, 9, 16, 14, 0));
        session10.setEndTime(LocalDateTime.of(2026, 9, 16, 16, 0));
        session10.setStatus(ShowSession.STATUS_READY);

        session20 = new ShowSession();
        session20.setId(20L);
        session20.setSessionNo("CC000020");
        session20.setScriptThemeId(1L);
        session20.setStartTime(LocalDateTime.of(2026, 9, 17, 19, 0));
        session20.setEndTime(LocalDateTime.of(2026, 9, 17, 21, 0));
        session20.setStatus(ShowSession.STATUS_SCHEDULING);
    }

    private SessionAssignment assignment(Long sessionId) {
        SessionAssignment a = new SessionAssignment();
        a.setSessionId(sessionId);
        a.setCharacterRoleId(1L);
        a.setPerformerId(1L);
        return a;
    }

    @Test
    void 默认删除_没有排班_直接删掉() {
        when(characterRoleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(sessionAssignmentRepository.findByCharacterRoleId(1L)).thenReturn(List.of());

        characterRoleService.delete(1L);

        verify(characterRoleRepository).deleteById(1L);
    }

    @Test
    void 默认删除_还排在场次里_拦住并写出卡在哪几场_人物保留() {
        when(characterRoleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(sessionAssignmentRepository.findByCharacterRoleId(1L))
                .thenReturn(List.of(assignment(10L), assignment(20L)));
        when(showSessionRepository.findById(10L)).thenReturn(Optional.of(session10));
        when(showSessionRepository.findById(20L)).thenReturn(Optional.of(session20));
        when(scriptThemeRepository.findById(1L)).thenReturn(Optional.of(theme));

        BusinessException ex = assertThrows(BusinessException.class, () -> characterRoleService.delete(1L));

        String message = ex.getMessage();
        assertTrue(message.contains("许文强"), "提示要写出被拦的人物名");
        assertTrue(message.contains("CC000010"), "提示要写出卡点场次编号");
        assertTrue(message.contains("CC000020"), "提示要写出全部卡点场次");
        assertTrue(message.contains("民国风云"), "提示要带上场次主题");
        // 拦住时人物必须还在：既不能删人物，也不能顺手撤排班
        verify(characterRoleRepository, never()).deleteById(anyLong());
        verify(characterRoleRepository, never()).delete(any());
        verify(sessionAssignmentRepository, never()).deleteByCharacterRoleId(anyLong());
    }

    @Test
    void 默认删除_人物不存在_抛业务异常() {
        when(characterRoleRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> characterRoleService.delete(9L));
        verify(characterRoleRepository, never()).deleteById(anyLong());
    }

    @Test
    void 撤下并删除_先撤各场排班_已排好打回排班中_再删人物() {
        when(characterRoleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(sessionAssignmentRepository.findByCharacterRoleId(1L))
                .thenReturn(List.of(assignment(10L), assignment(20L)));
        when(showSessionRepository.findById(10L)).thenReturn(Optional.of(session10));
        when(showSessionRepository.findById(20L)).thenReturn(Optional.of(session20));
        when(rolePropRepository.findByCharacterRoleId(1L)).thenReturn(List.of());

        characterRoleService.unassignAndDelete(1L);

        // 先把各场里这个人物的排班撤下
        verify(sessionAssignmentRepository).deleteByCharacterRoleId(1L);
        // 已排好的场次撤完人物不齐，打回排班中
        assertEquals(ShowSession.STATUS_SCHEDULING, session10.getStatus());
        verify(showSessionRepository).save(session10);
        // 原本就在排班中的场次无需改状态
        assertEquals(ShowSession.STATUS_SCHEDULING, session20.getStatus());
        verify(showSessionRepository, never()).save(session20);
        // 最后连人带档一起拿掉
        verify(characterRoleRepository).delete(role);
        verify(characterRoleRepository, never()).deleteById(anyLong());
    }

    @Test
    void 撤下并删除_人物的道具绑定一并清掉() {
        RoleProp binding = new RoleProp();
        binding.setId(5L);
        binding.setCharacterRoleId(1L);
        binding.setPropId(2L);
        binding.setScriptThemeId(1L);

        when(characterRoleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(sessionAssignmentRepository.findByCharacterRoleId(1L)).thenReturn(List.of());
        when(rolePropRepository.findByCharacterRoleId(1L)).thenReturn(List.of(binding));

        characterRoleService.unassignAndDelete(1L);

        verify(rolePropRepository).deleteByCharacterRoleId(1L);
        verify(characterRoleRepository).delete(role);
    }

    @Test
    void 撤下并删除_没有排班_直接删人物() {
        when(characterRoleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(sessionAssignmentRepository.findByCharacterRoleId(1L)).thenReturn(List.of());
        when(rolePropRepository.findByCharacterRoleId(1L)).thenReturn(List.of());

        characterRoleService.unassignAndDelete(1L);

        verify(sessionAssignmentRepository, never()).deleteByCharacterRoleId(anyLong());
        verify(showSessionRepository, never()).save(any());
        verify(characterRoleRepository).delete(role);
    }

    @Test
    void 撤下并删除_人物不存在_抛业务异常() {
        when(characterRoleRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> characterRoleService.unassignAndDelete(9L));
        verify(characterRoleRepository, never()).delete(any());
    }
}
