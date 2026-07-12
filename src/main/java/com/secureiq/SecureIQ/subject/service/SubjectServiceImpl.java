package com.secureiq.SecureIQ.subject.service;

import com.secureiq.SecureIQ.common.exception.ConflictException;
import com.secureiq.SecureIQ.common.exception.NotFoundException;
import com.secureiq.SecureIQ.common.exception.UnauthorizedException;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
import com.secureiq.SecureIQ.student.model.Student;
import com.secureiq.SecureIQ.student.repository.StudentRepository;
import com.secureiq.SecureIQ.subject.dto.*;
import com.secureiq.SecureIQ.subject.mapper.SubjectMapper;
import com.secureiq.SecureIQ.subject.model.Subject;
import com.secureiq.SecureIQ.subject.repository.SubjectRepository;
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

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final SubjectMapper subjectMapper;

    public SubjectServiceImpl(SubjectRepository subjectRepository,
                              DepartmentRepository departmentRepository,
                              UserRepository userRepository,
                              StudentRepository studentRepository,
                              SubjectMapper subjectMapper) {
        this.subjectRepository = subjectRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.subjectMapper = subjectMapper;
    }

    @Override
    public Page<SubjectResponse> getAll(String subjectName, String subjectCode, Integer semester, Long departmentId, Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        if (isAdmin) {
            Page<Subject> subjects = subjectRepository.findAllFiltered(subjectName, subjectCode, semester, departmentId, pageable);
            return subjects.map(subjectMapper::toResponse);
        }

        User principal = (User) auth.getPrincipal();

        if (isFaculty) {
            Page<Subject> subjects = subjectRepository.findAllFilteredForFaculty(principal.getId(), subjectName, subjectCode, semester, departmentId, pageable);
            return subjects.map(subjectMapper::toResponse);
        }

        if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            // Force filtering to HOD's department
            if (departmentId != null && !departmentId.equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only query subjects in their own department");
            }
            Page<Subject> subjects = subjectRepository.findAllFiltered(subjectName, subjectCode, semester, dept.getId(), pageable);
            return subjects.map(subjectMapper::toResponse);
        }

        if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Logged in user is not registered as a student"));
            
            Department dept = student.getDepartment();
            if (dept == null) {
                return new PageImpl<>(List.of(), pageable, 0);
            }

            // Force filtering to student's department and student's semester
            if (departmentId != null && !departmentId.equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: Student can only query subjects in their own department");
            }
            if (semester != null && !semester.equals(student.getSemester())) {
                throw new AccessDeniedException("Access denied: Student can only query subjects in their own semester");
            }

            Page<Subject> subjects = subjectRepository.findAllFiltered(subjectName, subjectCode, student.getSemester(), dept.getId(), pageable);
            return subjects.map(subjectMapper::toResponse);
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }

    @Override
    public SubjectResponse getById(Long id) {
        Subject subject = getEntityById(id);
        checkSubjectReadAccess(subject);
        return subjectMapper.toResponse(subject);
    }

    @Override
    @Transactional
    public SubjectResponse create(SubjectCreateRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) auth.getPrincipal();
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + request.getDepartmentId()));

        if (isHod) {
            if (department.getHod() == null || !department.getHod().getId().equals(principal.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only create subjects in their own department");
            }
        }

        if (subjectRepository.existsBySubjectCode(request.getSubjectCode())) {
            throw new ConflictException("Subject code is already registered");
        }

        Set<User> faculty = resolveFaculty(request.getFacultyIds());

        Subject subject = subjectMapper.toEntity(request, department, faculty);
        Subject savedSubject = subjectRepository.save(subject);
        return subjectMapper.toResponse(savedSubject);
    }

    @Override
    @Transactional
    public SubjectResponse update(Long id, SubjectUpdateRequest request) {
        Subject subject = getEntityById(id);
        checkSubjectWriteAccess(subject);

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + request.getDepartmentId()));

        // HOD check on target department as well
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) auth.getPrincipal();
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        if (isHod) {
            if (department.getHod() == null || !department.getHod().getId().equals(principal.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only move subjects to their own department");
            }
        }

        if (!subject.getSubjectCode().equalsIgnoreCase(request.getSubjectCode()) &&
                subjectRepository.existsBySubjectCode(request.getSubjectCode())) {
            throw new ConflictException("Subject code is already registered");
        }

        Set<User> faculty = resolveFaculty(request.getFacultyIds());

        subjectMapper.updateEntity(request, department, faculty, subject);
        Subject savedSubject = subjectRepository.save(subject);
        return subjectMapper.toResponse(savedSubject);
    }

    @Override
    @Transactional
    public SubjectResponse patch(Long id, SubjectPatchRequest request) {
        Subject subject = getEntityById(id);
        checkSubjectWriteAccess(subject);

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new NotFoundException("Department not found with id: " + request.getDepartmentId()));

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User principal = (User) auth.getPrincipal();
            boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
            if (isHod) {
                if (department.getHod() == null || !department.getHod().getId().equals(principal.getId())) {
                    throw new AccessDeniedException("Access denied: HOD can only move subjects to their own department");
                }
            }
        }

        if (request.getSubjectCode() != null &&
                !subject.getSubjectCode().equalsIgnoreCase(request.getSubjectCode()) &&
                subjectRepository.existsBySubjectCode(request.getSubjectCode())) {
            throw new ConflictException("Subject code is already registered");
        }

        Set<User> faculty = null;
        if (request.getFacultyIds() != null) {
            faculty = resolveFaculty(request.getFacultyIds());
        }

        subjectMapper.patchEntity(request, department, faculty, subject);
        Subject savedSubject = subjectRepository.save(subject);
        return subjectMapper.toResponse(savedSubject);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Subject subject = getEntityById(id);
        checkSubjectWriteAccess(subject);
        subjectRepository.delete(subject);
    }

    @Override
    public SubjectDashboardResponse getDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        User principal = (User) auth.getPrincipal();

        List<Subject> visibleSubjects = new ArrayList<>();

        if (isAdmin) {
            visibleSubjects = subjectRepository.findAll();
        } else if (isFaculty) {
            visibleSubjects = subjectRepository.findAllByFacultyId(principal.getId());
        } else if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            visibleSubjects = subjectRepository.findAllFiltered(null, null, null, dept.getId(), Pageable.unpaged()).getContent();
        } else if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Logged in user is not registered as a student"));
            if (student.getDepartment() != null) {
                visibleSubjects = subjectRepository.findAllByDepartmentIdAndSemester(student.getDepartment().getId(), student.getSemester());
            }
        }

        long totalSubjects = visibleSubjects.size();

        // Subjects by semester
        Map<Integer, Long> subjectsBySemester = visibleSubjects.stream()
                .collect(Collectors.groupingBy(Subject::getSemester, Collectors.counting()));

        // Subjects by department
        Map<String, Long> subjectsByDepartment = visibleSubjects.stream()
                .collect(Collectors.groupingBy(s -> s.getDepartment().getDepartmentCode(), Collectors.counting()));

        return SubjectDashboardResponse.builder()
                .totalSubjects(totalSubjects)
                .subjectsBySemester(subjectsBySemester)
                .subjectsByDepartment(subjectsByDepartment)
                .build();
    }

    private Subject getEntityById(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Subject not found with id: " + id));
    }

    private Set<User> resolveFaculty(List<Long> facultyIds) {
        if (facultyIds == null || facultyIds.isEmpty()) {
            return new HashSet<>();
        }
        return facultyIds.stream()
                .map(fid -> userRepository.findById(fid)
                        .orElseThrow(() -> new NotFoundException("Faculty user not found with id: " + fid)))
                .collect(Collectors.toSet());
    }

    private void checkSubjectReadAccess(Subject subject) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        if (isAdmin) {
            return;
        }

        User principal = (User) auth.getPrincipal();

        if (isFaculty) {
            boolean isAssigned = subject.getFaculty().stream().anyMatch(f -> f.getId().equals(principal.getId()));
            if (!isAssigned) {
                throw new AccessDeniedException("Access denied: Faculty can only read assigned subjects");
            }
            return;
        }

        if (isHod) {
            if (subject.getDepartment().getHod() == null || !subject.getDepartment().getHod().getId().equals(principal.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only read subjects in their own department");
            }
            return;
        }

        if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Logged in user is not registered as a student"));
            if (student.getDepartment() == null || !student.getDepartment().getId().equals(subject.getDepartment().getId()) ||
                    !student.getSemester().equals(subject.getSemester())) {
                throw new AccessDeniedException("Access denied: Students can only read subjects in their own department and semester");
            }
            return;
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }

    private void checkSubjectWriteAccess(Subject subject) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));

        if (isAdmin) {
            return;
        }

        User principal = (User) auth.getPrincipal();

        if (isHod) {
            if (subject.getDepartment().getHod() == null || !subject.getDepartment().getHod().getId().equals(principal.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only write subjects in their own department");
            }
            return;
        }

        throw new AccessDeniedException("Access denied: Only Admin and HOD roles can modify subjects");
    }
}
