package com.example.scriptkill.repository;

import com.example.scriptkill.entity.ScriptTheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScriptThemeRepository extends JpaRepository<ScriptTheme, Long> {
    List<ScriptTheme> findByEra(String era);
    List<ScriptTheme> findByDifficulty(String difficulty);
}