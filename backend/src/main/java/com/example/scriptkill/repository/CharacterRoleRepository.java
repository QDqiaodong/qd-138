package com.example.scriptkill.repository;

import com.example.scriptkill.entity.CharacterRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterRoleRepository extends JpaRepository<CharacterRole, Long> {
    List<CharacterRole> findByScriptThemeId(Long scriptThemeId);
    Optional<CharacterRole> findByRoleNameAndScriptThemeId(String roleName, Long scriptThemeId);
    List<CharacterRole> findByRoleNameContaining(String roleName);
}