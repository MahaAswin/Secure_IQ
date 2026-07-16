package com.secureiq.SecureIQ.examsession.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAssignmentRequest {
    @NotEmpty(message = "Student IDs list cannot be empty")
    private List<Long> studentIds;
}
