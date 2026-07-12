package com.secureiq.SecureIQ.student.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activities")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
