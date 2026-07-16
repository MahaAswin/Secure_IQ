package com.secureiq.SecureIQ.question.repository;

import com.secureiq.SecureIQ.question.model.Difficulty;
import com.secureiq.SecureIQ.question.model.Question;
import com.secureiq.SecureIQ.question.model.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("SELECT q FROM Question q WHERE " +
           "(:bankId IS NULL OR q.bank.id = :bankId) AND " +
           "(:subjectId IS NULL OR q.bank.subject.id = :subjectId) AND " +
           "(:departmentId IS NULL OR q.bank.department.id = :departmentId) AND " +
           "(:difficulty IS NULL OR q.difficulty = :difficulty) AND " +
           "(:questionType IS NULL OR q.questionType = :questionType) AND " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(q.questionText) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(q.explanation) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Question> findAllFiltered(
        @Param("bankId") Long bankId,
        @Param("subjectId") Long subjectId,
        @Param("departmentId") Long departmentId,
        @Param("difficulty") Difficulty difficulty,
        @Param("questionType") QuestionType questionType,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Query("SELECT q FROM Question q JOIN q.bank.subject.faculty f WHERE " +
           "f.id = :facultyId AND " +
           "(:bankId IS NULL OR q.bank.id = :bankId) AND " +
           "(:subjectId IS NULL OR q.bank.subject.id = :subjectId) AND " +
           "(:departmentId IS NULL OR q.bank.department.id = :departmentId) AND " +
           "(:difficulty IS NULL OR q.difficulty = :difficulty) AND " +
           "(:questionType IS NULL OR q.questionType = :questionType) AND " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(q.questionText) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(q.explanation) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Question> findAllFilteredForFaculty(
        @Param("facultyId") Long facultyId,
        @Param("bankId") Long bankId,
        @Param("subjectId") Long subjectId,
        @Param("departmentId") Long departmentId,
        @Param("difficulty") Difficulty difficulty,
        @Param("questionType") QuestionType questionType,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    // General counts
    @Query("SELECT COUNT(q) FROM Question q")
    long countAllQuestions();

    @Query("SELECT q.bank.subject.subjectCode, COUNT(q) FROM Question q GROUP BY q.bank.subject.subjectCode")
    List<Object[]> countQuestionsBySubject();

    @Query("SELECT q.difficulty, COUNT(q) FROM Question q GROUP BY q.difficulty")
    List<Object[]> countQuestionsByDifficulty();

    // HOD counts (filtered by department)
    @Query("SELECT COUNT(q) FROM Question q WHERE q.bank.department.id = :departmentId")
    long countQuestionsByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT q.bank.subject.subjectCode, COUNT(q) FROM Question q WHERE q.bank.department.id = :departmentId GROUP BY q.bank.subject.subjectCode")
    List<Object[]> countQuestionsBySubjectForDepartment(@Param("departmentId") Long departmentId);

    @Query("SELECT q.difficulty, COUNT(q) FROM Question q WHERE q.bank.department.id = :departmentId GROUP BY q.difficulty")
    List<Object[]> countQuestionsByDifficultyForDepartment(@Param("departmentId") Long departmentId);

    // Faculty counts (filtered by assigned subjects)
    @Query("SELECT COUNT(q) FROM Question q JOIN q.bank.subject.faculty f WHERE f.id = :facultyId")
    long countQuestionsByFacultyId(@Param("facultyId") Long facultyId);

    @Query("SELECT q.bank.subject.subjectCode, COUNT(q) FROM Question q JOIN q.bank.subject.faculty f WHERE f.id = :facultyId GROUP BY q.bank.subject.subjectCode")
    List<Object[]> countQuestionsBySubjectForFaculty(@Param("facultyId") Long facultyId);

    @Query("SELECT q.difficulty, COUNT(q) FROM Question q JOIN q.bank.subject.faculty f WHERE f.id = :facultyId GROUP BY q.difficulty")
    List<Object[]> countQuestionsByDifficultyForFaculty(@Param("facultyId") Long facultyId);
}
