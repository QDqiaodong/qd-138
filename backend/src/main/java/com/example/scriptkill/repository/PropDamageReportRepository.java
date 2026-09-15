package com.example.scriptkill.repository;

import com.example.scriptkill.entity.PropDamageReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropDamageReportRepository extends JpaRepository<PropDamageReport, Long> {
    Optional<PropDamageReport> findFirstByPropIdAndStatus(Long propId, String status);
    List<PropDamageReport> findAllByOrderByCreatedAtDesc();
}
