package com.secureiq.SecureIQ.student.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.secureiq.SecureIQ.department.model.Department;

@Entity
@Table(name = "exams")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
}
