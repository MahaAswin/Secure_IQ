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
import com.secureiq.SecureIQ.subject.repository.SubjectRepository;
import com.secureiq.SecureIQ.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
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
    private final ExamMapper examMapper;

    public ExamServiceImpl(ExamRepository examRepository,
                           SubjectRepository subjectRepository,
                           FacultyRepository facultyRepository,
                           DepartmentRepository departmentRepository,
                           StudentRepository studentRepository,
                           ExamMapper examMapper) {
        this.examRepository = examRepository;
        this.subjectRepository = subjectRepository;
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
        this.studentRepository = studentRepository;
        this.examMapper = examMapper;
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
            return examRepository.findAllFiltered(examTitle, subjectId, facultyId, departmentId, status, pageable).map(examMapper::toResponse);
        }

        if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            if (departmentId != null && !departmentId.equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only view exams in their own department");
            }
            return examRepository.findAllFilteredForHOD(dept.getId(), examTitle, subjectId, facultyId, status, pageable).map(examMapper::toResponse);
        }

        if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new NotFoundException("Faculty profile not found for user: " + principal.getEmail()));
            if (facultyId != null && !facultyId.equals(faculty.getId())) {
                throw new AccessDeniedException("Access denied: Faculty can only view their own exams");
            }
            return examRepository.findAllFilteredForFaculty(faculty.getId(), examTitle, subjectId, departmentId, status, pageable).map(examMapper::toResponse);
        }

        if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Logged in user is not registered as a student"));
            if (departmentId != null && !departmentId.equals(student.getDepartment().getId())) {
                throw new AccessDeniedException("Access denied: Student can only view exams in their own department");
            }
            return examRepository.findAllFilteredForStudent(student.getDepartment().getId(), student.getSemester(), examTitle, subjectId, facultyId, status, pageable).map(examMapper::toResponse);
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }

    @Override
    public ExamResponse getById(Long id) {
        Exam exam = getEntityById(id);
        checkExamReadAccess(exam);
        return examMapper.toResponse(exam);
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
        return examMapper.toResponse(savedExam);
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
        return examMapper.toResponse(savedExam);
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
        LocalTime sTime = request.getStartTime() != null ? request.getStartTime() : exam.getStartTime();
        LocalTime eTime = request.getEndTime() != null ? request.getEndTime() : exam.getEndTime();

        validateExamBusinessRules(code, id, tMarks, pMarks, duration, date, sTime, eTime);

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
        return examMapper.toResponse(savedExam);
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

        List<Exam> exams;

        if (isAdmin) {
            exams = examRepository.findByScheduledDateAfterOrderByScheduledDateAscStartTimeAsc(LocalDate.now());
        } else if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            exams = examRepository.findByDepartmentIdAndScheduledDateAfterOrderByScheduledDateAsc(dept.getId(), LocalDate.now());
        } else if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new NotFoundException("Faculty profile not found for user: " + principal.getEmail()));
            exams = examRepository.findByFacultyIdAndScheduledDateAfterOrderByScheduledDateAscStartTimeAsc(faculty.getId(), LocalDate.now());
        } else if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Logged in user is not registered as a student"));
            exams = examRepository.findByDepartmentIdAndSemesterAndScheduledDateAfterOrderByScheduledDateAscStartTimeAsc(
                    student.getDepartment().getId(), 
                    student.getSemester(), 
                    LocalDate.now()
            );
        } else {
            exams = List.of();
        }

        return exams.stream().map(examMapper::toResponse).toList();
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

        List<Exam> exams;

        if (isAdmin) {
            exams = examRepository.findByScheduledDateOrderByStartTimeAsc(LocalDate.now());
        } else if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            // For HOD, return all CSE exams scheduled today
            exams = examRepository.findByScheduledDateOrderByStartTimeAsc(LocalDate.now()).stream()
                    .filter(e -> e.getDepartment() != null && e.getDepartment().getId().equals(dept.getId()))
                    .toList();
        } else if (isFaculty) {
            Faculty faculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new NotFoundException("Faculty profile not found for user: " + principal.getEmail()));
            exams = examRepository.findByFacultyIdAndScheduledDateOrderByStartTimeAsc(faculty.getId(), LocalDate.now());
        } else if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Logged in user is not registered as a student"));
            exams = examRepository.findByDepartmentIdAndSemesterAndScheduledDateOrderByStartTimeAsc(
                    student.getDepartment().getId(), 
                    student.getSemester(), 
                    LocalDate.now()
            );
        } else {
            exams = List.of();
        }

        return exams.stream().map(examMapper::toResponse).toList();
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

        return exams.stream().map(examMapper::toResponse).toList();
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
            List<Exam> scheduled = examRepository.findByFacultyIdAndScheduledDateAfterOrderByScheduledDateAscStartTimeAsc(faculty.getId(), LocalDate.now());

            builder.createdExams(created.stream().map(examMapper::toResponse).toList())
                    .scheduledExams(scheduled.stream().map(examMapper::toResponse).toList());

        } else if (isStudent) {
            builder.role("STUDENT");
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Logged in user is not registered as a student"));

            List<Exam> upcoming = examRepository.findByDepartmentIdAndSemesterAndScheduledDateAfterOrderByScheduledDateAscStartTimeAsc(
                    student.getDepartment().getId(), 
                    student.getSemester(), 
                    LocalDate.now()
            );
            List<Exam> todays = examRepository.findByDepartmentIdAndSemesterAndScheduledDateOrderByStartTimeAsc(
                    student.getDepartment().getId(), 
                    student.getSemester(), 
                    LocalDate.now()
            );

            builder.upcomingExams(upcoming.stream().map(examMapper::toResponse).toList())
                    .todaysExams(todays.stream().map(examMapper::toResponse).toList());
        }

        return builder.build();
    }

    private Exam getEntityById(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Exam not found with id: " + id));
    }

    private void validateExamBusinessRules(String examCode, Long id, Integer totalMarks, Integer passingMarks, Integer durationMinutes, LocalDate scheduledDate, LocalTime startTime, LocalTime endTime) {
        if (passingMarks > totalMarks) {
            throw new BadRequestException("Passing marks cannot exceed total marks");
        }
        if (durationMinutes <= 0) {
            throw new BadRequestException("Duration must be positive");
        }
        if (scheduledDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Scheduled date cannot be in the past");
        }
        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new BadRequestException("Start time must be before end time");
        }
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
