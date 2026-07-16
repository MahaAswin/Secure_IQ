package com.secureiq.SecureIQ.examsession.model;

import com.secureiq.SecureIQ.common.entity.BaseEntity;
import com.secureiq.SecureIQ.exam.model.Exam;
import com.secureiq.SecureIQ.faculty.model.Faculty;
import com.secureiq.SecureIQ.student.model.Student;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "exam_sessions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"session_code"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE exam_sessions SET deleted = true, session_code = concat(session_code, '_deleted_', cast(extract(epoch from now()) as varchar)) WHERE id = ?")
@SQLRestriction("deleted = false")
public class ExamSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_code", nullable = false)
    private String sessionCode;

    @Column(name = "session_name", nullable = false)
    private String sessionName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private LocalDateTime endDateTime;

    @Column(nullable = false)
    private String venue;

    @Column(name = "total_students", nullable = false)
    @Builder.Default
    private Integer totalStudents = 0;

    @Column(name = "joined_students", nullable = false)
    @Builder.Default
    private Integer joinedStudents = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "session_students",
        joinColumns = @JoinColumn(name = "session_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    @Builder.Default
    private Set<Student> students = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;
}
