package com.melodical.backend.repository;

import com.melodical.backend.entity.Musical;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MusicalRepository extends JpaRepository<Musical, Long> {
}
