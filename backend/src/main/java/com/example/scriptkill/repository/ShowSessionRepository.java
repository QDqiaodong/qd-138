package com.example.scriptkill.repository;

import com.example.scriptkill.entity.ShowSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowSessionRepository extends JpaRepository<ShowSession, Long> {

    List<ShowSession> findAllByOrderByStartTimeDesc();

    /**
     * 查同一演职人员在其他场次中、时间区间与本场相交的场次。
     * 区间相交判定：对方开演早于本场结束，且对方结束晚于本场开演；
     * 首尾相接（一场结束正是另一场开演）不算重叠。
     */
    @Query("SELECT s FROM ShowSession s, SessionAssignment a "
            + "WHERE a.sessionId = s.id "
            + "AND a.performerId = :performerId "
            + "AND s.id <> :excludeSessionId "
            + "AND s.startTime < :endTime AND s.endTime > :startTime")
    List<ShowSession> findOverlappingSessions(@Param("performerId") Long performerId,
                                              @Param("excludeSessionId") Long excludeSessionId,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);
}
