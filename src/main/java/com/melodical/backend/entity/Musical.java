package com.melodical.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "musical")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Musical {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cast;
    private String endDate;
    private String posterUrl;
    private String runtime;
    private String startDate;
    private String theater;
    private String title;
}
