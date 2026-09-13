package com.example.scriptkill.repository;

import com.example.scriptkill.entity.Prop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropRepository extends JpaRepository<Prop, Long> {
    Optional<Prop> findByPropCode(String propCode);
    List<Prop> findByEra(String era);
    List<Prop> findByPropType(String propType);
    List<Prop> findByStatus(String status);
}