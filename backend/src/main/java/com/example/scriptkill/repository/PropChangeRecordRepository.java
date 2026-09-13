package com.example.scriptkill.repository;

import com.example.scriptkill.entity.PropChangeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropChangeRecordRepository extends JpaRepository<PropChangeRecord, Long> {
    List<PropChangeRecord> findByPropIdOrderByCreatedAtDesc(Long propId);
    List<PropChangeRecord> findByCharacterRoleIdOrderByCreatedAtDesc(Long characterRoleId);
    List<PropChangeRecord> findByScriptThemeIdOrderByCreatedAtDesc(Long scriptThemeId);
}