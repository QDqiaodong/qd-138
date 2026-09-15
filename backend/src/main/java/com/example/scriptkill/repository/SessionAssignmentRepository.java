package com.example.scriptkill.repository;

import com.example.scriptkill.entity.SessionAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionAssignmentRepository extends JpaRepository<SessionAssignment, Long> {
    List<SessionAssignment> findBySessionId(Long sessionId);
    Optional<SessionAssignment> findBySessionIdAndCharacterRoleId(Long sessionId, Long characterRoleId);
    Optional<SessionAssignment> findBySessionIdAndPerformerId(Long sessionId, Long performerId);
    long countBySessionId(Long sessionId);
    boolean existsByPerformerId(Long performerId);
}
