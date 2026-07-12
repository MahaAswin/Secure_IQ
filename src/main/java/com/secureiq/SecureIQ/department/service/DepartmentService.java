package com.secureiq.SecureIQ.department.service;

import com.secureiq.SecureIQ.department.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DepartmentService {
    Page<DepartmentResponse> getAll(String departmentName, String departmentCode, Pageable pageable);
    DepartmentResponse getById(Long id);
    DepartmentResponse create(DepartmentCreateRequest request);
    DepartmentResponse update(Long id, DepartmentUpdateRequest request);
    DepartmentResponse patch(Long id, DepartmentPatchRequest request);
    void delete(Long id);
    DepartmentDashboardResponse getDashboard(Long id);
}
