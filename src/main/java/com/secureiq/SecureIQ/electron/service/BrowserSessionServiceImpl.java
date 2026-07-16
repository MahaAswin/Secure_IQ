package com.secureiq.SecureIQ.electron.service;

import com.secureiq.SecureIQ.common.exception.BadRequestException;
import com.secureiq.SecureIQ.common.exception.NotFoundException;
import com.secureiq.SecureIQ.common.exception.UnauthorizedException;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
import com.secureiq.SecureIQ.electron.dto.*;
import com.secureiq.SecureIQ.electron.mapper.BrowserSessionMapper;
import com.secureiq.SecureIQ.electron.model.BrowserSession;
import com.secureiq.SecureIQ.electron.repository.BrowserSessionRepository;
import com.secureiq.SecureIQ.examattempt.model.AttemptStatus;
import com.secureiq.SecureIQ.examattempt.model.StudentExamAttempt;
import com.secureiq.SecureIQ.examattempt.repository.StudentExamAttemptRepository;
import com.secureiq.SecureIQ.faculty.model.Faculty;
import com.secureiq.SecureIQ.faculty.repository.FacultyRepository;
import com.secureiq.SecureIQ.student.model.Student;
import com.secureiq.SecureIQ.student.repository.StudentRepository;
import com.secureiq.SecureIQ.user.model.User;
import com.secureiq.SecureIQ.violation.dto.ViolationCreateRequest;
import com.secureiq.SecureIQ.violation.model.Severity;
import com.secureiq.SecureIQ.violation.model.Source;
import com.secureiq.SecureIQ.violation.model.ViolationType;
import com.secureiq.SecureIQ.violation.service.ViolationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class BrowserSessionServiceImpl implements BrowserSessionService {

    private final BrowserSessionRepository browserSessionRepository;
    private final StudentExamAttemptRepository studentExamAttemptRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final ViolationService violationService;
    private final BrowserSessionMapper browserSessionMapper;

    public BrowserSessionServiceImpl(BrowserSessionRepository browserSessionRepository,
                                     StudentExamAttemptRepository studentExamAttemptRepository,
                                     StudentRepository studentRepository,
                                     FacultyRepository facultyRepository,
                                     DepartmentRepository departmentRepository,
                                     ViolationService violationService,
                                     BrowserSessionMapper browserSessionMapper) {
        this.browserSessionRepository = browserSessionRepository;
        this.studentExamAttemptRepository = studentExamAttemptRepository;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
        this.violationService = violationService;
        this.browserSessionMapper = browserSessionMapper;
    }

    @Override
    @Transactional
    public BrowserSessionResponse connect(BrowserConnectRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        User principal = (User) auth.getPrincipal();

        Student student = studentRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new AccessDeniedException("Access denied: Student profile not found"));

        // Find student exam attempt by code
        StudentExamAttempt attempt = studentExamAttemptRepository.findAllByStudentId(student.getId()).stream()
                .filter(a -> a.getAttemptCode().equals(request.getAttemptCode()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Student Exam Attempt not found with code: " + request.getAttemptCode()));

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Cannot connect browser: Exam attempt is not IN_PROGRESS");
        }

        // Deactivate previous active sessions for this attempt
        List<BrowserSession> previousSessions = browserSessionRepository.findAllByStudentExamAttemptIdAndActiveTrue(attempt.getId());
        for (BrowserSession s : previousSessions) {
            s.setActive(false);
            s.setEndedAt(LocalDateTime.now());
            browserSessionRepository.save(s);
        }

        String sessionId = UUID.randomUUID().toString();

        BrowserSession session = BrowserSession.builder()
                .sessionId(sessionId)
                .studentExamAttempt(attempt)
                .browserVersion(request.getBrowserVersion())
                .operatingSystem(request.getOperatingSystem())
                .machineId(request.getMachineId())
                .ipAddress(request.getIpAddress())
                .startedAt(LocalDateTime.now())
                .active(true)
                .deleted(false)
                .build();

        BrowserSession saved = browserSessionRepository.save(session);
        return browserSessionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void disconnect(BrowserDisconnectRequest request) {
        BrowserSession session = browserSessionRepository.findBySessionId(request.getSessionId())
                .orElseThrow(() -> new NotFoundException("Browser Session not found with ID: " + request.getSessionId()));
        session.setActive(false);
        session.setEndedAt(LocalDateTime.now());
        browserSessionRepository.save(session);
    }

    @Override
    @Transactional
    public void heartbeat(BrowserHeartbeatRequest request) {
        BrowserSession session = browserSessionRepository.findBySessionId(request.getSessionId())
                .orElseThrow(() -> new NotFoundException("Browser Session not found with ID: " + request.getSessionId()));
        session.setActive(true);
        session.setUpdatedAt(LocalDateTime.now());
        browserSessionRepository.save(session);
    }

    @Override
    @Transactional
    public void recordEvent(BrowserEventRequest request) {
        BrowserSession session = browserSessionRepository.findBySessionId(request.getSessionId())
                .orElseThrow(() -> new NotFoundException("Browser Session not found with ID: " + request.getSessionId()));

        String event = request.getEventType().toUpperCase();
        ViolationType type;
        Severity severity;
        String defaultDesc = "Browser proctoring event detected";

        switch (event) {
            case "TAB_SWITCH":
                type = ViolationType.TAB_SWITCH;
                severity = Severity.MEDIUM;
                defaultDesc = "Tab switch event detected";
                break;
            case "WINDOW_BLUR":
                type = ViolationType.WINDOW_BLUR;
                severity = Severity.LOW;
                defaultDesc = "Window lost focus event detected";
                break;
            case "COPY_ATTEMPT":
            case "PASTE_ATTEMPT":
                type = ViolationType.COPY_PASTE;
                severity = Severity.MEDIUM;
                defaultDesc = "Clipboard copy/paste attempt detected";
                break;
            case "SCREENSHOT_ATTEMPT":
                type = ViolationType.SCREENSHOT_ATTEMPT;
                severity = Severity.HIGH;
                defaultDesc = "Screenshot attempt detected";
                break;
            case "DEVTOOLS_OPENED":
                type = ViolationType.DEVTOOLS_OPENED;
                severity = Severity.CRITICAL;
                defaultDesc = "Developer tools opened event detected";
                break;
            case "MULTIPLE_MONITOR":
                type = ViolationType.MULTIPLE_MONITOR;
                severity = Severity.HIGH;
                defaultDesc = "Multiple monitor setup connection detected";
                break;
            case "FULLSCREEN_EXIT":
                type = ViolationType.WINDOW_BLUR;
                severity = Severity.LOW;
                defaultDesc = "Exit from browser fullscreen mode detected";
                break;
            case "WINDOW_MINIMIZED":
                type = ViolationType.WINDOW_BLUR;
                severity = Severity.LOW;
                defaultDesc = "Browser window minimized";
                break;
            case "WINDOW_CLOSED":
                type = ViolationType.WINDOW_BLUR;
                severity = Severity.MEDIUM;
                defaultDesc = "Browser window closed";
                break;
            default:
                type = ViolationType.UNKNOWN;
                severity = Severity.LOW;
                defaultDesc = "Unknown browser proctoring event: " + request.getEventType();
        }

        String description = request.getDescription() != null ? request.getDescription() : defaultDesc;

        ViolationCreateRequest violationReq = ViolationCreateRequest.builder()
                .violationCode("VIO-BRW-" + session.getSessionId().substring(0, 8).toUpperCase() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .studentExamAttemptId(session.getStudentExamAttempt().getId())
                .violationType(type)
                .severity(severity)
                .source(Source.ELECTRON)
                .description(description)
                .confidenceScore(100.0)
                .detectedAt(LocalDateTime.now())
                .resolved(false)
                .build();

        violationService.recordViolation(violationReq);
    }

    @Override
    public BrowserSessionResponse getSession(Long id) {
        BrowserSession session = browserSessionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Browser Session not found with ID: " + id));
        checkReadAccess(session);
        return browserSessionMapper.toResponse(session);
    }

    @Override
    public ElectronDashboardResponse getDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));

        User principal = (User) auth.getPrincipal();

        ElectronDashboardResponse.ElectronDashboardResponseBuilder builder = ElectronDashboardResponse.builder();
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(45); // Active threshold

        if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Faculty profile not found"));

            List<BrowserSession> activeSessions = browserSessionRepository.findActiveSessionsByFaculty(faculty.getId(), threshold);
            long connected = activeSessions.size();
            long totalActiveAttempts = studentExamAttemptRepository.countByFacultyIdAndStatus(faculty.getId(), AttemptStatus.IN_PROGRESS);
            long disconnected = totalActiveAttempts > connected ? totalActiveAttempts - connected : 0;

            List<Map<String, Object>> browserStatus = new ArrayList<>();
            // Retrieve all attempts currently in progress for this faculty
            // Simple mapping logic
            for (BrowserSession bs : activeSessions) {
                Map<String, Object> map = new HashMap<>();
                map.put("studentName", bs.getStudentExamAttempt().getStudent().getUser().getFirstName() + " " + bs.getStudentExamAttempt().getStudent().getUser().getLastName());
                map.put("registerNumber", bs.getStudentExamAttempt().getStudent().getRegisterNumber());
                map.put("status", "CONNECTED");
                map.put("browserVersion", bs.getBrowserVersion());
                map.put("ipAddress", bs.getIpAddress());
                map.put("lastActive", bs.getUpdatedAt().toString());
                browserStatus.add(map);
            }

            builder.connectedStudentsCount(connected);
            builder.disconnectedStudentsCount(disconnected);
            builder.browserStatus(browserStatus);
        } else if (isAdmin) {
            long activeSessionsCount = browserSessionRepository.countActiveSessions(threshold);

            Map<String, Long> versionMap = new HashMap<>();
            for (Object[] obj : browserSessionRepository.countByBrowserVersionGroup()) {
                if (obj[0] != null) {
                    versionMap.put(obj[0].toString(), (Long) obj[1]);
                }
            }

            Map<String, Long> osMap = new HashMap<>();
            for (Object[] obj : browserSessionRepository.countByOperatingSystemGroup()) {
                if (obj[0] != null) {
                    osMap.put(obj[0].toString(), (Long) obj[1]);
                }
            }

            builder.activeBrowserSessionsCount(activeSessionsCount);
            builder.browserVersionStatistics(versionMap);
            builder.operatingSystemStatistics(osMap);
        }

        return builder.build();
    }

    private void checkReadAccess(BrowserSession session) {
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
            if (!session.getStudentExamAttempt().getExamSession().getExam().getDepartment().getId().equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only view browser sessions within their own department");
            }
            return;
        }

        if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Faculty profile not found"));
            if (!session.getStudentExamAttempt().getExamSession().getFaculty().getId().equals(faculty.getId())) {
                throw new AccessDeniedException("Access denied: Faculty can only view browser sessions for their assigned sessions");
            }
            return;
        }

        if (isStudent) {
            if (!session.getStudentExamAttempt().getStudent().getUser().getId().equals(principal.getId())) {
                throw new AccessDeniedException("Access denied: Students can only view their own browser sessions");
            }
            return;
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }
}
