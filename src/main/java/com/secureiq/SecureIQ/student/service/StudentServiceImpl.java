package com.secureiq.SecureIQ.student.service;

import com.secureiq.SecureIQ.common.exception.BadRequestException;
import com.secureiq.SecureIQ.common.exception.ConflictException;
import com.secureiq.SecureIQ.common.exception.NotFoundException;
import com.secureiq.SecureIQ.common.exception.UnauthorizedException;
import com.secureiq.SecureIQ.student.dto.*;
import com.secureiq.SecureIQ.student.mapper.StudentMapper;
import com.secureiq.SecureIQ.student.model.*;
import com.secureiq.SecureIQ.student.repository.*;
import com.secureiq.SecureIQ.exam.repository.ExamRepository;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
import com.secureiq.SecureIQ.user.model.User;
import com.secureiq.SecureIQ.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final NotificationRepository notificationRepository;
    private final RecentActivityRepository recentActivityRepository;
    private final StudentMapper studentMapper;

    public StudentServiceImpl(StudentRepository studentRepository,
                              DepartmentRepository departmentRepository,
                              UserRepository userRepository,
                              ExamRepository examRepository,
                              NotificationRepository notificationRepository,
                              RecentActivityRepository recentActivityRepository,
                              StudentMapper studentMapper) {
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.examRepository = examRepository;
        this.notificationRepository = notificationRepository;
        this.recentActivityRepository = recentActivityRepository;
        this.studentMapper = studentMapper;
    }

    @Override
    public Page<StudentResponse> getAll(String name, String registerNumber, String rollNumber, Long departmentId, Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));

        if (!isAdmin && !isFaculty && !isHod) {
            throw new AccessDeniedException("Access denied: You do not have permission to view students list");
        }

        if (isHod && !isAdmin) {
            User principal = (User) auth.getPrincipal();
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));

            if (departmentId != null && !departmentId.equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only view students in their own department");
            }
            departmentId = dept.getId();
        }

        Page<Student> students = studentRepository.findAllFiltered(name, registerNumber, rollNumber, departmentId, pageable);
        return students.map(studentMapper::toResponse);
    }

    @Override
    public StudentResponse getById(Long id) {
        Student student = getEntityById(id);
        checkStudentReadAccess(student);
        return studentMapper.toResponse(student);
    }

    @Override
    @Transactional
    public StudentResponse create(StudentCreateRequest request) {
        if (studentRepository.existsByRegisterNumber(request.getRegisterNumber())) {
            throw new ConflictException("Register number is already registered");
        }
        if (studentRepository.existsByRollNumber(request.getRollNumber())) {
            throw new ConflictException("Roll number is already registered");
        }
        if (studentRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new ConflictException("Student profile is already linked to this user");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found with id: " + request.getUserId()));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + request.getDepartmentId()));

        Student student = studentMapper.toEntity(request, user, department);
        Student savedStudent = studentRepository.save(student);
        return studentMapper.toResponse(savedStudent);
    }

    @Override
    @Transactional
    public StudentResponse update(Long id, StudentUpdateRequest request) {
        Student student = getEntityById(id);
        checkStudentWriteAccess(student);

        if (!student.getRegisterNumber().equalsIgnoreCase(request.getRegisterNumber()) &&
                studentRepository.existsByRegisterNumber(request.getRegisterNumber())) {
            throw new ConflictException("Register number is already registered");
        }
        if (!student.getRollNumber().equalsIgnoreCase(request.getRollNumber()) &&
                studentRepository.existsByRollNumber(request.getRollNumber())) {
            throw new ConflictException("Roll number is already registered");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + request.getDepartmentId()));

        studentMapper.updateEntity(request, department, student);
        Student savedStudent = studentRepository.save(student);
        return studentMapper.toResponse(savedStudent);
    }

    @Override
    @Transactional
    public StudentResponse patch(Long id, StudentPatchRequest request) {
        Student student = getEntityById(id);
        checkStudentWriteAccess(student);

        if (request.getRegisterNumber() != null &&
                !student.getRegisterNumber().equalsIgnoreCase(request.getRegisterNumber()) &&
                studentRepository.existsByRegisterNumber(request.getRegisterNumber())) {
            throw new ConflictException("Register number is already registered");
        }
        if (request.getRollNumber() != null &&
                !student.getRollNumber().equalsIgnoreCase(request.getRollNumber()) &&
                studentRepository.existsByRollNumber(request.getRollNumber())) {
            throw new ConflictException("Roll number is already registered");
        }

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new NotFoundException("Department not found with id: " + request.getDepartmentId()));
        }

        studentMapper.patchEntity(request, department, student);
        Student savedStudent = studentRepository.save(student);
        return studentMapper.toResponse(savedStudent);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Student student = getEntityById(id);
        studentRepository.delete(student);
    }

    @Override
    public StudentResponse getProfileByCurrentUser() {
        User currentUser = getCurrentUser();
        Student student = studentRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Student profile not found for user: " + currentUser.getEmail()));
        return studentMapper.toResponse(student);
    }

    @Override
    public StudentDashboardResponse getDashboardByCurrentUser() {
        User currentUser = getCurrentUser();
        Student student = studentRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Student profile not found for user: " + currentUser.getEmail()));

        long upcomingExams = 0;
        long completedExams = 0;
        if (student.getDepartment() != null) {
            upcomingExams = examRepository.countByDepartmentIdAndScheduledDateAfter(
                    student.getDepartment().getId(), java.time.LocalDate.now()
            );
            completedExams = examRepository.countByDepartmentIdAndScheduledDateBefore(
                    student.getDepartment().getId(), java.time.LocalDate.now()
            );
        }

        long notifications = notificationRepository.countByStudentIdAndReadFalse(student.getId());

        List<RecentActivity> activities = recentActivityRepository.findTop5ByStudentIdOrderByTimestampDesc(student.getId());
        List<RecentActivityResponse> activityResponses = activities.stream()
                .map(act -> RecentActivityResponse.builder()
                        .title(act.getTitle())
                        .description(act.getDescription())
                        .timestamp(act.getTimestamp().toString())
                        .build())
                .toList();

        return StudentDashboardResponse.builder()
                .studentProfile(studentMapper.toResponse(student))
                .upcomingExamsCount(upcomingExams)
                .completedExamsCount(completedExams)
                .notificationsCount(notifications)
                .recentActivities(activityResponses)
                .build();
    }

    private Student getEntityById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Student not found with id: " + id));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return (User) auth.getPrincipal();
    }

    private void checkStudentReadAccess(Student student) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));

        if (isAdmin || isFaculty) {
            return;
        }

        User principal = (User) auth.getPrincipal();

        if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            if (student.getDepartment() == null || !student.getDepartment().getId().equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only view students in their own department");
            }
            return;
        }

        // Student can only read own profile
        if (student.getUser() == null || !student.getUser().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Access denied: You can only view your own profile");
        }
    }

    private void checkStudentWriteAccess(Student student) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            User principal = (User) auth.getPrincipal();
            if (student.getUser() == null || !student.getUser().getId().equals(principal.getId())) {
                throw new AccessDeniedException("Access denied: You can only modify your own profile");
            }
        }
    }
}
