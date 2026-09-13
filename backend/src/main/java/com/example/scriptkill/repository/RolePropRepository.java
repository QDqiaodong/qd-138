package com.example.scriptkill.repository;

import com.example.scriptkill.entity.RoleProp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePropRepository extends JpaRepository<RoleProp, Long> {
    List<RoleProp> findByCharacterRoleId(Long characterRoleId);
    List<RoleProp> findByPropId(Long propId);
    List<RoleProp> findByScriptThemeId(Long scriptThemeId);
    List<RoleProp> findByCharacterRoleIdAndScriptThemeId(Long characterRoleId, Long scriptThemeId);
    
    @Query("SELECT rp FROM RoleProp rp WHERE rp.propId = :propId AND rp.scriptThemeId = :scriptThemeId")
    List<RoleProp> findByPropIdAndScriptThemeId(@Param("propId") Long propId, @Param("scriptThemeId") Long scriptThemeId);
    
    void deleteByCharacterRoleId(Long characterRoleId);
    void deleteByPropId(Long propId);
    void deleteByScriptThemeId(Long scriptThemeId);
}