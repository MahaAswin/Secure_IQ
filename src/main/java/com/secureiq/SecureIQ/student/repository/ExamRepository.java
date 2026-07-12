package com.secureiq.SecureIQ.student.repository;

import com.secureiq.SecureIQ.student.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    long countByDepartmentIdAndScheduledAtAfter(Long departmentId, LocalDateTime time);
    long countByDepartmentIdAndScheduledAtBefore(Long departmentId, LocalDateTime time);
}
