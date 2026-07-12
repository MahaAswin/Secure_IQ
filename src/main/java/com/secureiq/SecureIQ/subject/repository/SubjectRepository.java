package com.secureiq.SecureIQ.subject.repository;

import com.secureiq.SecureIQ.subject.model.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findBySubjectCode(String subjectCode);
    boolean existsBySubjectCode(String subjectCode);

    @Query("SELECT s FROM Subject s WHERE " +
           "(:subjectName IS NULL OR :subjectName = '' OR LOWER(s.subjectName) LIKE LOWER(CONCAT('%', :subjectName, '%'))) AND " +
           "(:subjectCode IS NULL OR :subjectCode = '' OR LOWER(s.subjectCode) LIKE LOWER(CONCAT('%', :subjectCode, '%'))) AND " +
           "(:semester IS NULL OR s.semester = :semester) AND " +
           "(:departmentId IS NULL OR s.department.id = :departmentId)")
    Page<Subject> findAllFiltered(
        @Param("subjectName") String subjectName,
        @Param("subjectCode") String subjectCode,
        @Param("semester") Integer semester,
        @Param("departmentId") Long departmentId,
        Pageable pageable
    );

    @Query("SELECT s FROM Subject s JOIN s.faculty f WHERE " +
           "f.id = :facultyId AND " +
           "(:subjectName IS NULL OR :subjectName = '' OR LOWER(s.subjectName) LIKE LOWER(CONCAT('%', :subjectName, '%'))) AND " +
           "(:subjectCode IS NULL OR :subjectCode = '' OR LOWER(s.subjectCode) LIKE LOWER(CONCAT('%', :subjectCode, '%'))) AND " +
           "(:semester IS NULL OR s.semester = :semester) AND " +
           "(:departmentId IS NULL OR s.department.id = :departmentId)")
    Page<Subject> findAllFilteredForFaculty(
        @Param("facultyId") Long facultyId,
        @Param("subjectName") String subjectName,
        @Param("subjectCode") String subjectCode,
        @Param("semester") Integer semester,
        @Param("departmentId") Long departmentId,
        Pageable pageable
    );

    @Query("SELECT s FROM Subject s JOIN s.faculty f WHERE f.id = :facultyId")
    List<Subject> findAllByFacultyId(@Param("facultyId") Long facultyId);

    List<Subject> findAllByDepartmentIdAndSemester(Long departmentId, Integer semester);

    long countByDepartmentId(Long departmentId);
}
