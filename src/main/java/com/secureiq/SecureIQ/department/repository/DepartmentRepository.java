package com.secureiq.SecureIQ.department.repository;

import com.secureiq.SecureIQ.department.model.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByHodId(Long hodId);
    Optional<Department> findByDepartmentCode(String departmentCode);
    boolean existsByDepartmentCode(String departmentCode);
    boolean existsByDepartmentName(String departmentName);

    @Query("SELECT d FROM Department d WHERE " +
           "(:departmentName IS NULL OR :departmentName = '' OR LOWER(d.departmentName) LIKE LOWER(CONCAT('%', :departmentName, '%'))) AND " +
           "(:departmentCode IS NULL OR :departmentCode = '' OR LOWER(d.departmentCode) LIKE LOWER(CONCAT('%', :departmentCode, '%')))")
    Page<Department> findAllFiltered(
        @Param("departmentName") String departmentName,
        @Param("departmentCode") String departmentCode,
        Pageable pageable
    );
}
