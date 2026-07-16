package com.secureiq.SecureIQ.question.controller;

import com.secureiq.SecureIQ.common.dto.ApiResponse;
import com.secureiq.SecureIQ.question.dto.*;
import com.secureiq.SecureIQ.question.model.Difficulty;
import com.secureiq.SecureIQ.question.model.QuestionType;
import com.secureiq.SecureIQ.question.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/questions")
@Tag(name = "Question Management", description = "Question Management APIs (CRUD, search, bulk import, dashboard)")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Get list of questions with pagination, sorting, and filters", description = "Retrieves a paginated list of questions. Supports searching by keyword, subject, difficulty, type, and bank.")
    public ApiResponse<Page<QuestionResponse>> getAll(
            @RequestParam(required = false) Long bankId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) QuestionType questionType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<QuestionResponse> questions = questionService.getAll(bankId, subjectId, departmentId, difficulty, questionType, keyword, pageable);
        return ApiResponse.success(questions, "Questions retrieved successfully");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Get question details by ID", description = "Retrieves a specific question's details.")
    public ApiResponse<QuestionResponse> getById(@PathVariable Long id) {
        QuestionResponse response = questionService.getById(id);
        return ApiResponse.success(response, "Question retrieved successfully");
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Get question dashboard statistics", description = "Retrieves count aggregations of question banks and questions, categorized by subject and difficulty.")
    public ApiResponse<QuestionDashboardResponse> getDashboard() {
        QuestionDashboardResponse response = questionService.getDashboard();
        return ApiResponse.success(response, "Question dashboard retrieved successfully");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Create a new question", description = "Create a new question under a question bank.")
    public ApiResponse<QuestionResponse> create(@Valid @RequestBody QuestionCreateRequest request) {
        QuestionResponse response = questionService.create(request);
        return ApiResponse.success(response, "Question created successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Update an existing question", description = "Fully update question details by ID.")
    public ApiResponse<QuestionResponse> update(@PathVariable Long id, @Valid @RequestBody QuestionUpdateRequest request) {
        QuestionResponse response = questionService.update(id, request);
        return ApiResponse.success(response, "Question updated successfully");
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Partially update an existing question", description = "Partially update question details by ID.")
    public ApiResponse<QuestionResponse> patch(@PathVariable Long id, @Valid @RequestBody QuestionPatchRequest request) {
        QuestionResponse response = questionService.patch(id, request);
        return ApiResponse.success(response, "Question updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Soft delete a question", description = "Performs a soft delete on a question record.")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ApiResponse.success("Question deleted successfully");
    }

    @PostMapping(value = "/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Bulk import questions via CSV", description = "Imports questions from a CSV file into the specified question bank.")
    public ApiResponse<BulkImportResponse> bulkImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bankId") Long bankId) {
        BulkImportResponse response = questionService.bulkImport(file, bankId);
        return ApiResponse.success(response, "Bulk import completed");
    }
}
