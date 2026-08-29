package com.dgi.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private TaxApplication application;

    @Column(name = "business_name", nullable = false, length = 150)
    private String businessName;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Column(length = 100)
    private String sector;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "start_date")
    private LocalDate startDate;
}
