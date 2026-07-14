package com.secureiq.SecureIQ.exam.repository;

import com.secureiq.SecureIQ.exam.model.Exam;
import com.secureiq.SecureIQ.exam.model.ExamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    Optional<Exam> findByExamCode(String examCode);
    boolean existsByExamCode(String examCode);

    long countByStatus(ExamStatus status);
    long countByFacultyId(Long facultyId);
    
    // Legacy support counts for other dashboard modules
    long countByDepartmentIdAndScheduledDateAfter(Long departmentId, LocalDate date);
    long countByDepartmentIdAndScheduledDateBefore(Long departmentId, LocalDate date);

    @Query("SELECT e FROM Exam e WHERE " +
           "(:examTitle IS NULL OR :examTitle = '' OR LOWER(e.examTitle) LIKE LOWER(CONCAT('%', :examTitle, '%'))) AND " +
           "(:subjectId IS NULL OR e.subject.id = :subjectId) AND " +
           "(:facultyId IS NULL OR e.faculty.id = :facultyId) AND " +
           "(:departmentId IS NULL OR e.department.id = :departmentId) AND " +
           "(:status IS NULL OR e.status = :status)")
    Page<Exam> findAllFiltered(
        @Param("examTitle") String examTitle,
        @Param("subjectId") Long subjectId,
        @Param("facultyId") Long facultyId,
        @Param("departmentId") Long departmentId,
        @Param("status") ExamStatus status,
        Pageable pageable
    );

    @Query("SELECT e FROM Exam e WHERE e.department.id = :departmentId AND e.semester = :semester AND " +
           "(:examTitle IS NULL OR :examTitle = '' OR LOWER(e.examTitle) LIKE LOWER(CONCAT('%', :examTitle, '%'))) AND " +
           "(:subjectId IS NULL OR e.subject.id = :subjectId) AND " +
           "(:facultyId IS NULL OR e.faculty.id = :facultyId) AND " +
           "(:status IS NULL OR e.status = :status)")
    Page<Exam> findAllFilteredForStudent(
        @Param("departmentId") Long departmentId,
        @Param("semester") Integer semester,
        @Param("examTitle") String examTitle,
        @Param("subjectId") Long subjectId,
        @Param("facultyId") Long facultyId,
        @Param("status") ExamStatus status,
        Pageable pageable
    );

    @Query("SELECT e FROM Exam e WHERE e.faculty.id = :facultyId AND " +
           "(:examTitle IS NULL OR :examTitle = '' OR LOWER(e.examTitle) LIKE LOWER(CONCAT('%', :examTitle, '%'))) AND " +
           "(:subjectId IS NULL OR e.subject.id = :subjectId) AND " +
           "(:departmentId IS NULL OR e.department.id = :departmentId) AND " +
           "(:status IS NULL OR e.status = :status)")
    Page<Exam> findAllFilteredForFaculty(
        @Param("facultyId") Long facultyId,
        @Param("examTitle") String examTitle,
        @Param("subjectId") Long subjectId,
        @Param("departmentId") Long departmentId,
        @Param("status") ExamStatus status,
        Pageable pageable
    );

    // List upcoming exams
    List<Exam> findByDepartmentIdAndSemesterAndScheduledDateAfterOrderByScheduledDateAscStartTimeAsc(
        Long departmentId, 
        Integer semester, 
        LocalDate date
    );
    
    List<Exam> findByFacultyIdAndScheduledDateAfterOrderByScheduledDateAscStartTimeAsc(
        Long facultyId, 
        LocalDate date
    );

    List<Exam> findByScheduledDateAfterOrderByScheduledDateAscStartTimeAsc(
        LocalDate date
    );

    // List today's exams
    List<Exam> findByDepartmentIdAndSemesterAndScheduledDateOrderByStartTimeAsc(
        Long departmentId, 
        Integer semester, 
        LocalDate date
    );

    List<Exam> findByFacultyIdAndScheduledDateOrderByStartTimeAsc(
        Long facultyId, 
        LocalDate date
    );

    List<Exam> findByScheduledDateOrderByStartTimeAsc(
        LocalDate date
    );

    // List active exams
    List<Exam> findByStatus(ExamStatus status);

    // HOD support counts and queries
    long countByDepartmentIdAndStatus(Long departmentId, ExamStatus status);
    
    @Query("SELECT e FROM Exam e WHERE e.department.id = :departmentId AND " +
           "(:examTitle IS NULL OR :examTitle = '' OR LOWER(e.examTitle) LIKE LOWER(CONCAT('%', :examTitle, '%'))) AND " +
           "(:subjectId IS NULL OR e.subject.id = :subjectId) AND " +
           "(:facultyId IS NULL OR e.faculty.id = :facultyId) AND " +
           "(:status IS NULL OR e.status = :status)")
    Page<Exam> findAllFilteredForHOD(
        @Param("departmentId") Long departmentId,
        @Param("examTitle") String examTitle,
        @Param("subjectId") Long subjectId,
        @Param("facultyId") Long facultyId,
        @Param("status") ExamStatus status,
        Pageable pageable
    );

    // Legacy method for Faculty dashboard count compatibility
    List<Exam> findByDepartmentIdInAndScheduledDateAfterOrderByScheduledDateAsc(
        Collection<Long> departmentIds, 
        LocalDate date
    );
    
    List<Exam> findByDepartmentIdAndScheduledDateAfterOrderByScheduledDateAsc(
        Long departmentId, 
        LocalDate date
    );
}
