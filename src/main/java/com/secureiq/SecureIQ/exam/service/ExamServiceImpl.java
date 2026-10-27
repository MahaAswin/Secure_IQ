package com.secureiq.SecureIQ.exam.service;

import com.secureiq.SecureIQ.common.exception.BadRequestException;
import com.secureiq.SecureIQ.common.exception.ConflictException;
import com.secureiq.SecureIQ.common.exception.NotFoundException;
import com.secureiq.SecureIQ.common.exception.UnauthorizedException;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
import com.secureiq.SecureIQ.exam.dto.*;
import com.secureiq.SecureIQ.exam.mapper.ExamMapper;
import com.secureiq.SecureIQ.exam.model.Exam;
import com.secureiq.SecureIQ.exam.model.ExamStatus;
import com.secureiq.SecureIQ.exam.model.ExamType;
import com.secureiq.SecureIQ.exam.repository.ExamRepository;
import com.secureiq.SecureIQ.faculty.model.Faculty;
import com.secureiq.SecureIQ.faculty.repository.FacultyRepository;
import com.secureiq.SecureIQ.student.model.Student;
import com.secureiq.SecureIQ.student.repository.StudentRepository;
import com.secureiq.SecureIQ.subject.model.Subject;
import com.secureiq.SecureIQ.common.config.DateTimeConfig;
import com.secureiq.SecureIQ.examattempt.model.AttemptStatus;
import com.secureiq.SecureIQ.examattempt.model.StudentExamAttempt;
import com.secureiq.SecureIQ.examattempt.repository.StudentExamAttemptRepository;
import com.secureiq.SecureIQ.subject.repository.SubjectRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final SubjectRepository subjectRepository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;
    private final StudentExamAttemptRepository attemptRepository;
    private final ExamMapper examMapper;
    private final Clock clock;

    public ExamServiceImpl(ExamRepository examRepository,
                           SubjectRepository subjectRepository,
                           FacultyRepository facultyRepository,
                           DepartmentRepository departmentRepository,
                           StudentRepository studentRepository,
                           StudentExamAttemptRepository attemptRepository,
                           ExamMapper examMapper,
                           Clock clock) {
        this.examRepository = examRepository;
        this.subjectRepository = subjectRepository;
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
        this.studentRepository = studentRepository;
        this.attemptRepository = attemptRepository;
        this.examMapper = examMapper;
        this.clock = clock;
    }

    @Override
    public Page<ExamResponse> getAll(String examTitle, Long subjectId, Long facultyId, Long departmentId, ExamStatus status, Pageable pageable) {
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
            return examRepository.findAllFiltered(examTitle, subjectId, facultyId, departmentId, status, pageable)
                    .map(e -> toEnrichedResponse(e, principal, isStudent));
        }

        if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            if (departmentId != null && !departmentId.equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only view exams in their own department");
            }
            return examRepository.findAllFilteredForHOD(dept.getId(), examTitle, subjectId, facultyId, status, pageable)
                    .map(e -> toEnrichedResponse(e, principal, isStudent));
        }

        if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new NotFoundException("Faculty profile not found for user: " + principal.getEmail()));
            if (facultyId != null && !facultyId.equals(faculty.getId())) {
                throw new AccessDeniedException("Access denied: Faculty can only view their own exams");
            }
            return examRepository.findAllFilteredForFaculty(faculty.getId(), examTitle, subjectId, departmentId, status, pageable)
                    .map(e -> toEnrichedResponse(e, principal, isStudent));
        }

        if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Logged in user is not registered as a student"));
            if (departmentId != null && !departmentId.equals(student.getDepartment().getId())) {
                throw new AccessDeniedException("Access denied: Student can only view exams in their own department");
            }
            return examRepository.findAllFilteredForStudent(student.getDepartment().getId(), student.getSemester(), examTitle, subjectId, facultyId, status, pageable)
                    .map(e -> toEnrichedResponse(e, principal, isStudent));
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }

    @Override
    public ExamResponse getById(Long id) {
        Exam exam = getEntityById(id);
        checkExamReadAccess(exam);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User principal = auth != null && auth.getPrincipal() instanceof User ? (User) auth.getPrincipal() : null;
        boolean isStudent = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        return toEnrichedResponse(exam, principal, isStudent);
    }

    @Override
    @Transactional
    public ExamResponse create(ExamCreateRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) auth.getPrincipal();
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        validateExamBusinessRules(
                request.getExamCode(),
                null,
                request.getTotalMarks(),
                request.getPassingMarks(),
                request.getDurationMinutes(),
                request.getScheduledDate(),
                request.getScheduledEndDate(),
                request.getStartTime(),
                request.getEndTime()
        );

        if (examRepository.existsByExamCode(request.getExamCode())) {
            throw new ConflictException("Exam Code is already registered");
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new NotFoundException("Subject not found with id: " + request.getSubjectId()));

        Faculty faculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() -> new NotFoundException("Faculty not found with id: " + request.getFacultyId()));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + request.getDepartmentId()));

        if (isHod && !isAdmin) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            if (!request.getDepartmentId().equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only create exams within their own department");
            }
        }

        if (isFaculty && !isAdmin && !isHod) {
            Faculty currentFaculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new NotFoundException("Faculty profile not found for user: " + principal.getEmail()));
            if (!request.getFacultyId().equals(currentFaculty.getId())) {
                throw new AccessDeniedException("Access denied: Faculty can only create exams for themselves");
            }
        }

        Exam exam = examMapper.toEntity(request, subject, faculty, department);
        Exam savedExam = examRepository.save(exam);
        return toEnrichedResponse(savedExam, principal, false);
    }

    @Override
    @Transactional
    public ExamResponse update(Long id, ExamUpdateRequest request) {
        Exam exam = getEntityById(id);
        checkExamWriteAccess(exam);

        validateExamBusinessRules(
                request.getExamCode(),
                id,
                request.getTotalMarks(),
                request.getPassingMarks(),
                request.getDurationMinutes(),
                request.getScheduledDate(),
                request.getScheduledEndDate(),
                request.getStartTime(),
                request.getEndTime()
        );

        if (!exam.getExamCode().equalsIgnoreCase(request.getExamCode()) && examRepository.existsByExamCode(request.getExamCode())) {
            throw new ConflictException("Exam Code is already registered");
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new NotFoundException("Subject not found with id: " + request.getSubjectId()));

        Faculty faculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() -> new NotFoundException("Faculty not found with id: " + request.getFacultyId()));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + request.getDepartmentId()));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) auth.getPrincipal();
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isHod && !isAdmin) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            if (!request.getDepartmentId().equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only update exams to their own department");
            }
        }

        if (isFaculty && !isAdmin && !isHod) {
            Faculty currentFaculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new NotFoundException("Faculty profile not found for user: " + principal.getEmail()));
            if (!request.getFacultyId().equals(currentFaculty.getId())) {
                throw new AccessDeniedException("Access denied: Faculty can only update exams assigned to themselves");
            }
        }

        examMapper.updateEntity(request, subject, faculty, department, exam);
        Exam savedExam = examRepository.save(exam);
        return toEnrichedResponse(savedExam, principal, false);
    }

    @Override
    @Transactional
    public ExamResponse patch(Long id, ExamPatchRequest request) {
        Exam exam = getEntityById(id);
        checkExamWriteAccess(exam);

        String code = request.getExamCode() != null ? request.getExamCode() : exam.getExamCode();
        Integer tMarks = request.getTotalMarks() != null ? request.getTotalMarks() : exam.getTotalMarks();
        Integer pMarks = request.getPassingMarks() != null ? request.getPassingMarks() : exam.getPassingMarks();
        Integer duration = request.getDurationMinutes() != null ? request.getDurationMinutes() : exam.getDurationMinutes();
        LocalDate date = request.getScheduledDate() != null ? request.getScheduledDate() : exam.getScheduledDate();
        LocalDate eDate = request.getScheduledEndDate() != null ? request.getScheduledEndDate() : exam.getScheduledEndDate();
        LocalTime sTime = request.getStartTime() != null ? request.getStartTime() : exam.getStartTime();
        LocalTime eTime = request.getEndTime() != null ? request.getEndTime() : exam.getEndTime();

        validateExamBusinessRules(code, id, tMarks, pMarks, duration, date, eDate, sTime, eTime);

        if (request.getExamCode() != null && !exam.getExamCode().equalsIgnoreCase(request.getExamCode()) && examRepository.existsByExamCode(request.getExamCode())) {
            throw new ConflictException("Exam Code is already registered");
        }

        Subject subject = null;
        if (request.getSubjectId() != null) {
            subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new NotFoundException("Subject not found with id: " + request.getSubjectId()));
        }

        Faculty faculty = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) auth.getPrincipal();
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (request.getFacultyId() != null) {
            faculty = facultyRepository.findById(request.getFacultyId())
                    .orElseThrow(() -> new NotFoundException("Faculty not found with id: " + request.getFacultyId()));

            if (isFaculty && !isAdmin && !isHod) {
                Faculty currentFaculty = facultyRepository.findByUserId(principal.getId())
                        .orElseThrow(() -> new NotFoundException("Faculty profile not found for user: " + principal.getEmail()));
                if (!request.getFacultyId().equals(currentFaculty.getId())) {
                    throw new AccessDeniedException("Access denied: Faculty can only patch exams to themselves");
                }
            }
        }

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new NotFoundException("Department not found with id: " + request.getDepartmentId()));

            if (isHod && !isAdmin) {
                Department dept = departmentRepository.findByHodId(principal.getId())
                        .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
                if (!request.getDepartmentId().equals(dept.getId())) {
                    throw new AccessDeniedException("Access denied: HOD can only patch exams within their own department");
                }
            }
        }

        examMapper.patchEntity(request, subject, faculty, department, exam);
        Exam savedExam = examRepository.save(exam);
        return toEnrichedResponse(savedExam, principal, false);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Exam exam = getEntityById(id);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        User principal = (User) auth.getPrincipal();

        if (isHod && !isAdmin) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            if (exam.getDepartment() == null || !exam.getDepartment().getId().equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only delete exams in their own department");
            }
        } else if (isFaculty && !isAdmin && !isHod) {
            Faculty currentFaculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new NotFoundException("Faculty profile not found for user: " + principal.getEmail()));
            if (exam.getFaculty() == null || !exam.getFaculty().getId().equals(currentFaculty.getId())) {
                throw new AccessDeniedException("Access denied: Faculty can only delete their own exams");
            }
        } else if (!isAdmin) {
            throw new AccessDeniedException("Access denied: Unauthorized role for deleting exams");
        }

        examRepository.delete(exam);
    }

    @Override
    public List<ExamResponse> getUpcomingExams() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        User principal = (User) auth.getPrincipal();

        LocalDate today = LocalDate.now(clock);
        List<Exam> exams;

        if (isAdmin) {
            exams = examRepository.findByScheduledDateAfterOrderByScheduledDateAscStartTimeAsc(today);
        } else if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            exams = examRepository.findByDepartmentIdAndScheduledDateAfterOrderByScheduledDateAsc(dept.getId(), today);
        } else if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new NotFoundException("Faculty profile not found for user: " + principal.getEmail()));
            exams = examRepository.findByFacultyIdAndScheduledDateAfterOrderByScheduledDateAscStartTimeAsc(faculty.getId(), today);
        } else if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Logged in user is not registered as a student"));
            exams = examRepository.findByDepartmentIdAndSemesterAndScheduledDateAfterOrderByScheduledDateAscStartTimeAsc(
                    student.getDepartment().getId(), 
                    student.getSemester(), 
                    today
            );
        } else {
            exams = List.of();
        }

        return exams.stream().map(e -> toEnrichedResponse(e, principal, isStudent)).toList();
    }

    @Override
    public List<ExamResponse> getTodaysExams() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        User principal = (User) auth.getPrincipal();

        LocalDate today = LocalDate.now(clock);
        List<Exam> exams;

        if (isAdmin) {
            exams = examRepository.findByScheduledDateOrderByStartTimeAsc(today);
        } else if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            exams = examRepository.findByScheduledDateOrderByStartTimeAsc(today).stream()
                    .filter(e -> e.getDepartment() != null && e.getDepartment().getId().equals(dept.getId()))
                    .toList();
        } else if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new NotFoundException("Faculty profile not found for user: " + principal.getEmail()));
            exams = examRepository.findByFacultyIdAndScheduledDateOrderByStartTimeAsc(faculty.getId(), today);
        } else if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Logged in user is not registered as a student"));
            exams = examRepository.findByDepartmentIdAndSemesterAndScheduledDateOrderByStartTimeAsc(
                    student.getDepartment().getId(), 
                    student.getSemester(), 
                    today
            );
        } else {
            exams = List.of();
        }

        return exams.stream().map(e -> toEnrichedResponse(e, principal, isStudent)).toList();
    }

    @Override
    public List<ExamResponse> getActiveExams() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        User principal = (User) auth.getPrincipal();

        List<Exam> exams = examRepository.findByStatus(ExamStatus.ACTIVE);

        if (isAdmin) {
            // no extra filter
        } else if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            exams = exams.stream().filter(e -> e.getDepartment() != null && e.getDepartment().getId().equals(dept.getId())).toList();
        } else if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new NotFoundException("Faculty profile not found for user: " + principal.getEmail()));
            exams = exams.stream().filter(e -> e.getFaculty() != null && e.getFaculty().getId().equals(faculty.getId())).toList();
        } else if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Logged in user is not registered as a student"));
            exams = exams.stream().filter(e -> e.getDepartment() != null && e.getDepartment().getId().equals(student.getDepartment().getId()) 
                                            && e.getSemester() != null && e.getSemester().equals(student.getSemester())).toList();
        } else {
            exams = List.of();
        }

        return exams.stream().map(e -> toEnrichedResponse(e, principal, isStudent)).toList();
    }

    @Override
    public ExamDashboardResponse getDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        User principal = (User) auth.getPrincipal();

        LocalDate today = LocalDate.now(clock);
        ExamDashboardResponse.ExamDashboardResponseBuilder builder = ExamDashboardResponse.builder();

        if (isAdmin || isHod) {
            builder.role(isAdmin ? "ADMIN" : "HOD");
            
            List<Exam> allExams;
            if (isAdmin) {
                allExams = examRepository.findAll();
            } else {
                Department dept = departmentRepository.findByHodId(principal.getId())
                        .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
                allExams = examRepository.findAllFilteredForHOD(dept.getId(), null, null, null, null, Pageable.unpaged()).getContent();
            }

            long activeCount = allExams.stream().filter(e -> e.getStatus() == ExamStatus.ACTIVE).count();
            long completedCount = allExams.stream().filter(e -> e.getStatus() == ExamStatus.COMPLETED).count();

            Map<String, Long> byType = allExams.stream()
                    .collect(Collectors.groupingBy(e -> e.getExamType().name(), Collectors.counting()));
            
            Map<String, Long> byStatus = allExams.stream()
                    .collect(Collectors.groupingBy(e -> e.getStatus().name(), Collectors.counting()));

            builder.totalExams((long) allExams.size())
                    .activeExamsCount(activeCount)
                    .completedExamsCount(completedCount)
                    .examsByType(byType)
                    .examsByStatus(byStatus);

        } else if (isFaculty) {
            builder.role("FACULTY");
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new NotFoundException("Faculty profile not found for user: " + principal.getEmail()));

            List<Exam> created = examRepository.findAllFilteredForFaculty(faculty.getId(), null, null, null, null, Pageable.unpaged()).getContent();
            List<Exam> scheduled = examRepository.findByFacultyIdAndScheduledDateAfterOrderByScheduledDateAscStartTimeAsc(faculty.getId(), today);

            builder.createdExams(created.stream().map(e -> toEnrichedResponse(e, principal, false)).toList())
                    .scheduledExams(scheduled.stream().map(e -> toEnrichedResponse(e, principal, false)).toList());

        } else if (isStudent) {
            builder.role("STUDENT");
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Logged in user is not registered as a student"));

            List<Exam> upcoming = examRepository.findByDepartmentIdAndSemesterAndScheduledDateAfterOrderByScheduledDateAscStartTimeAsc(
                    student.getDepartment().getId(), 
                    student.getSemester(), 
                    today
            );
            List<Exam> todays = examRepository.findByDepartmentIdAndSemesterAndScheduledDateOrderByStartTimeAsc(
                    student.getDepartment().getId(), 
                    student.getSemester(), 
                    today
            );

            builder.upcomingExams(upcoming.stream().map(e -> toEnrichedResponse(e, principal, true)).toList())
                    .todaysExams(todays.stream().map(e -> toEnrichedResponse(e, principal, true)).toList());
        }

        return builder.build();
    }

    private Exam getEntityById(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Exam not found with id: " + id));
    }

    private void validateExamBusinessRules(String examCode, Long id, Integer totalMarks, Integer passingMarks, Integer durationMinutes, LocalDate scheduledDate, LocalDate scheduledEndDate, LocalTime startTime, LocalTime endTime) {
        if (scheduledDate == null) {
            throw new BadRequestException("Scheduled start date cannot be missing");
        }
        if (startTime == null) {
            throw new BadRequestException("Start time cannot be missing");
        }
        if (endTime == null) {
            throw new BadRequestException("End time cannot be missing");
        }
        if (passingMarks != null && totalMarks != null && passingMarks > totalMarks) {
            throw new BadRequestException("Passing marks cannot exceed total marks");
        }
        if (durationMinutes != null && durationMinutes <= 0) {
            throw new BadRequestException("Duration must be positive");
        }
        LocalDate effectiveEndDate = scheduledEndDate != null ? scheduledEndDate : scheduledDate;
        LocalDate today = LocalDate.now(clock);
        if (scheduledDate.isBefore(today)) {
            throw new BadRequestException("Scheduled date cannot be in the past");
        }
        LocalDateTime startLdt = LocalDateTime.of(scheduledDate, startTime);
        LocalDateTime endLdt = LocalDateTime.of(effectiveEndDate, endTime);
        if (!startLdt.isBefore(endLdt)) {
            throw new BadRequestException("Start time must be before end time");
        }
        if (durationMinutes != null) {
            Instant startInstant = startLdt.atZone(DateTimeConfig.INSTITUTION_ZONE).toInstant();
            Instant endInstant = endLdt.atZone(DateTimeConfig.INSTITUTION_ZONE).toInstant();
            long windowMinutes = Duration.between(startInstant, endInstant).toMinutes();
            if (durationMinutes > windowMinutes) {
                throw new BadRequestException("Exam duration cannot exceed the scheduled window (" + windowMinutes + " minutes)");
            }
        }
    }

    private ExamResponse toEnrichedResponse(Exam exam, User principal, boolean isStudent) {
        ExamResponse res = examMapper.toResponse(exam);
        Instant now = clock.instant();
        res.setServerTime(now);

        Instant start = exam.getAuthoritativeStartInstant();
        Instant end = exam.getAuthoritativeEndInstant();

        if (exam.getStatus() == ExamStatus.CANCELLED) {
            res.setScheduleState("CANCELLED");
            res.setCanStart(false);
            res.setAccessMessage("This exam has been cancelled.");
            return res;
        }

        if (exam.getStatus() == ExamStatus.DRAFT) {
            res.setScheduleState("DRAFT");
            res.setCanStart(false);
            res.setAccessMessage("This exam is currently in draft.");
            return res;
        }

        if (start != null && now.isBefore(start)) {
            res.setScheduleState("UPCOMING");
            res.setCanStart(false);
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("en-IN"));
            DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a", Locale.forLanguageTag("en-IN"));
            ZonedDateTime zdt = start.atZone(DateTimeConfig.INSTITUTION_ZONE);
            res.setAccessMessage("This exam is not available yet. The exam starts on " + zdt.format(dateFmt) + " at " + zdt.format(timeFmt) + ".");
            return res;
        }

        if (end != null && !now.isBefore(end)) {
            res.setScheduleState("EXPIRED");
            res.setCanStart(false);
            res.setAccessMessage("The scheduled exam has ended.");
            return res;
        }

        // Current time is within [start, end)
        if (isStudent && principal != null) {
            Student student = studentRepository.findByUserId(principal.getId()).orElse(null);
            if (student != null) {
                boolean matchesDept = exam.getDepartment() == null || exam.getDepartment().getId().equals(student.getDepartment().getId());
                boolean matchesSem = exam.getSemester() == null || exam.getSemester().equals(student.getSemester());
                if (!matchesDept || !matchesSem) {
                    res.setScheduleState("UNAUTHORIZED");
                    res.setCanStart(false);
                    res.setAccessMessage("You are not authorized to attend this exam.");
                    return res;
                }

                Optional<StudentExamAttempt> optAttempt = attemptRepository.findFirstByStudentIdAndExamId(student.getId(), exam.getId());
                if (optAttempt.isPresent()) {
                    StudentExamAttempt att = optAttempt.get();
                    res.setAttemptId(att.getId());
                    res.setAttemptStatus(att.getStatus().name());

                    if (att.getStatus() == AttemptStatus.SUBMITTED || att.getStatus() == AttemptStatus.AUTO_SUBMITTED) {
                        res.setScheduleState("COMPLETED");
                        res.setCanStart(false);
                        res.setAccessMessage(att.getStatus() == AttemptStatus.AUTO_SUBMITTED
                                ? "Your exam was auto-submitted."
                                : "You have already completed this exam.");
                        return res;
                    }
                    if (att.getStatus() == AttemptStatus.TERMINATED) {
                        res.setScheduleState("TERMINATED");
                        res.setCanStart(false);
                        res.setAccessMessage("Your exam attempt has been terminated.");
                        return res;
                    }
                    if (att.getStatus() == AttemptStatus.IN_PROGRESS) {
                        Instant effectiveDeadline = att.getAuthoritativeEndTime();
                        if (effectiveDeadline == null) {
                            effectiveDeadline = end;
                        }
                        if (effectiveDeadline != null && !now.isBefore(effectiveDeadline)) {
                            res.setScheduleState("EXPIRED");
                            res.setCanStart(false);
                            res.setAccessMessage("Your exam attempt has expired.");
                            return res;
                        }
                        long remainingSeconds = effectiveDeadline != null ? Math.max(0, Duration.between(now, effectiveDeadline).toSeconds()) : 0;
                        res.setRemainingSeconds(remainingSeconds);
                        res.setScheduleState("IN_PROGRESS");
                        res.setCanStart(true);
                        res.setAccessMessage("Your exam attempt is in progress. You can resume your exam.");
                        return res;
                    }
                }

                res.setScheduleState("AVAILABLE");
                res.setCanStart(true);
                res.setAccessMessage("Exam is available. You can start the exam.");
                return res;
            }
        }

        res.setScheduleState("AVAILABLE");
        res.setCanStart(true);
        res.setAccessMessage("Exam window is currently active.");
        return res;
    }

    private void checkExamReadAccess(Exam exam) {
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
            if (exam.getDepartment() == null || !exam.getDepartment().getId().equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only view exams in their own department");
            }
            return;
        }

        if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new NotFoundException("Faculty profile not found for user: " + principal.getEmail()));
            if (exam.getFaculty() == null || !exam.getFaculty().getId().equals(faculty.getId())) {
                throw new AccessDeniedException("Access denied: Faculty can only view their own exams");
            }
            return;
        }

        if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Logged in user is not registered as a student"));
            if (exam.getDepartment() == null || !exam.getDepartment().getId().equals(student.getDepartment().getId()) ||
                    exam.getSemester() == null || !exam.getSemester().equals(student.getSemester())) {
                throw new AccessDeniedException("Access denied: Student can only view exams in their own department and semester");
            }
            if (exam.getStatus() == ExamStatus.DRAFT) {
                throw new AccessDeniedException("Access denied: Draft exams are not available to students");
            }
            return;
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }

    private void checkExamWriteAccess(Exam exam) {
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
            if (exam.getDepartment() == null || !exam.getDepartment().getId().equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only modify exams in their own department");
            }
            return;
        }

        if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new NotFoundException("Faculty profile not found for user: " + principal.getEmail()));
            if (exam.getFaculty() == null || !exam.getFaculty().getId().equals(faculty.getId())) {
                throw new AccessDeniedException("Access denied: Faculty can only modify their own exams");
            }
            return;
        }

        throw new AccessDeniedException("Access denied: Unauthorized role for writing exams");
    }
}
