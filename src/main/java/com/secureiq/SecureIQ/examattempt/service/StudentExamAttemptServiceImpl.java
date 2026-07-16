package com.secureiq.SecureIQ.examattempt.service;

import com.secureiq.SecureIQ.common.exception.BadRequestException;
import com.secureiq.SecureIQ.common.exception.ConflictException;
import com.secureiq.SecureIQ.common.exception.NotFoundException;
import com.secureiq.SecureIQ.common.exception.UnauthorizedException;
import com.secureiq.SecureIQ.examattempt.dto.*;
import com.secureiq.SecureIQ.examattempt.mapper.StudentExamAttemptMapper;
import com.secureiq.SecureIQ.examattempt.model.AttemptStatus;
import com.secureiq.SecureIQ.examattempt.model.StudentExamAttempt;
import com.secureiq.SecureIQ.examattempt.repository.StudentExamAttemptRepository;
import com.secureiq.SecureIQ.examsession.model.ExamSession;
import com.secureiq.SecureIQ.examsession.model.SessionStatus;
import com.secureiq.SecureIQ.examsession.repository.ExamSessionRepository;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
import com.secureiq.SecureIQ.faculty.model.Faculty;
import com.secureiq.SecureIQ.faculty.repository.FacultyRepository;
import com.secureiq.SecureIQ.student.model.Student;
import com.secureiq.SecureIQ.student.repository.StudentRepository;
import com.secureiq.SecureIQ.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StudentExamAttemptServiceImpl implements StudentExamAttemptService {

    private final StudentExamAttemptRepository studentExamAttemptRepository;
    private final ExamSessionRepository examSessionRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentExamAttemptMapper studentExamAttemptMapper;

    public StudentExamAttemptServiceImpl(StudentExamAttemptRepository studentExamAttemptRepository,
                                         ExamSessionRepository examSessionRepository,
                                         StudentRepository studentRepository,
                                         FacultyRepository facultyRepository,
                                         DepartmentRepository departmentRepository,
                                         StudentExamAttemptMapper studentExamAttemptMapper) {
        this.studentExamAttemptRepository = studentExamAttemptRepository;
        this.examSessionRepository = examSessionRepository;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
        this.studentExamAttemptMapper = studentExamAttemptMapper;
    }

    @Override
    public Page<StudentExamAttemptResponse> getAll(String attemptCode, Long studentId, Long examSessionId, Long departmentId, AttemptStatus status, Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));

        User principal = (User) auth.getPrincipal();

        if (isAdmin || isHod) {
            // HOD filters inside controller / custom checks if department passed.
            Page<StudentExamAttempt> attempts = studentExamAttemptRepository.findAllFiltered(attemptCode, studentId, examSessionId, departmentId, status, pageable);
            return attempts.map(studentExamAttemptMapper::toResponse);
        }

        if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Faculty profile not found"));
            Page<StudentExamAttempt> attempts = studentExamAttemptRepository.findAllFilteredForFaculty(faculty.getId(), attemptCode, studentId, examSessionId, departmentId, status, pageable);
            return attempts.map(studentExamAttemptMapper::toResponse);
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }

    @Override
    public StudentExamAttemptResponse getById(Long id) {
        StudentExamAttempt attempt = getEntityById(id);
        checkReadAccess(attempt);
        return studentExamAttemptMapper.toResponse(attempt);
    }

    @Override
    @Transactional
    public StudentExamAttemptResponse startAttempt(StartAttemptRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        User principal = (User) auth.getPrincipal();

        Student student = studentRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new AccessDeniedException("Access denied: Student profile not found"));

        ExamSession session = examSessionRepository.findById(request.getExamSessionId())
                .orElseThrow(() -> new NotFoundException("Exam Session not found with id: " + request.getExamSessionId()));

        // 1. Verify student is assigned to target session
        boolean isAssigned = session.getStudents().stream().anyMatch(s -> s.getId().equals(student.getId()));
        if (!isAssigned) {
            throw new AccessDeniedException("Access denied: You are not assigned to this exam session");
        }

        // 2. Verify student does not already have an attempt for the session
        if (studentExamAttemptRepository.existsByStudentIdAndExamSessionId(student.getId(), session.getId())) {
            throw new ConflictException("You have already attempted or started this exam session");
        }

        // 3. Verify session is LIVE
        if (session.getStatus() != SessionStatus.LIVE) {
            throw new BadRequestException("Cannot start exam attempt: Exam session is not LIVE");
        }

        String attemptCode = "ATT-" + session.getId() + "-" + student.getId() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime computedEnd = now.plusMinutes(session.getExam().getDurationMinutes());
        // Cap at session's end time
        if (computedEnd.isAfter(session.getEndDateTime())) {
            computedEnd = session.getEndDateTime();
        }

        StudentExamAttempt attempt = StudentExamAttempt.builder()
                .attemptCode(attemptCode)
                .student(student)
                .examSession(session)
                .startTime(now)
                .endTime(computedEnd)
                .status(AttemptStatus.IN_PROGRESS)
                .totalMarks(session.getExam().getTotalMarks())
                .score(0.0)
                .percentage(0.0)
                .obtainedMarks(0.0)
                .autoSubmitted(false)
                .browserWarnings(0)
                .aiWarnings(0)
                .totalViolations(0)
                .deleted(false)
                .build();

        StudentExamAttempt savedAttempt = studentExamAttemptRepository.save(attempt);
        return studentExamAttemptMapper.toResponse(savedAttempt);
    }

    @Override
    @Transactional
    public StudentExamAttemptResponse submitAttempt(SubmitAttemptRequest request) {
        StudentExamAttempt attempt = getEntityById(request.getAttemptId());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) auth.getPrincipal();
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        if (isStudent) {
            if (!attempt.getStudent().getUser().getId().equals(principal.getId())) {
                throw new AccessDeniedException("Access denied: You can only submit your own exam attempts");
            }
        }

        if (attempt.getStatus() == AttemptStatus.SUBMITTED || attempt.getStatus() == AttemptStatus.AUTO_SUBMITTED || attempt.getStatus() == AttemptStatus.TERMINATED) {
            throw new BadRequestException("Attempt is already submitted or terminated");
        }

        double obtained = request.getObtainedMarks();
        if (obtained < 0 || obtained > attempt.getTotalMarks()) {
            throw new BadRequestException("Obtained marks must be between 0 and total marks (" + attempt.getTotalMarks() + ")");
        }

        attempt.setObtainedMarks(obtained);
        attempt.setScore(obtained);
        attempt.setPercentage((obtained / attempt.getTotalMarks()) * 100.0);
        attempt.setSubmittedTime(LocalDateTime.now());
        attempt.setAutoSubmitted(request.isAutoSubmitted());
        attempt.setStatus(request.isAutoSubmitted() ? AttemptStatus.AUTO_SUBMITTED : AttemptStatus.SUBMITTED);

        StudentExamAttempt saved = studentExamAttemptRepository.save(attempt);
        return studentExamAttemptMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public StudentExamAttemptResponse terminateAttempt(TerminateAttemptRequest request) {
        StudentExamAttempt attempt = getEntityById(request.getAttemptId());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) auth.getPrincipal();

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));

        if (!isAdmin && !isFaculty) {
            throw new AccessDeniedException("Access denied: Only Faculty and Admin can terminate attempts");
        }

        if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Faculty profile not found"));
            if (!attempt.getExamSession().getFaculty().getId().equals(faculty.getId())) {
                throw new AccessDeniedException("Access denied: Faculty can only terminate attempts in their own exam sessions");
            }
        }

        if (attempt.getStatus() == AttemptStatus.SUBMITTED || attempt.getStatus() == AttemptStatus.AUTO_SUBMITTED || attempt.getStatus() == AttemptStatus.TERMINATED) {
            throw new BadRequestException("Attempt is already submitted or terminated");
        }

        attempt.setStatus(AttemptStatus.TERMINATED);
        attempt.setSubmittedTime(LocalDateTime.now());
        // Custom handling or logging reason in audit logs could be done here.
        StudentExamAttempt saved = studentExamAttemptRepository.save(attempt);
        return studentExamAttemptMapper.toResponse(saved);
    }

    @Override
    public Page<StudentExamAttemptResponse> getMyAttempts(String attemptCode, Long examSessionId, AttemptStatus status, Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        User principal = (User) auth.getPrincipal();

        Student student = studentRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new AccessDeniedException("Access denied: Student profile not found"));

        Page<StudentExamAttempt> attempts = studentExamAttemptRepository.findAllFilteredForStudent(student.getId(), attemptCode, examSessionId, status, pageable);
        return attempts.map(studentExamAttemptMapper::toResponse);
    }

    @Override
    public Page<StudentExamAttemptResponse> getAttemptsBySession(Long sessionId, Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));

        User principal = (User) auth.getPrincipal();

        ExamSession session = examSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Exam Session not found with id: " + sessionId));

        if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Faculty profile not found"));
            if (!session.getFaculty().getId().equals(faculty.getId())) {
                throw new AccessDeniedException("Access denied: Faculty can only read attempts for their own assigned sessions");
            }
        }

        if (isHod) {
            // HOD checks department of session
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            if (!session.getExam().getDepartment().getId().equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only view attempts inside their own department");
            }
        }

        Page<StudentExamAttempt> attempts = studentExamAttemptRepository.findAllFiltered(null, null, sessionId, null, null, pageable);
        return attempts.map(studentExamAttemptMapper::toResponse);
    }

    @Override
    public AttemptDashboardResponse getDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        User principal = (User) auth.getPrincipal();

        AttemptDashboardResponse.AttemptDashboardResponseBuilder builder = AttemptDashboardResponse.builder();

        if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Student profile not found"));

            Optional<StudentExamAttempt> activeOpt = studentExamAttemptRepository.findFirstByStudentIdAndStatus(student.getId(), AttemptStatus.IN_PROGRESS);
            if (activeOpt.isPresent()) {
                StudentExamAttempt attempt = activeOpt.get();
                long remaining = Duration.between(LocalDateTime.now(), attempt.getEndTime()).toSeconds();
                if (remaining < 0) {
                    remaining = 0;
                }
                builder.currentAttempt(studentExamAttemptMapper.toResponse(attempt));
                builder.remainingTimeSeconds(remaining);
                builder.attemptStatus(attempt.getStatus().name());
            } else {
                builder.currentAttempt(null);
                builder.remainingTimeSeconds(0L);
                builder.attemptStatus("NO_ACTIVE_ATTEMPT");
            }
        } else if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Faculty profile not found"));
            long active = studentExamAttemptRepository.countByFacultyIdAndStatus(faculty.getId(), AttemptStatus.IN_PROGRESS);
            long submitted = studentExamAttemptRepository.countSubmittedByFacultyId(faculty.getId());
            builder.activeAttemptsCount(active);
            builder.submittedAttemptsCount(submitted);
        } else if (isAdmin) {
            long total = studentExamAttemptRepository.count();
            long live = studentExamAttemptRepository.countByStatus(AttemptStatus.IN_PROGRESS);
            builder.totalAttemptsCount(total);
            builder.liveAttemptsCount(live);
        }

        return builder.build();
    }

    private StudentExamAttempt getEntityById(Long id) {
        return studentExamAttemptRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Student Exam Attempt not found with id: " + id));
    }

    private void checkReadAccess(StudentExamAttempt attempt) {
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
            if (!attempt.getExamSession().getExam().getDepartment().getId().equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only view attempts inside their own department");
            }
            return;
        }

        if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Faculty profile not found"));
            if (!attempt.getExamSession().getFaculty().getId().equals(faculty.getId())) {
                throw new AccessDeniedException("Access denied: Faculty can only view attempts inside their own exam sessions");
            }
            return;
        }

        if (isStudent) {
            if (!attempt.getStudent().getUser().getId().equals(principal.getId())) {
                throw new AccessDeniedException("Access denied: Students can only view their own exam attempts");
            }
            return;
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }
}
