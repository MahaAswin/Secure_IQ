package com.secureiq.SecureIQ.violation.service;

import com.secureiq.SecureIQ.common.exception.ConflictException;
import com.secureiq.SecureIQ.common.exception.NotFoundException;
import com.secureiq.SecureIQ.common.exception.UnauthorizedException;
import com.secureiq.SecureIQ.examattempt.model.StudentExamAttempt;
import com.secureiq.SecureIQ.examattempt.repository.StudentExamAttemptRepository;
import com.secureiq.SecureIQ.examsession.model.SessionStatus;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
import com.secureiq.SecureIQ.faculty.model.Faculty;
import com.secureiq.SecureIQ.faculty.repository.FacultyRepository;
import com.secureiq.SecureIQ.student.model.Student;
import com.secureiq.SecureIQ.student.repository.StudentRepository;
import com.secureiq.SecureIQ.user.model.User;
import com.secureiq.SecureIQ.violation.dto.*;
import com.secureiq.SecureIQ.violation.mapper.ViolationMapper;
import com.secureiq.SecureIQ.violation.model.Severity;
import com.secureiq.SecureIQ.violation.model.Source;
import com.secureiq.SecureIQ.violation.model.Violation;
import com.secureiq.SecureIQ.violation.model.ViolationType;
import com.secureiq.SecureIQ.violation.repository.ViolationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class ViolationServiceImpl implements ViolationService {

    private final ViolationRepository violationRepository;
    private final StudentExamAttemptRepository studentExamAttemptRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final ViolationMapper violationMapper;

    public ViolationServiceImpl(ViolationRepository violationRepository,
                                StudentExamAttemptRepository studentExamAttemptRepository,
                                StudentRepository studentRepository,
                                FacultyRepository facultyRepository,
                                DepartmentRepository departmentRepository,
                                ViolationMapper violationMapper) {
        this.violationRepository = violationRepository;
        this.studentExamAttemptRepository = studentExamAttemptRepository;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
        this.violationMapper = violationMapper;
    }

    @Override
    public Page<ViolationResponse> getAll(String violationCode, Long studentId, Long sessionId, Severity severity, ViolationType type, Source source, Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        User principal = (User) auth.getPrincipal();

        if (isAdmin || isHod) {
            // Admin and HOD can read all matching filter criteria
            Page<Violation> violations = violationRepository.findAllFiltered(violationCode, studentId, sessionId, severity, type, source, pageable);
            return violations.map(violationMapper::toResponse);
        }

        if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Faculty profile not found"));
            Page<Violation> violations = violationRepository.findAllFilteredForFaculty(faculty.getId(), violationCode, studentId, sessionId, severity, type, source, pageable);
            return violations.map(violationMapper::toResponse);
        }

        if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Student profile not found"));
            if (studentId != null && !studentId.equals(student.getId())) {
                throw new AccessDeniedException("Access denied: Students can only view their own violations");
            }
            Page<Violation> violations = violationRepository.findAllFilteredForStudent(student.getId(), violationCode, sessionId, severity, type, source, pageable);
            return violations.map(violationMapper::toResponse);
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }

    @Override
    public ViolationResponse getById(Long id) {
        Violation violation = getEntityById(id);
        checkReadAccess(violation);
        return violationMapper.toResponse(violation);
    }

    @Override
    @Transactional
    public ViolationResponse recordViolation(ViolationCreateRequest request) {
        if (violationRepository.existsByViolationCode(request.getViolationCode())) {
            throw new ConflictException("Violation code must be unique");
        }

        StudentExamAttempt attempt = studentExamAttemptRepository.findById(request.getStudentExamAttemptId())
                .orElseThrow(() -> new NotFoundException("Student Exam Attempt not found with id: " + request.getStudentExamAttemptId()));

        checkWriteAccess(attempt);

        Violation violation = violationMapper.toEntity(request, attempt);

        // Update warnings counts in the Student Exam Attempt
        if (violation.getSource() == Source.AI_ENGINE) {
            attempt.setAiWarnings(attempt.getAiWarnings() + 1);
        } else if (violation.getSource() == Source.ELECTRON || violation.getSource() == Source.SYSTEM) {
            attempt.setBrowserWarnings(attempt.getBrowserWarnings() + 1);
        }
        attempt.setTotalViolations(attempt.getTotalViolations() + 1);
        studentExamAttemptRepository.save(attempt);

        Violation savedViolation = violationRepository.save(violation);
        return violationMapper.toResponse(savedViolation);
    }

    @Override
    @Transactional
    public ViolationResponse updateViolation(Long id, ViolationUpdateRequest request) {
        Violation violation = getEntityById(id);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) auth.getPrincipal();

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));

        if (!isAdmin && !isFaculty) {
            throw new AccessDeniedException("Access denied: Only Admin and Faculty can update or resolve violations");
        }

        if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Faculty profile not found"));
            if (!violation.getStudentExamAttempt().getExamSession().getFaculty().getId().equals(faculty.getId())) {
                throw new AccessDeniedException("Access denied: Faculty can only update violations in their own assigned sessions");
            }
        }

        StudentExamAttempt targetAttempt = null;
        if (request.getStudentExamAttemptId() != null) {
            targetAttempt = studentExamAttemptRepository.findById(request.getStudentExamAttemptId())
                    .orElseThrow(() -> new NotFoundException("Student Exam Attempt not found with id: " + request.getStudentExamAttemptId()));
        }

        if (request.getViolationCode() != null && !request.getViolationCode().equals(violation.getViolationCode())) {
            if (violationRepository.existsByViolationCode(request.getViolationCode())) {
                throw new ConflictException("Violation code must be unique");
            }
        }

        violationMapper.updateEntity(request, targetAttempt, violation);
        Violation saved = violationRepository.save(violation);
        return violationMapper.toResponse(saved);
    }

    @Override
    public Page<ViolationResponse> getViolationsByStudent(Long studentId, Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        User principal = (User) auth.getPrincipal();

        if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Student profile not found"));
            if (!studentId.equals(student.getId())) {
                throw new AccessDeniedException("Access denied: Students can only view their own violations");
            }
        }

        Page<Violation> violations = violationRepository.findAllFiltered(null, studentId, null, null, null, null, pageable);
        return violations.map(violationMapper::toResponse);
    }

    @Override
    public Page<ViolationResponse> getViolationsBySession(Long sessionId, Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));

        User principal = (User) auth.getPrincipal();

        if (isStudent) {
            throw new AccessDeniedException("Access denied: Students cannot query violations by exam session");
        }

        if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Faculty profile not found"));
            Page<Violation> violations = violationRepository.findAllFilteredForFaculty(faculty.getId(), null, null, sessionId, null, null, null, pageable);
            return violations.map(violationMapper::toResponse);
        }

        Page<Violation> violations = violationRepository.findAllFiltered(null, null, sessionId, null, null, null, pageable);
        return violations.map(violationMapper::toResponse);
    }

    @Override
    public Page<ViolationResponse> getLiveViolations(Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        User principal = (User) auth.getPrincipal();

        if (isStudent) {
            throw new AccessDeniedException("Access denied: Students cannot view live violations board");
        }

        Page<Violation> violations;

        if (isAdmin) {
            violations = violationRepository.findAllBySessionStatus(SessionStatus.LIVE, pageable);
        } else if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Faculty profile not found"));
            violations = violationRepository.findAllByFacultyIdAndSessionStatus(faculty.getId(), SessionStatus.LIVE, pageable);
        } else if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            violations = violationRepository.findAllByDepartmentIdAndSessionStatus(dept.getId(), SessionStatus.LIVE, pageable);
        } else {
            violations = Page.empty();
        }

        return violations.map(violationMapper::toResponse);
    }

    @Override
    public ViolationDashboardResponse getDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        User principal = (User) auth.getPrincipal();

        ViolationDashboardResponse.ViolationDashboardResponseBuilder builder = ViolationDashboardResponse.builder();

        if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Student profile not found"));
            long total = violationRepository.countByStudentExamAttemptStudentId(student.getId());
            builder.totalViolationsCount(total);
        } else if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Faculty profile not found"));
            long liveCount = violationRepository.countByFacultyIdAndSessionStatus(faculty.getId(), SessionStatus.LIVE);
            long criticalCount = violationRepository.countByFacultyIdAndSeverity(faculty.getId(), Severity.CRITICAL);
            builder.liveViolationsCount(liveCount);
            builder.criticalViolationsCount(criticalCount);
        } else if (isAdmin) {
            // Group severity counts
            Map<String, Long> severityMap = new HashMap<>();
            for (Object[] obj : violationRepository.countBySeverityGroup()) {
                if (obj[0] != null) {
                    severityMap.put(((Severity) obj[0]).name(), (Long) obj[1]);
                }
            }

            // Group type counts
            Map<String, Long> typeMap = new HashMap<>();
            for (Object[] obj : violationRepository.countByViolationTypeGroup()) {
                if (obj[0] != null) {
                    typeMap.put(((ViolationType) obj[0]).name(), (Long) obj[1]);
                }
            }

            // Daily reports
            List<Map<String, Object>> dailyReports = new ArrayList<>();
            for (Object[] obj : violationRepository.countByDayGroup()) {
                if (obj[0] != null) {
                    Map<String, Object> dayMap = new HashMap<>();
                    dayMap.put("date", obj[0].toString());
                    dayMap.put("count", obj[1]);
                    dailyReports.add(dayMap);
                }
            }

            builder.severityStatistics(severityMap);
            builder.typeStatistics(typeMap);
            builder.dailyReports(dailyReports);
        }

        return builder.build();
    }

    private Violation getEntityById(Long id) {
        return violationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Violation not found with id: " + id));
    }

    private void checkReadAccess(Violation violation) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));

        User principal = (User) auth.getPrincipal();

        if (isAdmin) {
            return;
        }

        if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            if (!violation.getStudentExamAttempt().getExamSession().getExam().getDepartment().getId().equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only view violations recorded within their own department");
            }
            return;
        }

        if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Faculty profile not found"));
            if (!violation.getStudentExamAttempt().getExamSession().getFaculty().getId().equals(faculty.getId())) {
                throw new AccessDeniedException("Access denied: Faculty can only view violations recorded in their assigned sessions");
            }
            return;
        }

        if (isStudent) {
            if (!violation.getStudentExamAttempt().getStudent().getUser().getId().equals(principal.getId())) {
                throw new AccessDeniedException("Access denied: Students can only view their own proctoring violations");
            }
            return;
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }

    private void checkWriteAccess(StudentExamAttempt attempt) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        User principal = (User) auth.getPrincipal();

        if (isStudent) {
            if (!attempt.getStudent().getUser().getId().equals(principal.getId())) {
                throw new AccessDeniedException("Access denied: You can only log proctoring violations for your own exam attempts");
            }
        }
    }
}
