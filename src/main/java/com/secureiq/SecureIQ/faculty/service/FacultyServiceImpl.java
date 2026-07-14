package com.secureiq.SecureIQ.faculty.service;

import com.secureiq.SecureIQ.common.exception.ConflictException;
import com.secureiq.SecureIQ.common.exception.NotFoundException;
import com.secureiq.SecureIQ.common.exception.UnauthorizedException;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
import com.secureiq.SecureIQ.faculty.dto.*;
import com.secureiq.SecureIQ.faculty.mapper.FacultyMapper;
import com.secureiq.SecureIQ.faculty.model.Faculty;
import com.secureiq.SecureIQ.faculty.repository.FacultyRepository;
import com.secureiq.SecureIQ.student.dto.RecentActivityResponse;
import com.secureiq.SecureIQ.student.dto.StudentResponse;
import com.secureiq.SecureIQ.student.mapper.StudentMapper;
import com.secureiq.SecureIQ.exam.model.Exam;
import com.secureiq.SecureIQ.student.model.RecentActivity;
import com.secureiq.SecureIQ.student.model.Student;
import com.secureiq.SecureIQ.exam.repository.ExamRepository;
import com.secureiq.SecureIQ.student.repository.RecentActivityRepository;
import com.secureiq.SecureIQ.student.repository.StudentRepository;
import com.secureiq.SecureIQ.subject.model.Subject;
import com.secureiq.SecureIQ.subject.repository.SubjectRepository;
import com.secureiq.SecureIQ.user.model.User;
import com.secureiq.SecureIQ.user.model.Role;
import com.secureiq.SecureIQ.user.repository.UserRepository;
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
public class FacultyServiceImpl implements FacultyService {

    private final FacultyRepository facultyRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final ExamRepository examRepository;
    private final RecentActivityRepository recentActivityRepository;
    private final FacultyMapper facultyMapper;
    private final StudentMapper studentMapper;

    public FacultyServiceImpl(FacultyRepository facultyRepository,
                              UserRepository userRepository,
                              DepartmentRepository departmentRepository,
                              SubjectRepository subjectRepository,
                              StudentRepository studentRepository,
                              ExamRepository examRepository,
                              RecentActivityRepository recentActivityRepository,
                              FacultyMapper facultyMapper,
                              StudentMapper studentMapper) {
        this.facultyRepository = facultyRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.subjectRepository = subjectRepository;
        this.studentRepository = studentRepository;
        this.examRepository = examRepository;
        this.recentActivityRepository = recentActivityRepository;
        this.facultyMapper = facultyMapper;
        this.studentMapper = studentMapper;
    }

