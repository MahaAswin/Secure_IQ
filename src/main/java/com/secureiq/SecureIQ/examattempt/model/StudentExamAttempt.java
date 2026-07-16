package com.secureiq.SecureIQ.examattempt.model;

import com.secureiq.SecureIQ.common.entity.BaseEntity;
import com.secureiq.SecureIQ.examsession.model.ExamSession;
import com.secureiq.SecureIQ.student.model.Student;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_exam_attempts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"attempt_code"}),
    @UniqueConstraint(columnNames = {"student_id", "exam_session_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE student_exam_attempts SET deleted = true, attempt_code = concat(attempt_code, '_deleted_', cast(extract(epoch from now()) as varchar)) WHERE id = ?")
@SQLRestriction("deleted = false")
public class StudentExamAttempt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_code", nullable = false)
    private String attemptCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_session_id", nullable = false)
    private ExamSession examSession;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "submitted_time")
    private LocalDateTime submittedTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptStatus status;

    @Column(nullable = false)
    @Builder.Default
    private Double score = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double percentage = 0.0;

    @Column(name = "total_marks", nullable = false)
    private Integer totalMarks;

    @Column(name = "obtained_marks", nullable = false)
    @Builder.Default
    private Double obtainedMarks = 0.0;

    @Column(name = "auto_submitted", nullable = false)
    @Builder.Default
    private boolean autoSubmitted = false;

    @Column(name = "browser_warnings", nullable = false)
    @Builder.Default
    private Integer browserWarnings = 0;

    @Column(name = "ai_warnings", nullable = false)
    @Builder.Default
    private Integer aiWarnings = 0;

    @Column(name = "total_violations", nullable = false)
    @Builder.Default
    private Integer totalViolations = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;
}
