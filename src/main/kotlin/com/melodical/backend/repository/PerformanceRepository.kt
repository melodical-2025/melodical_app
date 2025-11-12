package com.melodical.backend.repository

import com.melodical.backend.domain.Performance
import org.springframework.data.jpa.repository.JpaRepository

// Performance 테이블에서 데이터를 꺼내올 수 있게 해주는 JpaRepository
interface PerformanceRepository : JpaRepository<Performance, String>
