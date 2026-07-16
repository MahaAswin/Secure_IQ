package com.secureiq.SecureIQ.violation.repository;

import com.secureiq.SecureIQ.examsession.model.SessionStatus;
import com.secureiq.SecureIQ.violation.model.Severity;
import com.secureiq.SecureIQ.violation.model.Source;
import com.secureiq.SecureIQ.violation.model.Violation;
import com.secureiq.SecureIQ.violation.model.ViolationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViolationRepository extends JpaRepository<Violation, Long> {

    boolean existsByViolationCode(String violationCode);

    @Query("SELECT v FROM Violation v WHERE " +
           "(:violationCode IS NULL OR :violationCode = '' OR LOWER(v.violationCode) LIKE LOWER(CONCAT('%', :violationCode, '%'))) AND " +
           "(:studentId IS NULL OR v.studentExamAttempt.student.id = :studentId) AND " +
           "(:sessionId IS NULL OR v.studentExamAttempt.examSession.id = :sessionId) AND " +
           "(:severity IS NULL OR v.severity = :severity) AND " +
           "(:type IS NULL OR v.violationType = :type) AND " +
           "(:source IS NULL OR v.source = :source)")
    Page<Violation> findAllFiltered(
        @Param("violationCode") String violationCode,
        @Param("studentId") Long studentId,
        @Param("sessionId") Long sessionId,
        @Param("severity") Severity severity,
        @Param("type") ViolationType type,
        @Param("source") Source source,
        Pageable pageable
    );

    @Query("SELECT v FROM Violation v WHERE " +
           "v.studentExamAttempt.examSession.faculty.id = :facultyId AND " +
           "(:violationCode IS NULL OR :violationCode = '' OR LOWER(v.violationCode) LIKE LOWER(CONCAT('%', :violationCode, '%'))) AND " +
           "(:studentId IS NULL OR v.studentExamAttempt.student.id = :studentId) AND " +
           "(:sessionId IS NULL OR v.studentExamAttempt.examSession.id = :sessionId) AND " +
           "(:severity IS NULL OR v.severity = :severity) AND " +
           "(:type IS NULL OR v.violationType = :type) AND " +
           "(:source IS NULL OR v.source = :source)")
    Page<Violation> findAllFilteredForFaculty(
        @Param("facultyId") Long facultyId,
        @Param("violationCode") String violationCode,
        @Param("studentId") Long studentId,
        @Param("sessionId") Long sessionId,
        @Param("severity") Severity severity,
        @Param("type") ViolationType type,
        @Param("source") Source source,
        Pageable pageable
    );

    @Query("SELECT v FROM Violation v WHERE " +
           "v.studentExamAttempt.student.id = :studentId AND " +
           "(:violationCode IS NULL OR :violationCode = '' OR LOWER(v.violationCode) LIKE LOWER(CONCAT('%', :violationCode, '%'))) AND " +
           "(:sessionId IS NULL OR v.studentExamAttempt.examSession.id = :sessionId) AND " +
           "(:severity IS NULL OR v.severity = :severity) AND " +
           "(:type IS NULL OR v.violationType = :type) AND " +
           "(:source IS NULL OR v.source = :source)")
    Page<Violation> findAllFilteredForStudent(
        @Param("studentId") Long studentId,
        @Param("violationCode") String violationCode,
        @Param("sessionId") Long sessionId,
        @Param("severity") Severity severity,
        @Param("type") ViolationType type,
        @Param("source") Source source,
        Pageable pageable
    );

    // Dashboard count aggregations
    long countByStudentExamAttemptStudentId(Long studentId);

    @Query("SELECT COUNT(v) FROM Violation v WHERE v.studentExamAttempt.examSession.status = 'LIVE'")
    long countLiveViolations();

    @Query("SELECT v FROM Violation v WHERE v.studentExamAttempt.examSession.status = :status")
    Page<Violation> findAllBySessionStatus(@Param("status") SessionStatus status, Pageable pageable);

    @Query("SELECT v FROM Violation v WHERE v.studentExamAttempt.examSession.faculty.id = :facultyId AND v.studentExamAttempt.examSession.status = :status")
    Page<Violation> findAllByFacultyIdAndSessionStatus(@Param("facultyId") Long facultyId, @Param("status") SessionStatus status, Pageable pageable);

    @Query("SELECT v FROM Violation v WHERE v.studentExamAttempt.examSession.exam.department.id = :departmentId AND v.studentExamAttempt.examSession.status = :status")
    Page<Violation> findAllByDepartmentIdAndSessionStatus(@Param("departmentId") Long departmentId, @Param("status") SessionStatus status, Pageable pageable);

    @Query("SELECT COUNT(v) FROM Violation v WHERE v.studentExamAttempt.examSession.faculty.id = :facultyId AND v.studentExamAttempt.examSession.status = :status")
    long countByFacultyIdAndSessionStatus(@Param("facultyId") Long facultyId, @Param("status") SessionStatus status);

    @Query("SELECT COUNT(v) FROM Violation v WHERE v.studentExamAttempt.examSession.faculty.id = :facultyId AND v.severity = :severity")
    long countByFacultyIdAndSeverity(@Param("facultyId") Long facultyId, @Param("severity") Severity severity);

    @Query("SELECT v.severity, COUNT(v) FROM Violation v GROUP BY v.severity")
    List<Object[]> countBySeverityGroup();

    @Query("SELECT v.violationType, COUNT(v) FROM Violation v GROUP BY v.violationType")
    List<Object[]> countByViolationTypeGroup();

    @Query("SELECT CAST(v.detectedAt AS date), COUNT(v) FROM Violation v GROUP BY CAST(v.detectedAt AS date) ORDER BY CAST(v.detectedAt AS date) ASC")
    List<Object[]> countByDayGroup();
}
