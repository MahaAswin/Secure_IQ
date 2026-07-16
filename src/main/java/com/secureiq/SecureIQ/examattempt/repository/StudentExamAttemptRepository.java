package com.secureiq.SecureIQ.examattempt.repository;

import com.secureiq.SecureIQ.examattempt.model.AttemptStatus;
import com.secureiq.SecureIQ.examattempt.model.StudentExamAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentExamAttemptRepository extends JpaRepository<StudentExamAttempt, Long> {

    boolean existsByStudentIdAndExamSessionId(Long studentId, Long sessionId);

    Optional<StudentExamAttempt> findByStudentIdAndExamSessionId(Long studentId, Long sessionId);

    Optional<StudentExamAttempt> findFirstByStudentIdAndStatus(Long studentId, AttemptStatus status);

    List<StudentExamAttempt> findAllByStudentId(Long studentId);

    List<StudentExamAttempt> findAllByExamSessionId(Long sessionId);

    @Query("SELECT sea FROM StudentExamAttempt sea WHERE " +
           "(:attemptCode IS NULL OR :attemptCode = '' OR LOWER(sea.attemptCode) LIKE LOWER(CONCAT('%', :attemptCode, '%'))) AND " +
           "(:studentId IS NULL OR sea.student.id = :studentId) AND " +
           "(:examSessionId IS NULL OR sea.examSession.id = :examSessionId) AND " +
           "(:departmentId IS NULL OR sea.examSession.exam.department.id = :departmentId) AND " +
           "(:status IS NULL OR sea.status = :status)")
    Page<StudentExamAttempt> findAllFiltered(
        @Param("attemptCode") String attemptCode,
        @Param("studentId") Long studentId,
        @Param("examSessionId") Long examSessionId,
        @Param("departmentId") Long departmentId,
        @Param("status") AttemptStatus status,
        Pageable pageable
    );

    @Query("SELECT sea FROM StudentExamAttempt sea WHERE " +
           "sea.examSession.faculty.id = :facultyId AND " +
           "(:attemptCode IS NULL OR :attemptCode = '' OR LOWER(sea.attemptCode) LIKE LOWER(CONCAT('%', :attemptCode, '%'))) AND " +
           "(:studentId IS NULL OR sea.student.id = :studentId) AND " +
           "(:examSessionId IS NULL OR sea.examSession.id = :examSessionId) AND " +
           "(:departmentId IS NULL OR sea.examSession.exam.department.id = :departmentId) AND " +
           "(:status IS NULL OR sea.status = :status)")
    Page<StudentExamAttempt> findAllFilteredForFaculty(
        @Param("facultyId") Long facultyId,
        @Param("attemptCode") String attemptCode,
        @Param("studentId") Long studentId,
        @Param("examSessionId") Long examSessionId,
        @Param("departmentId") Long departmentId,
        @Param("status") AttemptStatus status,
        Pageable pageable
    );

    @Query("SELECT sea FROM StudentExamAttempt sea WHERE " +
           "sea.student.id = :studentId AND " +
           "(:attemptCode IS NULL OR :attemptCode = '' OR LOWER(sea.attemptCode) LIKE LOWER(CONCAT('%', :attemptCode, '%'))) AND " +
           "(:examSessionId IS NULL OR sea.examSession.id = :examSessionId) AND " +
           "(:status IS NULL OR sea.status = :status)")
    Page<StudentExamAttempt> findAllFilteredForStudent(
        @Param("studentId") Long studentId,
        @Param("attemptCode") String attemptCode,
        @Param("examSessionId") Long examSessionId,
        @Param("status") AttemptStatus status,
        Pageable pageable
    );

    // Dashboard metrics
    long countByStatus(AttemptStatus status);

    @Query("SELECT COUNT(sea) FROM StudentExamAttempt sea WHERE sea.examSession.faculty.id = :facultyId AND sea.status = :status")
    long countByFacultyIdAndStatus(@Param("facultyId") Long facultyId, @Param("status") AttemptStatus status);

    @Query("SELECT COUNT(sea) FROM StudentExamAttempt sea WHERE sea.examSession.faculty.id = :facultyId AND sea.status IN ('SUBMITTED', 'AUTO_SUBMITTED')")
    long countSubmittedByFacultyId(@Param("facultyId") Long facultyId);
}
