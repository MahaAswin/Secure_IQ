package com.secureiq.SecureIQ.department.service;

import com.secureiq.SecureIQ.common.exception.ConflictException;
import com.secureiq.SecureIQ.common.exception.NotFoundException;
import com.secureiq.SecureIQ.common.exception.UnauthorizedException;
import com.secureiq.SecureIQ.department.dto.*;
import com.secureiq.SecureIQ.department.mapper.DepartmentMapper;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
import com.secureiq.SecureIQ.student.model.Student;
import com.secureiq.SecureIQ.student.repository.StudentRepository;
import com.secureiq.SecureIQ.user.dto.UserResponse;
import com.secureiq.SecureIQ.user.model.User;
import com.secureiq.SecureIQ.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final DepartmentMapper departmentMapper;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository,
                                 UserRepository userRepository,
                                 StudentRepository studentRepository,
                                 DepartmentMapper departmentMapper) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.departmentMapper = departmentMapper;
    }

    @Override
    public Page<DepartmentResponse> getAll(String departmentName, String departmentCode, Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        if (isAdmin || isFaculty) {
            Page<Department> depts = departmentRepository.findAllFiltered(departmentName, departmentCode, pageable);
            return depts.map(d -> departmentMapper.toResponse(d, getStudentCount(d), 0L));
        }

        User principal = (User) auth.getPrincipal();

        if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));

            if ((departmentName != null && !departmentName.isEmpty() && !dept.getDepartmentName().toLowerCase().contains(departmentName.toLowerCase())) ||
                (departmentCode != null && !departmentCode.isEmpty() && !dept.getDepartmentCode().toLowerCase().contains(departmentCode.toLowerCase()))) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
            Page<Department> depts = new PageImpl<>(List.of(dept), pageable, 1);
            return depts.map(d -> departmentMapper.toResponse(d, getStudentCount(d), 0L));
        }

        if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Logged in user is not registered as a student"));
            Department dept = student.getDepartment();
            if (dept == null) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
            if ((departmentName != null && !departmentName.isEmpty() && !dept.getDepartmentName().toLowerCase().contains(departmentName.toLowerCase())) ||
                (departmentCode != null && !departmentCode.isEmpty() && !dept.getDepartmentCode().toLowerCase().contains(departmentCode.toLowerCase()))) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
            Page<Department> depts = new PageImpl<>(List.of(dept), pageable, 1);
            return depts.map(d -> departmentMapper.toResponse(d, getStudentCount(d), 0L));
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }

    @Override
    public DepartmentResponse getById(Long id) {
        Department department = getEntityById(id);
        checkDepartmentReadAccess(department);
        return departmentMapper.toResponse(department, getStudentCount(department), 0L);
    }

    @Override
    @Transactional
    public DepartmentResponse create(DepartmentCreateRequest request) {
        if (departmentRepository.existsByDepartmentName(request.getDepartmentName())) {
            throw new ConflictException("Department name is already registered");
        }
        if (departmentRepository.existsByDepartmentCode(request.getDepartmentCode())) {
            throw new ConflictException("Department code is already registered");
        }

        User hod = null;
        if (request.getHodId() != null) {
            hod = userRepository.findById(request.getHodId())
                    .orElseThrow(() -> new NotFoundException("HOD user not found with id: " + request.getHodId()));
        }

        Department department = departmentMapper.toEntity(request, hod);
        Department savedDepartment = departmentRepository.save(department);
        return departmentMapper.toResponse(savedDepartment, 0L, 0L);
    }

    @Override
    @Transactional
    public DepartmentResponse update(Long id, DepartmentUpdateRequest request) {
        Department department = getEntityById(id);

        if (!department.getDepartmentName().equalsIgnoreCase(request.getDepartmentName()) &&
                departmentRepository.existsByDepartmentName(request.getDepartmentName())) {
            throw new ConflictException("Department name is already registered");
        }
        if (!department.getDepartmentCode().equalsIgnoreCase(request.getDepartmentCode()) &&
                departmentRepository.existsByDepartmentCode(request.getDepartmentCode())) {
            throw new ConflictException("Department code is already registered");
        }

        User hod = null;
        if (request.getHodId() != null) {
            hod = userRepository.findById(request.getHodId())
                    .orElseThrow(() -> new NotFoundException("HOD user not found with id: " + request.getHodId()));
        }

        departmentMapper.updateEntity(request, hod, department);
        Department savedDepartment = departmentRepository.save(department);
        return departmentMapper.toResponse(savedDepartment, getStudentCount(savedDepartment), 0L);
    }

    @Override
    @Transactional
    public DepartmentResponse patch(Long id, DepartmentPatchRequest request) {
        Department department = getEntityById(id);

        if (request.getDepartmentName() != null &&
                !department.getDepartmentName().equalsIgnoreCase(request.getDepartmentName()) &&
                departmentRepository.existsByDepartmentName(request.getDepartmentName())) {
            throw new ConflictException("Department name is already registered");
        }
        if (request.getDepartmentCode() != null &&
                !department.getDepartmentCode().equalsIgnoreCase(request.getDepartmentCode()) &&
                departmentRepository.existsByDepartmentCode(request.getDepartmentCode())) {
            throw new ConflictException("Department code is already registered");
        }

        User hod = null;
        if (request.getHodId() != null) {
            hod = userRepository.findById(request.getHodId())
                    .orElseThrow(() -> new NotFoundException("HOD user not found with id: " + request.getHodId()));
        }

        departmentMapper.patchEntity(request, hod, department);
        Department savedDepartment = departmentRepository.save(department);
        return departmentMapper.toResponse(savedDepartment, getStudentCount(savedDepartment), 0L);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Department department = getEntityById(id);
        departmentRepository.delete(department);
    }

    @Override
    public DepartmentDashboardResponse getDashboard(Long id) {
        Department department = getEntityById(id);
        checkDepartmentReadAccess(department);

        UserResponse hodResponse = department.getHod() != null ? UserResponse.fromEntity(department.getHod()) : null;

        return DepartmentDashboardResponse.builder()
                .departmentId(department.getId())
                .departmentName(department.getDepartmentName())
                .departmentCode(department.getDepartmentCode())
                .description(department.getDescription())
                .totalStudents(getStudentCount(department))
                .totalFaculty(0L) // Placeholder for future faculty relationship
                .hod(hodResponse)
                .build();
    }

    private Department getEntityById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + id));
    }

    private long getStudentCount(Department d) {
        return studentRepository.countByDepartmentId(d.getId());
    }

    private void checkDepartmentReadAccess(Department department) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        if (isAdmin || isFaculty) {
            return;
        }

        User principal = (User) auth.getPrincipal();

        if (isHod) {
            if (department.getHod() == null || !department.getHod().getId().equals(principal.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only view their own department");
            }
            return;
        }

        if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Logged in user is not registered as a student"));
            if (student.getDepartment() == null || !student.getDepartment().getId().equals(department.getId())) {
                throw new AccessDeniedException("Access denied: Students can only view their own department");
            }
            return;
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }
}
