package com.secureiq.SecureIQ.exam.model;

import com.secureiq.SecureIQ.common.entity.BaseEntity;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.faculty.model.Faculty;
import com.secureiq.SecureIQ.subject.model.Subject;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Entity
@Table(name = "exams")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE exams SET deleted = true, exam_code = concat(exam_code, '_deleted_', cast(extract(epoch from now()) as varchar)) WHERE id = ?")
@SQLRestriction("deleted = false")
public class Exam extends BaseEntity {

    public static final ZoneId INSTITUTION_ZONE = ZoneId.of("Asia/Kolkata");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_code", nullable = false, unique = true)
    private String examCode;

    @Column(name = "exam_title", nullable = false)
    private String examTitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false)
    private Integer semester;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false)
    private ExamType examType;

    @Column(name = "total_marks", nullable = false)
    private Integer totalMarks;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "passing_marks", nullable = false)
    private Integer passingMarks;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "scheduled_end_date")
    private LocalDate scheduledEndDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "scheduled_start_instant")
    private Instant scheduledStartInstant;

    @Column(name = "scheduled_end_instant")
    private Instant scheduledEndInstant;

    private String venue;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ExamStatus status = ExamStatus.DRAFT;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @PrePersist
    @PreUpdate
    public void syncScheduleInstants() {
        if (this.scheduledEndDate == null) {
            this.scheduledEndDate = this.scheduledDate;
        }
        if (this.scheduledDate != null && this.startTime != null) {
            this.scheduledStartInstant = this.scheduledDate.atTime(this.startTime).atZone(INSTITUTION_ZONE).toInstant();
        }
        LocalDate effectiveEnd = this.scheduledEndDate != null ? this.scheduledEndDate : this.scheduledDate;
        if (effectiveEnd != null && this.endTime != null) {
            this.scheduledEndInstant = effectiveEnd.atTime(this.endTime).atZone(INSTITUTION_ZONE).toInstant();
        }
    }

    public Instant getAuthoritativeStartInstant() {
        if (this.scheduledStartInstant != null) {
            return this.scheduledStartInstant;
        }
        if (this.scheduledDate != null && this.startTime != null) {
            return this.scheduledDate.atTime(this.startTime).atZone(INSTITUTION_ZONE).toInstant();
        }
        return null;
    }

    public Instant getAuthoritativeEndInstant() {
        if (this.scheduledEndInstant != null) {
            return this.scheduledEndInstant;
        }
        LocalDate effectiveEnd = this.scheduledEndDate != null ? this.scheduledEndDate : this.scheduledDate;
        if (effectiveEnd != null && this.endTime != null) {
            return effectiveEnd.atTime(this.endTime).atZone(INSTITUTION_ZONE).toInstant();
        }
        return null;
    }
}
