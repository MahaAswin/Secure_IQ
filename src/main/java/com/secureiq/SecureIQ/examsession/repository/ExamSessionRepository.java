package com.secureiq.SecureIQ.examsession.repository;

import com.secureiq.SecureIQ.examsession.model.ExamSession;
import com.secureiq.SecureIQ.examsession.model.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {

    boolean existsBySessionCode(String sessionCode);

    boolean existsBySessionCodeAndIdNot(String sessionCode, Long id);

    @Query("SELECT COUNT(es) > 0 FROM ExamSession es WHERE " +
           "es.venue = :venue AND es.id <> :id AND es.status <> 'CANCELLED' AND " +
           "es.startDateTime < :endDateTime AND es.endDateTime > :startDateTime")
    boolean existsOverlap(@Param("venue") String venue,
                          @Param("startDateTime") LocalDateTime start,
                          @Param("endDateTime") LocalDateTime end,
                          @Param("id") Long id);

    @Query("SELECT es FROM ExamSession es WHERE " +
           "(:sessionCode IS NULL OR :sessionCode = '' OR LOWER(es.sessionCode) LIKE LOWER(CONCAT('%', :sessionCode, '%'))) AND " +
           "(:examId IS NULL OR es.exam.id = :examId) AND " +
           "(:facultyId IS NULL OR es.faculty.id = :facultyId) AND " +
           "(:departmentId IS NULL OR es.exam.department.id = :departmentId) AND " +
           "(:status IS NULL OR es.status = :status)")
    Page<ExamSession> findAllFiltered(
        @Param("sessionCode") String sessionCode,
        @Param("examId") Long examId,
        @Param("facultyId") Long facultyId,
        @Param("departmentId") Long departmentId,
        @Param("status") SessionStatus status,
        Pageable pageable
    );

    @Query("SELECT es FROM ExamSession es WHERE " +
           "es.faculty.id = :facultyId AND " +
           "(:sessionCode IS NULL OR :sessionCode = '' OR LOWER(es.sessionCode) LIKE LOWER(CONCAT('%', :sessionCode, '%'))) AND " +
           "(:examId IS NULL OR es.exam.id = :examId) AND " +
           "(:departmentId IS NULL OR es.exam.department.id = :departmentId) AND " +
           "(:status IS NULL OR es.status = :status)")
    Page<ExamSession> findAllFilteredForFaculty(
        @Param("facultyId") Long facultyId,
        @Param("sessionCode") String sessionCode,
        @Param("examId") Long examId,
        @Param("departmentId") Long departmentId,
        @Param("status") SessionStatus status,
        Pageable pageable
    );

    @Query("SELECT es FROM ExamSession es JOIN es.students s WHERE " +
           "s.id = :studentId AND " +
           "(:sessionCode IS NULL OR :sessionCode = '' OR LOWER(es.sessionCode) LIKE LOWER(CONCAT('%', :sessionCode, '%'))) AND " +
           "(:examId IS NULL OR es.exam.id = :examId) AND " +
           "(:facultyId IS NULL OR es.faculty.id = :facultyId) AND " +
           "(:departmentId IS NULL OR es.exam.department.id = :departmentId) AND " +
           "(:status IS NULL OR es.status = :status)")
    Page<ExamSession> findAllFilteredForStudent(
        @Param("studentId") Long studentId,
        @Param("sessionCode") String sessionCode,
        @Param("examId") Long examId,
        @Param("facultyId") Long facultyId,
        @Param("departmentId") Long departmentId,
        @Param("status") SessionStatus status,
        Pageable pageable
    );

    // List of Live sessions
    List<ExamSession> findAllByStatus(SessionStatus status);

    // List of Live sessions for Faculty
    List<ExamSession> findAllByFacultyIdAndStatus(Long facultyId, SessionStatus status);

    // List of Live sessions for Student
    @Query("SELECT es FROM ExamSession es JOIN es.students s WHERE s.id = :studentId AND es.status = :status")
    List<ExamSession> findAllByStudentIdAndStatus(@Param("studentId") Long studentId, @Param("status") SessionStatus status);

    // List of Upcoming sessions
    List<ExamSession> findAllByStatusAndStartDateTimeAfter(SessionStatus status, LocalDateTime now);

    // List of Upcoming sessions for Faculty
    List<ExamSession> findAllByFacultyIdAndStatusAndStartDateTimeAfter(Long facultyId, SessionStatus status, LocalDateTime now);

    // List of Upcoming sessions for Student
    @Query("SELECT es FROM ExamSession es JOIN es.students s WHERE s.id = :studentId AND es.status = :status AND es.startDateTime > :now")
    List<ExamSession> findAllByStudentIdAndStatusAndStartDateTimeAfter(@Param("studentId") Long studentId, @Param("status") SessionStatus status, @Param("now") LocalDateTime now);

    // HOD session counts & lists
    List<ExamSession> findAllByExamDepartmentId(Long departmentId);
    List<ExamSession> findAllByExamDepartmentIdAndStatus(Long departmentId, SessionStatus status);

    // Admin counts
    long countByStatus(SessionStatus status);

    // Attendance aggregation (returns [totalStudents, joinedStudents])
    @Query("SELECT COALESCE(SUM(es.totalStudents), 0), COALESCE(SUM(es.joinedStudents), 0) FROM ExamSession es")
    List<Object[]> getAttendanceSummary();
}
