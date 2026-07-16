package com.secureiq.SecureIQ.electron.model;

import com.secureiq.SecureIQ.common.entity.BaseEntity;
import com.secureiq.SecureIQ.examattempt.model.StudentExamAttempt;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "browser_sessions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"session_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE browser_sessions SET deleted = true, session_id = concat(session_id, '_deleted_', cast(extract(epoch from now()) as varchar)) WHERE id = ?")
@SQLRestriction("deleted = false")
public class BrowserSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_exam_attempt_id", nullable = false)
    private StudentExamAttempt studentExamAttempt;

    @Column(name = "browser_version")
    private String browserVersion;

    @Column(name = "operating_system")
    private String operatingSystem;

    @Column(name = "machine_id")
    private String machineId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;
}
