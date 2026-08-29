package com.dgi.platform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private TaxApplication application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentType type;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @CreationTimestamp
    @Column(name = "upload_date", updatable = false)
    private LocalDateTime uploadDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 30)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;
}
