package com.melodical.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "performance") // MySQL의 'performance' 테이블과 연결
class Performance(
    @Id
    val id: String, // KOPIS ID (예: PF12345)

    val title: String,

    @Column(name = "start_date") // DB의 start_date 컬럼
    val startDate: LocalDate?,

    @Column(name = "end_date") // DB의 end_date 컬럼
    val endDate: LocalDate?,

    @Column(name = "poster_url", length = 1024) // DB의 poster_url 컬럼
    val posterUrl: String?,

    val genre: String?
)
