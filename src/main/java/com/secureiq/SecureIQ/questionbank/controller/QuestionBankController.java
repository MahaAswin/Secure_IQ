package com.secureiq.SecureIQ.questionbank.controller;

import com.secureiq.SecureIQ.common.dto.ApiResponse;
import com.secureiq.SecureIQ.questionbank.dto.QuestionBankCreateRequest;
import com.secureiq.SecureIQ.questionbank.dto.QuestionBankPatchRequest;
import com.secureiq.SecureIQ.questionbank.dto.QuestionBankResponse;
import com.secureiq.SecureIQ.questionbank.dto.QuestionBankUpdateRequest;
import com.secureiq.SecureIQ.questionbank.service.QuestionBankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/question-banks")
@Tag(name = "Question Bank Management", description = "Question Bank Management APIs (CRUD, search, pagination)")
public class QuestionBankController {

    private final QuestionBankService questionBankService;

    public QuestionBankController(QuestionBankService questionBankService) {
        this.questionBankService = questionBankService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Get list of question banks with pagination and search", description = "Retrieves a paginated list of question banks. Admins see all. Faculty see assigned subjects. HODs see own department. Students have no access.")
    public ApiResponse<Page<QuestionBankResponse>> getAll(
            @RequestParam(required = false) String bankName,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<QuestionBankResponse> banks = questionBankService.getAll(bankName, subjectId, departmentId, pageable);
        return ApiResponse.success(banks, "Question banks retrieved successfully");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Get question bank details by ID", description = "Retrieves a specific question bank's details.")
    public ApiResponse<QuestionBankResponse> getById(@PathVariable Long id) {
        QuestionBankResponse response = questionBankService.getById(id);
        return ApiResponse.success(response, "Question bank retrieved successfully");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Create a new question bank", description = "Create a new question bank. Admin has full rights. HOD can create within their department. Faculty can create for assigned subjects.")
    public ApiResponse<QuestionBankResponse> create(@Valid @RequestBody QuestionBankCreateRequest request) {
        QuestionBankResponse response = questionBankService.create(request);
        return ApiResponse.success(response, "Question bank created successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Update an existing question bank", description = "Fully update question bank details by ID.")
    public ApiResponse<QuestionBankResponse> update(@PathVariable Long id, @Valid @RequestBody QuestionBankUpdateRequest request) {
        QuestionBankResponse response = questionBankService.update(id, request);
        return ApiResponse.success(response, "Question bank updated successfully");
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Partially update an existing question bank", description = "Partially update question bank details by ID.")
    public ApiResponse<QuestionBankResponse> patch(@PathVariable Long id, @Valid @RequestBody QuestionBankPatchRequest request) {
        QuestionBankResponse response = questionBankService.patch(id, request);
        return ApiResponse.success(response, "Question bank updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Soft delete a question bank", description = "Performs a soft delete on a question bank record.")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        questionBankService.delete(id);
        return ApiResponse.success("Question bank deleted successfully");
    }
}