    @Override
    public Page<FacultyResponse> getAll(String name, String employeeId, Long departmentId, String specialization, Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        User principal = (User) auth.getPrincipal();

        if (isHod && !isAdmin) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));

            if (departmentId != null && !departmentId.equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only view faculty in their own department");
            }
            departmentId = dept.getId();
        } else if (isStudent && !isAdmin) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Logged in user is not registered as a student"));
            
            if (student.getDepartment() != null) {
                if (departmentId != null && !departmentId.equals(student.getDepartment().getId())) {
                    throw new AccessDeniedException("Access denied: Student can only view faculty in their own department");
                }
                departmentId = student.getDepartment().getId();
            } else {
                departmentId = -1L; // Invalid department id to force empty results
            }
        } else if (isFaculty && !isAdmin && !isHod) {
            Faculty fac = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new NotFoundException("Faculty profile not found for user: " + principal.getEmail()));
            
            if (fac.getDepartment() != null) {
                if (departmentId != null && !departmentId.equals(fac.getDepartment().getId())) {
                    throw new AccessDeniedException("Access denied: Faculty can only view faculty in their own department");
                }
                departmentId = fac.getDepartment().getId();
            } else {
                departmentId = -1L;
            }
        }

        Page<Faculty> faculties = facultyRepository.findAllFiltered(name, employeeId, departmentId, specialization, pageable);
        return faculties.map(facultyMapper::toResponse);
    }

    @Override
    public FacultyResponse getById(Long id) {
        Faculty faculty = getEntityById(id);
        checkFacultyReadAccess(faculty);
        return facultyMapper.toResponse(faculty);
    }

    @Override
    public FacultyResponse getProfileByCurrentUser() {
        User currentUser = getCurrentUser();
        Faculty faculty = facultyRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Faculty profile not found for user: " + currentUser.getEmail()));
        return facultyMapper.toResponse(faculty);
    }

    @Override
    @Transactional
    public FacultyResponse create(FacultyCreateRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) auth.getPrincipal();
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isHod && !isAdmin) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            if (!request.getDepartmentId().equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only create faculty in their own department");
            }
        }

        if (facultyRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new ConflictException("Employee ID is already registered");
        }
        if (facultyRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new ConflictException("Faculty profile is already linked to this user");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found with id: " + request.getUserId()));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + request.getDepartmentId()));

        Set<Subject> subjects = resolveSubjects(request.getSubjectIds());

        Faculty faculty = facultyMapper.toEntity(request, user, department, subjects);
        Faculty savedFaculty = facultyRepository.save(faculty);
        return facultyMapper.toResponse(savedFaculty);
    }

    @Override
    @Transactional
    public FacultyResponse update(Long id, FacultyUpdateRequest request) {
        Faculty faculty = getEntityById(id);
        checkFacultyWriteAccess(faculty);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) auth.getPrincipal();
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isHod && !isAdmin) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            if (!request.getDepartmentId().equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only update faculty to their own department");
            }
        }

        if (!faculty.getEmployeeId().equalsIgnoreCase(request.getEmployeeId()) &&
                facultyRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new ConflictException("Employee ID is already registered");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + request.getDepartmentId()));

        Set<Subject> subjects = resolveSubjects(request.getSubjectIds());

        facultyMapper.updateEntity(request, department, subjects, faculty);
        Faculty savedFaculty = facultyRepository.save(faculty);
        return facultyMapper.toResponse(savedFaculty);
    }

    @Override
    @Transactional
    public FacultyResponse patch(Long id, FacultyPatchRequest request) {
        Faculty faculty = getEntityById(id);
        checkFacultyWriteAccess(faculty);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) auth.getPrincipal();
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (request.getEmployeeId() != null &&
                !faculty.getEmployeeId().equalsIgnoreCase(request.getEmployeeId()) &&
                facultyRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new ConflictException("Employee ID is already registered");
        }

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new NotFoundException("Department not found with id: " + request.getDepartmentId()));

            if (isHod && !isAdmin) {
                Department dept = departmentRepository.findByHodId(principal.getId())
                        .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
                if (!request.getDepartmentId().equals(dept.getId())) {
                    throw new AccessDeniedException("Access denied: HOD can only change faculty to their own department");
                }
            }
        }

        Set<Subject> subjects = null;
        if (request.getSubjectIds() != null) {
            subjects = resolveSubjects(request.getSubjectIds());
        }

        facultyMapper.patchEntity(request, department, subjects, faculty);
        Faculty savedFaculty = facultyRepository.save(faculty);
        return facultyMapper.toResponse(savedFaculty);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Faculty faculty = getEntityById(id);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        User principal = (User) auth.getPrincipal();

        if (isHod && !isAdmin) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            if (faculty.getDepartment() == null || !faculty.getDepartment().getId().equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only delete faculty in their own department");
            }
        } else if (!isAdmin) {
            throw new AccessDeniedException("Access denied: Only Admin and HOD roles can delete faculty");
        }

        facultyRepository.delete(faculty);
    }

    @Override
    public List<FacultyResponse.SubjectDto> getAssignedSubjects() {
        Faculty faculty = getFacultyProfileForCurrentUser();
        if (faculty.getSubjects() == null) {
            return List.of();
        }
        return faculty.getSubjects().stream()
                .map(sub -> FacultyResponse.SubjectDto.builder()
                        .id(sub.getId())
                        .subjectCode(sub.getSubjectCode())
                        .subjectName(sub.getSubjectName())
                        .credits(sub.getCredits())
                        .semester(sub.getSemester())
                        .build())
                .toList();
    }

    @Override
    public List<StudentResponse> getAssignedStudents() {
        Faculty faculty = getFacultyProfileForCurrentUser();
        List<Student> students = new ArrayList<>();

        if (faculty.getSubjects() != null && !faculty.getSubjects().isEmpty()) {
            Set<Long> deptIds = new HashSet<>();
            Set<Integer> semesters = new HashSet<>();
            for (Subject sub : faculty.getSubjects()) {
                if (sub.getDepartment() != null) {
                    deptIds.add(sub.getDepartment().getId());
                }
                if (sub.getSemester() != null) {
                    semesters.add(sub.getSemester());
                }
            }
            if (!deptIds.isEmpty() && !semesters.isEmpty()) {
                students = studentRepository.findByDepartmentIdInAndSemesterIn(deptIds, semesters);
            }
        }

        if (students.isEmpty() && faculty.getDepartment() != null) {
            students = studentRepository.findAllByDepartmentId(faculty.getDepartment().getId());
        }

        return students.stream()
                .map(studentMapper::toResponse)
                .toList();
    }

    @Override
    public List<FacultyDashboardResponse.ExamDto> getUpcomingExams() {
        Faculty faculty = getFacultyProfileForCurrentUser();
        Set<Long> deptIds = new HashSet<>();
        if (faculty.getDepartment() != null) {
            deptIds.add(faculty.getDepartment().getId());
        }
        if (faculty.getSubjects() != null) {
            for (Subject s : faculty.getSubjects()) {
                if (s.getDepartment() != null) {
                    deptIds.add(s.getDepartment().getId());
                }
            }
        }

        if (deptIds.isEmpty()) {
            return List.of();
        }

        List<Exam> exams = examRepository.findByDepartmentIdInAndScheduledDateAfterOrderByScheduledDateAsc(deptIds, java.time.LocalDate.now());
        return exams.stream()
                .map(exam -> FacultyDashboardResponse.ExamDto.builder()
                        .id(exam.getId())
                        .title(exam.getExamTitle())
                        .scheduledAt(exam.getScheduledDate().toString())
                        .departmentName(exam.getDepartment() != null ? exam.getDepartment().getDepartmentName() : null)
                        .build())
                .toList();
    }

    @Override
    public List<RecentActivityResponse> getRecentActivities() {
        List<StudentResponse> assignedStudents = getAssignedStudents();
        if (assignedStudents.isEmpty()) {
            return List.of();
        }
        List<Long> studentIds = assignedStudents.stream().map(StudentResponse::getId).toList();
        List<RecentActivity> activities = recentActivityRepository.findTop5ByStudentIdInOrderByTimestampDesc(studentIds);
        return activities.stream()
                .map(act -> RecentActivityResponse.builder()
                        .title(act.getTitle())
                        .description(act.getDescription())
                        .timestamp(act.getTimestamp().toString())
                        .build())
                .toList();
    }

    @Override
    public FacultyDashboardResponse getCombinedDashboard() {
        Faculty faculty = getFacultyProfileForCurrentUser();
        List<FacultyResponse.SubjectDto> subjects = getAssignedSubjects();
        List<StudentResponse> students = getAssignedStudents();
        List<FacultyDashboardResponse.ExamDto> exams = getUpcomingExams();
        List<RecentActivityResponse> activities = getRecentActivities();

        return FacultyDashboardResponse.builder()
                .facultyProfile(facultyMapper.toResponse(faculty))
                .assignedSubjectsCount(subjects.size())
                .assignedStudentsCount(students.size())
                .upcomingExamsCount(exams.size())
                .recentActivities(activities)
                .build();
    }

    private Faculty getFacultyProfileForCurrentUser() {
        User currentUser = getCurrentUser();
        return facultyRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Faculty profile not found for user: " + currentUser.getEmail()));
    }

    private Faculty getEntityById(Long id) {
        return facultyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Faculty not found with id: " + id));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return (User) auth.getPrincipal();
    }

    private Set<Subject> resolveSubjects(List<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return new HashSet<>();
        }
        return subjectIds.stream()
                .map(sid -> subjectRepository.findById(sid)
                        .orElseThrow(() -> new NotFoundException("Subject not found with id: " + sid)))
                .collect(Collectors.toSet());
    }

    private void checkFacultyReadAccess(Faculty faculty) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        if (isAdmin) {
            return;
        }

        User principal = (User) auth.getPrincipal();

        if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            if (faculty.getDepartment() == null || !faculty.getDepartment().getId().equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only view faculty in their own department");
            }
            return;
        }

        if (isFaculty) {
            // Can view own profile, or faculty in same department
            if (faculty.getUser() != null && faculty.getUser().getId().equals(principal.getId())) {
                return;
            }
            Faculty loggedInFaculty = facultyRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new NotFoundException("Faculty profile not found for user: " + principal.getEmail()));
            
            if (faculty.getDepartment() == null || loggedInFaculty.getDepartment() == null ||
                    !faculty.getDepartment().getId().equals(loggedInFaculty.getDepartment().getId())) {
                throw new AccessDeniedException("Access denied: Faculty can only view faculty in their own department");
            }
            return;
        }

        if (isStudent) {
            Student student = studentRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: Logged in user is not registered as a student"));
            if (faculty.getDepartment() == null || student.getDepartment() == null ||
                    !faculty.getDepartment().getId().equals(student.getDepartment().getId())) {
                throw new AccessDeniedException("Access denied: Student can only view faculty in their own department");
            }
            return;
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }

    private void checkFacultyWriteAccess(Faculty faculty) {
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
            if (faculty.getDepartment() == null || !faculty.getDepartment().getId().equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only modify faculty in their own department");
            }
            return;
        }

        if (isFaculty) {
            if (faculty.getUser() != null && faculty.getUser().getId().equals(principal.getId())) {
                return;
            }
            throw new AccessDeniedException("Access denied: Faculty can only modify their own profile");
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }
}
