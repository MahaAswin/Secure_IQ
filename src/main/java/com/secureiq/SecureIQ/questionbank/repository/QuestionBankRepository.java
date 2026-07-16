package com.secureiq.SecureIQ.questionbank.repository;

import com.secureiq.SecureIQ.questionbank.model.QuestionBank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long> {

    boolean existsByBankNameAndSubjectId(String bankName, Long subjectId);

    boolean existsByBankNameAndSubjectIdAndIdNot(String bankName, Long subjectId, Long id);

    @Query("SELECT qb FROM QuestionBank qb WHERE " +
           "(:bankName IS NULL OR :bankName = '' OR LOWER(qb.bankName) LIKE LOWER(CONCAT('%', :bankName, '%'))) AND " +
           "(:subjectId IS NULL OR qb.subject.id = :subjectId) AND " +
           "(:departmentId IS NULL OR qb.department.id = :departmentId)")
    Page<QuestionBank> findAllFiltered(
        @Param("bankName") String bankName,
        @Param("subjectId") Long subjectId,
        @Param("departmentId") Long departmentId,
        Pageable pageable
    );

    @Query("SELECT qb FROM QuestionBank qb JOIN qb.subject.faculty f WHERE " +
           "f.id = :facultyId AND " +
           "(:bankName IS NULL OR :bankName = '' OR LOWER(qb.bankName) LIKE LOWER(CONCAT('%', :bankName, '%'))) AND " +
           "(:subjectId IS NULL OR qb.subject.id = :subjectId) AND " +
           "(:departmentId IS NULL OR qb.department.id = :departmentId)")
    Page<QuestionBank> findAllFilteredForFaculty(
        @Param("facultyId") Long facultyId,
        @Param("bankName") String bankName,
        @Param("subjectId") Long subjectId,
        @Param("departmentId") Long departmentId,
        Pageable pageable
    );

    @Query("SELECT qb FROM QuestionBank qb JOIN qb.subject.faculty f WHERE f.id = :facultyId")
    List<QuestionBank> findAllByFacultyId(@Param("facultyId") Long facultyId);

    List<QuestionBank> findAllByDepartmentId(Long departmentId);
}
