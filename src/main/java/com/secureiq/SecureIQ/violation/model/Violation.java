package com.secureiq.SecureIQ.violation.model;

import com.secureiq.SecureIQ.common.entity.BaseEntity;
import com.secureiq.SecureIQ.examattempt.model.StudentExamAttempt;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "violations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"violation_code"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE violations SET deleted = true, violation_code = concat(violation_code, '_deleted_', cast(extract(epoch from now()) as varchar)) WHERE id = ?")
@SQLRestriction("deleted = false")
public class Violation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "violation_code", nullable = false)
    private String violationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_exam_attempt_id", nullable = false)
    private StudentExamAttempt studentExamAttempt;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type", nullable = false)
    private ViolationType violationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "evidence_path")
    private String evidencePath;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "action_taken")
    private String actionTaken;

    @Column(nullable = false)
    @Builder.Default
    private boolean resolved = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;
}
