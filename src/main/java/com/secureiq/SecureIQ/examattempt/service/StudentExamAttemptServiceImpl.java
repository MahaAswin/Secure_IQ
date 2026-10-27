package com.secureiq.SecureIQ.examattempt.service;

import com.secureiq.SecureIQ.common.config.DateTimeConfig;
import com.secureiq.SecureIQ.common.exception.BadRequestException;
import com.secureiq.SecureIQ.common.exception.ConflictException;
import com.secureiq.SecureIQ.common.exception.NotFoundException;
import com.secureiq.SecureIQ.common.exception.UnauthorizedException;
import com.secureiq.SecureIQ.exam.model.Exam;
import com.secureiq.SecureIQ.exam.model.ExamStatus;
import com.secureiq.SecureIQ.exam.repository.ExamRepository;
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StudentExamAttemptServiceImpl implements StudentExamAttemptService {

    private final StudentExamAttemptRepository studentExamAttemptRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamRepository examRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentExamAttemptMapper studentExamAttemptMapper;
    private final Clock clock;

    public StudentExamAttemptServiceImpl(StudentExamAttemptRepository studentExamAttemptRepository,
                                         ExamSessionRepository examSessionRepository,
                                         ExamRepository examRepository,
                                         StudentRepository studentRepository,
                                         FacultyRepository facultyRepository,
                                         DepartmentRepository departmentRepository,
                                         StudentExamAttemptMapper studentExamAttemptMapper,
                                         Clock clock) {
        this.studentExamAttemptRepository = studentExamAttemptRepository;
        this.examSessionRepository = examSessionRepository;
        this.examRepository = examRepository;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
        this.studentExamAttemptMapper = studentExamAttemptMapper;
        this.clock = clock;
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
    @Transactional
    public StudentExamAttemptResponse getById(Long id) {
        StudentExamAttempt attempt = getEntityById(id);
        checkReadAccess(attempt);

        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            Instant now = clock.instant();
            Instant deadline = attempt.getAuthoritativeEndTime();
            Instant scheduledEnd = attempt.getExamSession() != null ? attempt.getExamSession().getAuthoritativeEndInstant() : null;
            Instant effectiveLimit = (scheduledEnd != null && scheduledEnd.isBefore(deadline)) ? scheduledEnd : deadline;

            if (effectiveLimit != null && !now.isBefore(effectiveLimit)) {
                attempt.setStatus(AttemptStatus.AUTO_SUBMITTED);
                attempt.setAutoSubmitted(true);
                if (attempt.getSubmittedTime() == null) {
                    attempt.setSubmittedTime(LocalDateTime.ofInstant(effectiveLimit, DateTimeConfig.INSTITUTION_ZONE));
                }
                attempt = studentExamAttemptRepository.save(attempt);
            }
        }
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

        ExamSession session = null;
        Exam exam = null;

        if (request.getExamSessionId() != null) {
            session = examSessionRepository.findById(request.getExamSessionId())
                    .orElseThrow(() -> new NotFoundException("Exam Session not found with id: " + request.getExamSessionId()));
            exam = session.getExam();
        } else if (request.getExamId() != null) {
            exam = examRepository.findById(request.getExamId())
                    .orElseThrow(() -> new NotFoundException("Exam not found with id: " + request.getExamId()));
            List<ExamSession> sessions = examSessionRepository.findAllByExamId(exam.getId());
            if (!sessions.isEmpty()) {
                session = sessions.get(0);
            } else {
                session = ExamSession.builder()
                        .sessionCode(exam.getExamCode() + "-S1")
                        .sessionName(exam.getExamTitle() + " - Session 1")
                        .exam(exam)
                        .faculty(exam.getFaculty())
                        .startDateTime(LocalDateTime.ofInstant(exam.getAuthoritativeStartInstant(), DateTimeConfig.INSTITUTION_ZONE))
                        .endDateTime(LocalDateTime.ofInstant(exam.getAuthoritativeEndInstant(), DateTimeConfig.INSTITUTION_ZONE))
                        .venue(exam.getVenue() != null ? exam.getVenue() : "Main Examination Hall")
                        .instructions(exam.getInstructions())
                        .status(SessionStatus.SCHEDULED)
                        .build();
                session = examSessionRepository.save(session);
            }
        } else {
            throw new BadRequestException("Either examSessionId or examId is required");
        }

        // 1. Verify student authorization and eligibility
        boolean isAssigned = session.getStudents() != null && !session.getStudents().isEmpty() &&
                session.getStudents().stream().anyMatch(s -> s.getId().equals(student.getId()));
        boolean matchesDeptAndSemester = exam.getDepartment() != null &&
                exam.getDepartment().getId().equals(student.getDepartment().getId()) &&
                (exam.getSemester() == null || exam.getSemester().equals(student.getSemester()));

        if (session.getStudents() != null && !session.getStudents().isEmpty()) {
            if (!isAssigned) {
                throw new AccessDeniedException("Access denied: You are not assigned to this exam session");
            }
        } else {
            if (!matchesDeptAndSemester && !isAssigned) {
                throw new AccessDeniedException("Access denied: You are not authorized to attend this exam");
            }
        }

        // 2. Check exam status
        if (exam.getStatus() == ExamStatus.CANCELLED || session.getStatus() == SessionStatus.CANCELLED) {
            throw new BadRequestException("This exam has been cancelled");
        }
        if (exam.getStatus() == ExamStatus.DRAFT) {
            throw new AccessDeniedException("This exam is not available yet");
        }

        // 3. Read authoritative backend time and scheduled window
        Instant now = clock.instant();
        Instant scheduledStart = session.getAuthoritativeStartInstant();
        Instant scheduledEnd = session.getAuthoritativeEndInstant();

        // 4. Check whether student already has an attempt
        Optional<StudentExamAttempt> existingOpt = studentExamAttemptRepository.findByStudentIdAndExamSessionId(student.getId(), session.getId());
        if (existingOpt.isEmpty() && exam.getId() != null) {
            existingOpt = studentExamAttemptRepository.findFirstByStudentIdAndExamId(student.getId(), exam.getId());
        }

        if (existingOpt.isPresent()) {
            StudentExamAttempt existing = existingOpt.get();
            if (existing.getStatus() == AttemptStatus.SUBMITTED || existing.getStatus() == AttemptStatus.AUTO_SUBMITTED || existing.getStatus() == AttemptStatus.TERMINATED) {
                throw new BadRequestException("You have already completed or submitted this exam");
            }

            // Attempt is IN_PROGRESS: check if expired
            Instant attemptDeadline = existing.getAuthoritativeEndTime();
            Instant effectiveLimit = (scheduledEnd != null && scheduledEnd.isBefore(attemptDeadline)) ? scheduledEnd : attemptDeadline;
            if (effectiveLimit != null && !now.isBefore(effectiveLimit)) {
                existing.setStatus(AttemptStatus.AUTO_SUBMITTED);
                existing.setAutoSubmitted(true);
                if (existing.getSubmittedTime() == null) {
                    existing.setSubmittedTime(LocalDateTime.ofInstant(effectiveLimit, DateTimeConfig.INSTITUTION_ZONE));
                }
                studentExamAttemptRepository.save(existing);
                throw new BadRequestException("Your exam attempt has expired");
            }

            if (Boolean.TRUE.equals(request.getResume())) {
                return studentExamAttemptMapper.toResponse(existing);
            }
            throw new ConflictException("You have already attempted or started this exam session");
        }

        // 5. Enforce schedule window: scheduledStart <= now < scheduledEnd
        if (scheduledStart != null && now.isBefore(scheduledStart)) {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(DateTimeConfig.INSTITUTION_ZONE);
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a").withZone(DateTimeConfig.INSTITUTION_ZONE);
            String formattedDate = dateFormatter.format(scheduledStart);
            String formattedTime = timeFormatter.format(scheduledStart);
            throw new BadRequestException("This exam is not available yet. The exam starts on " + formattedDate + " at " + formattedTime + ".");
        }

        if (scheduledEnd != null && !now.isBefore(scheduledEnd)) {
            throw new BadRequestException("The scheduled exam has ended");
        }

        // 6. Transition session status to LIVE if scheduled
        if (session.getStatus() == SessionStatus.SCHEDULED) {
            session.setStatus(SessionStatus.LIVE);
            examSessionRepository.save(session);
        }

        // 7. Calculate effective deadline: min(attemptStart + duration, scheduledEnd)
        int durationMinutes = exam.getDurationMinutes();
        Instant candidateEnd = now.plus(Duration.ofMinutes(durationMinutes));
        Instant effectiveDeadline = (scheduledEnd != null && candidateEnd.isAfter(scheduledEnd)) ? scheduledEnd : candidateEnd;

        String attemptCode = "ATT-" + session.getId() + "-" + student.getId() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        StudentExamAttempt attempt = StudentExamAttempt.builder()
                .attemptCode(attemptCode)
                .student(student)
                .examSession(session)
                .startTime(LocalDateTime.ofInstant(now, DateTimeConfig.INSTITUTION_ZONE))
                .endTime(LocalDateTime.ofInstant(effectiveDeadline, DateTimeConfig.INSTITUTION_ZONE))
                .status(AttemptStatus.IN_PROGRESS)
                .totalMarks(exam.getTotalMarks())
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

        Instant now = clock.instant();
        Instant attemptDeadline = attempt.getAuthoritativeEndTime();
        Instant scheduledEnd = attempt.getExamSession() != null ? attempt.getExamSession().getAuthoritativeEndInstant() : null;
        Instant effectiveLimit = (scheduledEnd != null && attemptDeadline != null && scheduledEnd.isBefore(attemptDeadline)) ? scheduledEnd : attemptDeadline;

        boolean isPastDeadline = effectiveLimit != null && !now.isBefore(effectiveLimit);
        boolean autoSubmitted = request.isAutoSubmitted() || isPastDeadline;

        attempt.setObtainedMarks(obtained);
        attempt.setScore(obtained);
        attempt.setPercentage((obtained / attempt.getTotalMarks()) * 100.0);
        attempt.setSubmittedTime(LocalDateTime.ofInstant(now, DateTimeConfig.INSTITUTION_ZONE));
        attempt.setAutoSubmitted(autoSubmitted);
        attempt.setStatus(autoSubmitted ? AttemptStatus.AUTO_SUBMITTED : AttemptStatus.SUBMITTED);

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
        attempt.setSubmittedTime(LocalDateTime.ofInstant(clock.instant(), DateTimeConfig.INSTITUTION_ZONE));
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
                Instant now = clock.instant();
                Instant attemptDeadline = attempt.getAuthoritativeEndTime();
                Instant scheduledEnd = attempt.getExamSession() != null ? attempt.getExamSession().getAuthoritativeEndInstant() : null;
                Instant effectiveLimit = (scheduledEnd != null && attemptDeadline != null && scheduledEnd.isBefore(attemptDeadline)) ? scheduledEnd : attemptDeadline;

                if (effectiveLimit != null && !now.isBefore(effectiveLimit)) {
                    attempt.setStatus(AttemptStatus.AUTO_SUBMITTED);
                    attempt.setAutoSubmitted(true);
                    if (attempt.getSubmittedTime() == null) {
                        attempt.setSubmittedTime(LocalDateTime.ofInstant(effectiveLimit, DateTimeConfig.INSTITUTION_ZONE));
                    }
                    studentExamAttemptRepository.save(attempt);
                    builder.currentAttempt(null);
                    builder.remainingTimeSeconds(0L);
                    builder.attemptStatus("NO_ACTIVE_ATTEMPT");
                } else {
                    long remaining = effectiveLimit != null ? Math.max(0L, Duration.between(now, effectiveLimit).toSeconds()) : 0L;
                    builder.currentAttempt(studentExamAttemptMapper.toResponse(attempt));
                    builder.remainingTimeSeconds(remaining);
                    builder.attemptStatus(attempt.getStatus().name());
                }
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
