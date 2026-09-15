package com.example.scriptkill.service;

import com.example.scriptkill.entity.Performer;
import com.example.scriptkill.exception.BusinessException;
import com.example.scriptkill.repository.PerformerRepository;
import com.example.scriptkill.repository.SessionAssignmentRepository;
import com.example.scriptkill.service.impl.PerformerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerformerServiceTest {

    @Mock
    private PerformerRepository performerRepository;

    @Mock
    private SessionAssignmentRepository sessionAssignmentRepository;

    @InjectMocks
    private PerformerServiceImpl performerService;

    private Performer performer;

    @BeforeEach
    void setUp() {
        performer = new Performer();
        performer.setId(1L);
        performer.setPerformerName("张子昂");
        performer.setSuspended(false);
    }

    @Test
    void 标停演成功_落停演标记和原因原文() {
        when(performerRepository.findById(1L)).thenReturn(Optional.of(performer));
        when(performerRepository.save(any(Performer.class))).thenAnswer(inv -> inv.getArgument(0));

        Performer saved = performerService.suspend(1L, "  突发高烧，临时无法上场  ");

        assertTrue(saved.getSuspended());
        assertEquals("突发高烧，临时无法上场", saved.getSuspendReason());
        verify(performerRepository).save(performer);
    }

    @Test
    void 标停演必须填原因() {
        when(performerRepository.findById(1L)).thenReturn(Optional.of(performer));

        BusinessException ex = assertThrows(BusinessException.class, () -> performerService.suspend(1L, "   "));
        assertTrue(ex.getMessage().contains("请填写停演原因"));
        assertFalse(performer.getSuspended());
        verify(performerRepository, never()).save(any());
    }

    @Test
    void 恢复演出后清掉停演标记和原因() {
        performer.setSuspended(true);
        performer.setSuspendReason("家里有事请假");
        when(performerRepository.findById(1L)).thenReturn(Optional.of(performer));
        when(performerRepository.save(any(Performer.class))).thenAnswer(inv -> inv.getArgument(0));

        Performer saved = performerService.resume(1L);

        assertFalse(saved.getSuspended());
        assertNull(saved.getSuspendReason());
    }

    @Test
    void 标停演和恢复都要查得到人() {
        when(performerRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> performerService.suspend(9L, "原因"));
        assertThrows(BusinessException.class, () -> performerService.resume(9L));
        verify(performerRepository, never()).save(any());
    }
}
