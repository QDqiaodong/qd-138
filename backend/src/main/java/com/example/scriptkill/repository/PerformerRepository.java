package com.example.scriptkill.repository;

import com.example.scriptkill.entity.Performer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerformerRepository extends JpaRepository<Performer, Long> {
    Optional<Performer> findByPerformerName(String performerName);
}
