package com.melodical.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "musical")
public class Musical {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String title;

    private String theater;
    private String startDate;
    private String endDate;
    private String cast;
    private String runtime;
    private String posterUrl;
}