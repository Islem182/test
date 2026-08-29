package com.dgi.platform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "fiscal_numbers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiscalNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taxpayer_id", nullable = false, unique = true)
    private Taxpayer taxpayer;

    @Column(name = "fiscal_number", nullable = false, unique = true, length = 50)
    private String fiscalNumber;

    @CreationTimestamp
    @Column(name = "generated_date", updatable = false)
    private LocalDateTime generatedDate;
}
