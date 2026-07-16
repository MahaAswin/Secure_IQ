package com.secureiq.SecureIQ.examsession.service;

import com.secureiq.SecureIQ.common.exception.BadRequestException;
import com.secureiq.SecureIQ.common.exception.ConflictException;
import com.secureiq.SecureIQ.common.exception.NotFoundException;
import com.secureiq.SecureIQ.common.exception.UnauthorizedException;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
import com.secureiq.SecureIQ.exam.model.Exam;
import com.secureiq.SecureIQ.exam.repository.ExamRepository;
import com.secureiq.SecureIQ.examsession.dto.*;
import com.secureiq.SecureIQ.examsession.mapper.ExamSessionMapper;
import com.secureiq.SecureIQ.examsession.model.ExamSession;
import com.secureiq.SecureIQ.examsession.model.SessionStatus;
import com.secureiq.SecureIQ.examsession.repository.ExamSessionRepository;
import com.secureiq.SecureIQ.faculty.model.Faculty;
import com.secureiq.SecureIQ.faculty.repository.FacultyRepository;
import com.secureiq.SecureIQ.student.dto.StudentResponse;
import com.secureiq.SecureIQ.student.mapper.StudentMapper;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ExamSessionServiceImpl implements ExamSessionService {

    private final ExamSessionRepository examSessionRepository;
    private final ExamRepository examRepository;
    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final ExamSessionMapper examSessionMapper;
    private final StudentMapper studentMapper;

    public ExamSessionServiceImpl(ExamSessionRepository examSessionRepository,
                                   ExamRepository examRepository,
                                   FacultyRepository facultyRepository,
                                   StudentRepository studentRepository,
                                   DepartmentRepository departmentRepository,
                                   ExamSessionMapper examSessionMapper,
                                   StudentMapper studentMapper) {
        this.examSessionRepository = examSessionRepository;
        this.examRepository = examRepository;
        this.facultyRepository = facultyRepository;
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
        this.examSessionMapper = examSessionMapper;
        this.studentMapper = studentMapper;
    }

    @Override
    public Page<ExamSessionResponse> getAll(String sessionCode, Long examId, Long facultyId, Long departmentId, SessionStatus status, Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        User principal = (User) auth.getPrincipal();

        if (isAdmin) {
            Page<ExamSession> sessions = examSessionRepository.findAllFiltered(sessionCode, examId, facultyId, departmentId, status, pageable);
            return sessions.map(examSessionMapper::toResponse);
        }

        if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            if (departmentId != null && !departmentId.equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only query sessions in their own department");
            }
            Page<ExamSession> sessions = examSessionRepository.findAllFiltered(sessionCode, examId, facultyId, dept.getId(), status, pageable);
            return sessions.map(examSessionMapper::toResponse);
        }

        if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Faculty profile not found"));
            if (facultyId != null && !facultyId.equals(faculty.getId())) {
                throw new AccessDeniedException("Access denied: Faculty can only query their own sessions");
            }
            Page<ExamSession> sessions = examSessionRepository.findAllFilteredForFaculty(faculty.getId(), sessionCode, examId, departmentId, status, pageable);
            return sessions.map(examSessionMapper::toResponse);
        }

        if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Student profile not found"));
            Page<ExamSession> sessions = examSessionRepository.findAllFilteredForStudent(student.getId(), sessionCode, examId, facultyId, departmentId, status, pageable);
            return sessions.map(examSessionMapper::toResponse);
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }

    @Override
    public ExamSessionResponse getById(Long id) {
        ExamSession session = getEntityById(id);
        checkReadAccess(session);
        return examSessionMapper.toResponse(session);
    }

    @Override
    @Transactional
    public ExamSessionResponse create(ExamSessionCreateRequest request) {
        validateTimes(request.getStartDateTime(), request.getEndDateTime());
        validateVenueOverlap(request.getVenue(), request.getStartDateTime(), request.getEndDateTime(), null);
        validateSessionCodeUniqueness(request.getSessionCode(), null);

        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> new NotFoundException("Exam not found with id: " + request.getExamId()));
        Faculty faculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() -> new NotFoundException("Faculty not found with id: " + request.getFacultyId()));

        checkWriteAccess(exam, faculty);

        ExamSession session = examSessionMapper.toEntity(request, exam, faculty);
        ExamSession savedSession = examSessionRepository.save(session);
        return examSessionMapper.toResponse(savedSession);
    }

    @Override
    @Transactional
    public ExamSessionResponse update(Long id, ExamSessionUpdateRequest request) {
        ExamSession session = getEntityById(id);

        checkWriteAccess(session.getExam(), session.getFaculty());

        validateTimes(request.getStartDateTime(), request.getEndDateTime());
        validateVenueOverlap(request.getVenue(), request.getStartDateTime(), request.getEndDateTime(), id);
        validateSessionCodeUniqueness(request.getSessionCode(), id);

        Exam targetExam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> new NotFoundException("Exam not found with id: " + request.getExamId()));
        Faculty targetFaculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() -> new NotFoundException("Faculty not found with id: " + request.getFacultyId()));

        if (!targetExam.getId().equals(session.getExam().getId()) || !targetFaculty.getId().equals(session.getFaculty().getId())) {
            checkWriteAccess(targetExam, targetFaculty);
        }

        examSessionMapper.updateEntity(request, targetExam, targetFaculty, session);
        ExamSession savedSession = examSessionRepository.save(session);
        return examSessionMapper.toResponse(savedSession);
    }

    @Override
    @Transactional
    public ExamSessionResponse patch(Long id, ExamSessionPatchRequest request) {
        ExamSession session = getEntityById(id);

        checkWriteAccess(session.getExam(), session.getFaculty());

        LocalDateTime resolvedStart = request.getStartDateTime() != null ? request.getStartDateTime() : session.getStartDateTime();
        LocalDateTime resolvedEnd = request.getEndDateTime() != null ? request.getEndDateTime() : session.getEndDateTime();
        validateTimes(resolvedStart, resolvedEnd);

        String resolvedVenue = request.getVenue() != null ? request.getVenue() : session.getVenue();
        validateVenueOverlap(resolvedVenue, resolvedStart, resolvedEnd, id);

        if (request.getSessionCode() != null) {
            validateSessionCodeUniqueness(request.getSessionCode(), id);
        }

        Exam targetExam = null;
        if (request.getExamId() != null) {
            targetExam = examRepository.findById(request.getExamId())
                    .orElseThrow(() -> new NotFoundException("Exam not found with id: " + request.getExamId()));
        }

        Faculty targetFaculty = null;
        if (request.getFacultyId() != null) {
            targetFaculty = facultyRepository.findById(request.getFacultyId())
                    .orElseThrow(() -> new NotFoundException("Faculty not found with id: " + request.getFacultyId()));
        }

        if ((targetExam != null && !targetExam.getId().equals(session.getExam().getId())) || 
            (targetFaculty != null && !targetFaculty.getId().equals(session.getFaculty().getId()))) {
            checkWriteAccess(targetExam != null ? targetExam : session.getExam(), targetFaculty != null ? targetFaculty : session.getFaculty());
        }

        examSessionMapper.patchEntity(request, targetExam, targetFaculty, session);
        ExamSession savedSession = examSessionRepository.save(session);
        return examSessionMapper.toResponse(savedSession);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ExamSession session = getEntityById(id);
        checkWriteAccess(session.getExam(), session.getFaculty());
        examSessionRepository.delete(session);
    }

    @Override
    @Transactional
    public ExamSessionResponse assignStudents(Long sessionId, StudentAssignmentRequest request) {
        ExamSession session = getEntityById(sessionId);
        checkWriteAccess(session.getExam(), session.getFaculty());

        Set<Student> resolvedStudents = new HashSet<>();
        for (Long studentId : request.getStudentIds()) {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new NotFoundException("Student not found with id: " + studentId));
            resolvedStudents.add(student);
        }

        session.setStudents(resolvedStudents);
        session.setTotalStudents(resolvedStudents.size());
        ExamSession savedSession = examSessionRepository.save(session);
        return examSessionMapper.toResponse(savedSession);
    }

    @Override
    public List<StudentResponse> getAssignedStudents(Long sessionId) {
        ExamSession session = getEntityById(sessionId);
        checkReadAccess(session);
        return session.getStudents().stream()
                .map(studentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExamSessionResponse> getLiveSessions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        User principal = (User) auth.getPrincipal();

        List<ExamSession> sessions = new ArrayList<>();

        if (isAdmin) {
            sessions = examSessionRepository.findAllByStatus(SessionStatus.LIVE);
        } else if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            sessions = examSessionRepository.findAllByExamDepartmentIdAndStatus(dept.getId(), SessionStatus.LIVE);
        } else if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Faculty profile not found"));
            sessions = examSessionRepository.findAllByFacultyIdAndStatus(faculty.getId(), SessionStatus.LIVE);
        } else if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Student profile not found"));
            sessions = examSessionRepository.findAllByStudentIdAndStatus(student.getId(), SessionStatus.LIVE);
        }

        return sessions.stream()
                .map(examSessionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExamSessionResponse> getUpcomingSessions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        User principal = (User) auth.getPrincipal();
        LocalDateTime now = LocalDateTime.now();

        List<ExamSession> sessions = new ArrayList<>();

        if (isAdmin) {
            sessions = examSessionRepository.findAllByStatusAndStartDateTimeAfter(SessionStatus.SCHEDULED, now);
        } else if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            sessions = examSessionRepository.findAllByExamDepartmentId(dept.getId()).stream()
                    .filter(s -> s.getStatus() == SessionStatus.SCHEDULED && s.getStartDateTime().isAfter(now))
                    .collect(Collectors.toList());
        } else if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Faculty profile not found"));
            sessions = examSessionRepository.findAllByFacultyIdAndStatusAndStartDateTimeAfter(faculty.getId(), SessionStatus.SCHEDULED, now);
        } else if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Student profile not found"));
            sessions = examSessionRepository.findAllByStudentIdAndStatusAndStartDateTimeAfter(student.getId(), SessionStatus.SCHEDULED, now);
        }

        return sessions.stream()
                .map(examSessionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ExamSessionDashboardResponse getDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        User principal = (User) auth.getPrincipal();

        ExamSessionDashboardResponse.ExamSessionDashboardResponseBuilder builder = ExamSessionDashboardResponse.builder();

        if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Student profile not found"));
            
            List<ExamSession> upcoming = examSessionRepository.findAllByStudentIdAndStatusAndStartDateTimeAfter(student.getId(), SessionStatus.SCHEDULED, LocalDateTime.now());
            List<ExamSession> live = examSessionRepository.findAllByStudentIdAndStatus(student.getId(), SessionStatus.LIVE);

            builder.upcomingSessions(upcoming.stream().map(examSessionMapper::toResponse).collect(Collectors.toList()));
            builder.activeSession(live.isEmpty() ? null : examSessionMapper.toResponse(live.get(0)));
        } else if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Faculty profile not found"));
            
            List<ExamSession> mySessions = examSessionRepository.findAllFilteredForFaculty(faculty.getId(), null, null, null, null, Pageable.unpaged()).getContent();
            List<ExamSession> live = examSessionRepository.findAllByFacultyIdAndStatus(faculty.getId(), SessionStatus.LIVE);

            builder.mySessions(mySessions.stream().map(examSessionMapper::toResponse).collect(Collectors.toList()));
            builder.liveSessions(live.stream().map(examSessionMapper::toResponse).collect(Collectors.toList()));
        } else if (isAdmin || isHod) {
            // HOD stats scoped to department, Admin stats global
            long activeCount = 0;
            long totalCount = 0;
            long totalAssigned = 0;
            long totalJoined = 0;

            if (isAdmin) {
                activeCount = examSessionRepository.countByStatus(SessionStatus.LIVE);
                totalCount = examSessionRepository.count();
                List<Object[]> attendanceResult = examSessionRepository.getAttendanceSummary();
                if (!attendanceResult.isEmpty() && attendanceResult.get(0) != null) {
                    totalAssigned = ((Number) attendanceResult.get(0)[0]).longValue();
                    totalJoined = ((Number) attendanceResult.get(0)[1]).longValue();
                }
            } else {
                Department dept = departmentRepository.findByHodId(principal.getId())
                        .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
                List<ExamSession> deptSessions = examSessionRepository.findAllByExamDepartmentId(dept.getId());
                activeCount = deptSessions.stream().filter(s -> s.getStatus() == SessionStatus.LIVE).count();
                totalCount = deptSessions.size();
                for (ExamSession s : deptSessions) {
                    totalAssigned += s.getTotalStudents();
                    totalJoined += s.getJoinedStudents();
                }
            }

            double rate = totalAssigned > 0 ? ((double) totalJoined / totalAssigned) * 100.0 : 0.0;
            Map<String, Object> attSummary = new HashMap<>();
            attSummary.put("totalAssigned", totalAssigned);
            attSummary.put("totalJoined", totalJoined);
            attSummary.put("attendanceRate", rate);

            builder.activeSessionsCount(activeCount);
            builder.totalSessionsCount(totalCount);
            builder.attendanceSummary(attSummary);
        }

        return builder.build();
    }

    private ExamSession getEntityById(Long id) {
        return examSessionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Exam Session not found with id: " + id));
    }

    private void validateTimes(LocalDateTime start, LocalDateTime end) {
        if (!start.isBefore(end)) {
            throw new BadRequestException("Start time must be before end time");
        }
    }

    private void validateVenueOverlap(String venue, LocalDateTime start, LocalDateTime end, Long id) {
        if (examSessionRepository.existsOverlap(venue, start, end, id != null ? id : -1L)) {
            throw new BadRequestException("Session overlaps with another session for the same venue");
        }
    }

    private void validateSessionCodeUniqueness(String code, Long id) {
        if (id == null) {
            if (examSessionRepository.existsBySessionCode(code)) {
                throw new ConflictException("Session code must be unique");
            }
        } else {
            if (examSessionRepository.existsBySessionCodeAndIdNot(code, id)) {
                throw new ConflictException("Session code must be unique");
            }
        }
    }

    private void checkReadAccess(ExamSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        User principal = (User) auth.getPrincipal();

        if (isAdmin) {
            return;
        }

        if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            if (!session.getExam().getDepartment().getId().equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only read sessions inside their own department");
            }
            return;
        }

        if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Faculty profile not found"));
            if (!session.getFaculty().getId().equals(faculty.getId())) {
                throw new AccessDeniedException("Access denied: Faculty can only read their own assigned sessions");
            }
            return;
        }

        if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Student profile not found"));
            boolean isAssigned = session.getStudents().stream().anyMatch(s -> s.getId().equals(student.getId()));
            if (!isAssigned) {
                throw new AccessDeniedException("Access denied: Students can only view sessions they are assigned to");
            }
            return;
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }

    private void checkWriteAccess(Exam exam, Faculty faculty) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));

        User principal = (User) auth.getPrincipal();

        if (isAdmin) {
            return;
        }

        if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            if (!exam.getDepartment().getId().equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only manage sessions for exams inside their own department");
            }
            return;
        }

        if (isFaculty) {
            Faculty loggedInFaculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Faculty profile not found"));
            if (!faculty.getId().equals(loggedInFaculty.getId())) {
                throw new AccessDeniedException("Access denied: Faculty can only manage their own sessions");
            }
            return;
        }

        throw new AccessDeniedException("Access denied: Only Admin, HOD, and Faculty can manage exam sessions");
    }
}
