package com.secureiq.SecureIQ.student.repository;

import com.secureiq.SecureIQ.student.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    long countByStudentIdAndReadFalse(Long studentId);
}
