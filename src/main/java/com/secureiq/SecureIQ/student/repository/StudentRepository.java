package com.secureiq.SecureIQ.student.repository;

import com.secureiq.SecureIQ.student.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUserId(Long userId);
    boolean existsByRegisterNumber(String registerNumber);
    boolean existsByRollNumber(String rollNumber);
    long countByDepartmentId(Long departmentId);
    java.util.List<Student> findAllByDepartmentId(Long departmentId);
    java.util.List<Student> findByDepartmentIdInAndSemesterIn(java.util.Collection<Long> departmentIds, java.util.Collection<Integer> semesters);

    @Query("SELECT s FROM Student s LEFT JOIN s.user u WHERE " +
           "(:name IS NULL OR :name = '' OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:registerNumber IS NULL OR :registerNumber = '' OR LOWER(s.registerNumber) LIKE LOWER(CONCAT('%', :registerNumber, '%'))) AND " +
           "(:rollNumber IS NULL OR :rollNumber = '' OR LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', :rollNumber, '%'))) AND " +
           "(:departmentId IS NULL OR s.department.id = :departmentId)")
    Page<Student> findAllFiltered(
        @Param("name") String name,
        @Param("registerNumber") String registerNumber,
        @Param("rollNumber") String rollNumber,
        @Param("departmentId") Long departmentId,
        Pageable pageable
    );
}
