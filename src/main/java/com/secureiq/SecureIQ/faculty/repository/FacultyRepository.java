package com.secureiq.SecureIQ.faculty.repository;

import com.secureiq.SecureIQ.faculty.model.Faculty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    Optional<Faculty> findByUserId(Long userId);
    boolean existsByEmployeeId(String employeeId);
    long countByDepartmentId(Long departmentId);

    @Query("SELECT f FROM Faculty f LEFT JOIN f.user u WHERE " +
           "(:name IS NULL OR :name = '' OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:employeeId IS NULL OR :employeeId = '' OR LOWER(f.employeeId) LIKE LOWER(CONCAT('%', :employeeId, '%'))) AND " +
           "(:departmentId IS NULL OR f.department.id = :departmentId) AND " +
           "(:specialization IS NULL OR :specialization = '' OR LOWER(f.specialization) LIKE LOWER(CONCAT('%', :specialization, '%')))")
    Page<Faculty> findAllFiltered(
        @Param("name") String name,
        @Param("employeeId") String employeeId,
        @Param("departmentId") Long departmentId,
        @Param("specialization") String specialization,
        Pageable pageable
    );
}
