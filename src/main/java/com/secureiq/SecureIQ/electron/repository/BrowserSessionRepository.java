package com.secureiq.SecureIQ.electron.repository;

import com.secureiq.SecureIQ.electron.model.BrowserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BrowserSessionRepository extends JpaRepository<BrowserSession, Long> {

    Optional<BrowserSession> findBySessionId(String sessionId);

    List<BrowserSession> findAllByStudentExamAttemptIdAndActiveTrue(Long attemptId);

    @Query("SELECT COUNT(bs) FROM BrowserSession bs WHERE bs.active = true AND bs.updatedAt >= :threshold")
    long countActiveSessions(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT bs.browserVersion, COUNT(bs) FROM BrowserSession bs GROUP BY bs.browserVersion")
    List<Object[]> countByBrowserVersionGroup();

    @Query("SELECT bs.operatingSystem, COUNT(bs) FROM BrowserSession bs GROUP BY bs.operatingSystem")
    List<Object[]> countByOperatingSystemGroup();

    @Query("SELECT bs FROM BrowserSession bs WHERE bs.studentExamAttempt.examSession.faculty.id = :facultyId AND bs.active = true AND bs.updatedAt >= :threshold AND bs.studentExamAttempt.status = 'IN_PROGRESS'")
    List<BrowserSession> findActiveSessionsByFaculty(@Param("facultyId") Long facultyId, @Param("threshold") LocalDateTime threshold);

    @Query("SELECT COUNT(DISTINCT bs.studentExamAttempt.id) FROM BrowserSession bs WHERE bs.studentExamAttempt.examSession.faculty.id = :facultyId AND bs.active = true AND bs.updatedAt >= :threshold AND bs.studentExamAttempt.status = 'IN_PROGRESS'")
    long countConnectedStudentsByFaculty(@Param("facultyId") Long facultyId, @Param("threshold") LocalDateTime threshold);
}
