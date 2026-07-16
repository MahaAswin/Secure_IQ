package com.secureiq.SecureIQ.examsession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
import com.secureiq.SecureIQ.exam.model.Exam;
import com.secureiq.SecureIQ.exam.model.ExamStatus;
import com.secureiq.SecureIQ.exam.model.ExamType;
import com.secureiq.SecureIQ.exam.repository.ExamRepository;
import com.secureiq.SecureIQ.examsession.dto.*;
import com.secureiq.SecureIQ.examsession.model.ExamSession;
import com.secureiq.SecureIQ.examsession.model.SessionStatus;
import com.secureiq.SecureIQ.examsession.repository.ExamSessionRepository;
import com.secureiq.SecureIQ.faculty.model.Faculty;
import com.secureiq.SecureIQ.faculty.repository.FacultyRepository;
import com.secureiq.SecureIQ.security.jwt.JwtTokenProvider;
import com.secureiq.SecureIQ.student.model.Student;
import com.secureiq.SecureIQ.student.repository.StudentRepository;
import com.secureiq.SecureIQ.subject.model.Subject;
import com.secureiq.SecureIQ.subject.repository.SubjectRepository;
import com.secureiq.SecureIQ.user.model.Role;
import com.secureiq.SecureIQ.user.model.Status;
import com.secureiq.SecureIQ.user.model.User;
import com.secureiq.SecureIQ.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ExamSessionControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private ExamSessionRepository examSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;
    private User hodUser;
    private User facultyUser;
    private User studentUser;

    private String adminToken;
    private String hodToken;
    private String facultyToken;
    private String studentToken;

    private Department cseDept;
    private Department eceDept;

    private Subject javaSubject;
    private Exam javaExam;
    private Faculty cseFaculty;
    private Student cseStudent;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        cleanupAll();

        // Create Users
        adminUser = userRepository.save(User.builder().firstName("Admin").lastName("User").email("admin@secureiq.com").password(passwordEncoder.encode("password")).role(Role.ADMIN).status(Status.ACTIVE).build());
        adminToken = "Bearer " + tokenProvider.generateToken(adminUser.getEmail());

        hodUser = userRepository.save(User.builder().firstName("Hod").lastName("User").email("hod@secureiq.com").password(passwordEncoder.encode("password")).role(Role.HOD).status(Status.ACTIVE).build());
        hodToken = "Bearer " + tokenProvider.generateToken(hodUser.getEmail());

        facultyUser = userRepository.save(User.builder().firstName("Faculty").lastName("User").email("faculty@secureiq.com").password(passwordEncoder.encode("password")).role(Role.FACULTY).status(Status.ACTIVE).build());
        facultyToken = "Bearer " + tokenProvider.generateToken(facultyUser.getEmail());

        studentUser = userRepository.save(User.builder().firstName("Student").lastName("User").email("student@secureiq.com").password(passwordEncoder.encode("password")).role(Role.STUDENT).status(Status.ACTIVE).build());
        studentToken = "Bearer " + tokenProvider.generateToken(studentUser.getEmail());

        // Create Departments
        cseDept = departmentRepository.save(Department.builder().departmentName("Computer Science").departmentCode("CSE").hod(hodUser).build());
        eceDept = departmentRepository.save(Department.builder().departmentName("Electronics").departmentCode("ECE").build());

        // Create Faculty Profile
        cseFaculty = facultyRepository.save(Faculty.builder()
                .user(facultyUser)
                .employeeId("FAC001")
                .designation("Assistant Professor")
                .qualification("Ph.D.")
                .specialization("Java")
                .yearsOfExperience(5)
                .department(cseDept)
                .joiningDate(LocalDate.now())
                .profileCompleted(true)
                .build());

        // Create Student Profile
        cseStudent = studentRepository.save(Student.builder()
                .user(studentUser)
                .registerNumber("REG2001")
                .rollNumber("ROLL2001")
                .department(cseDept)
                .academicYear("2026")
                .semester(3)
                .profileCompleted(true)
                .build());

        // Create Subjects
        javaSubject = subjectRepository.save(Subject.builder()
                .subjectCode("CSE301")
                .subjectName("Java Programming")
                .credits(4)
                .semester(3)
                .regulation("R2021")
                .department(cseDept)
                .faculty(Set.of(facultyUser))
                .build());

        // Create Exam
        javaExam = examRepository.save(Exam.builder()
                .examCode("EXM-JAVA")
                .examTitle("Java Semester Exam")
                .subject(javaSubject)
                .faculty(cseFaculty)
                .department(cseDept)
                .semester(3)
                .examType(ExamType.INTERNAL)
                .totalMarks(100)
                .durationMinutes(180)
                .passingMarks(40)
                .scheduledDate(LocalDate.now().plusDays(5))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(13, 0))
                .venue("Lab 1")
                .status(ExamStatus.PUBLISHED)
                .build());
    }

    @AfterEach
    public void cleanup() {
        cleanupAll();
    }

    private void cleanupAll() {
        jdbcTemplate.execute("ALTER TABLE departments DROP COLUMN IF EXISTS name CASCADE");
        jdbcTemplate.execute("ALTER TABLE departments DROP COLUMN IF EXISTS code CASCADE");
        jdbcTemplate.execute("ALTER TABLE exams DROP COLUMN IF EXISTS title CASCADE");
        jdbcTemplate.execute("ALTER TABLE exams DROP COLUMN IF EXISTS scheduled_at CASCADE");

        jdbcTemplate.execute("DELETE FROM activities");
        jdbcTemplate.execute("DELETE FROM notifications");
        jdbcTemplate.execute("DELETE FROM browser_sessions");
        jdbcTemplate.execute("DELETE FROM violations");
        jdbcTemplate.execute("DELETE FROM student_exam_attempts");
        jdbcTemplate.execute("DELETE FROM session_students");
        jdbcTemplate.execute("DELETE FROM exam_sessions");
        jdbcTemplate.execute("DELETE FROM exams");
        jdbcTemplate.execute("DELETE FROM students");
        jdbcTemplate.execute("DELETE FROM subject_faculty");
        jdbcTemplate.execute("DELETE FROM faculty_subjects");
        jdbcTemplate.execute("DELETE FROM subjects");
        jdbcTemplate.execute("DELETE FROM faculties");
        jdbcTemplate.execute("DELETE FROM question_options");
        jdbcTemplate.execute("DELETE FROM questions");
        jdbcTemplate.execute("DELETE FROM question_banks");
        jdbcTemplate.execute("DELETE FROM departments");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    public void testCreateSession_ValidationsAndCRUD() throws Exception {
        ExamSessionCreateRequest request = ExamSessionCreateRequest.builder()
                .sessionCode("SES-001")
                .sessionName("Java Lab Session A")
                .examId(javaExam.getId())
                .facultyId(cseFaculty.getId())
                .startDateTime(LocalDateTime.now().plusDays(2))
                .endDateTime(LocalDateTime.now().plusDays(2).plusHours(3))
                .venue("Seminar Hall")
                .status(SessionStatus.SCHEDULED)
                .instructions("Carry ID card")
                .build();

        // 1. Create session successfully
        mockMvc.perform(post("/api/v1/exam-sessions")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionCode", is("SES-001")));

        // 2. Duplicate Session Code -> Conflict
        request.setVenue("Different Venue");
        mockMvc.perform(post("/api/v1/exam-sessions")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // 3. Time Validation: Start must be before End
        request.setSessionCode("SES-002");
        request.setStartDateTime(LocalDateTime.now().plusDays(3));
        request.setEndDateTime(LocalDateTime.now().plusDays(3).minusHours(1)); // Invalid
        mockMvc.perform(post("/api/v1/exam-sessions")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // 4. Overlap Validation: Venue overlap in same time range
        request.setStartDateTime(LocalDateTime.now().plusDays(2).plusHours(1)); // Overlaps with SES-001 at Seminar Hall
        request.setEndDateTime(LocalDateTime.now().plusDays(2).plusHours(2));
        request.setVenue("Seminar Hall");
        mockMvc.perform(post("/api/v1/exam-sessions")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testStudentAssignmentAndRetrieve() throws Exception {
        ExamSession session = examSessionRepository.save(ExamSession.builder()
                .sessionCode("SES-ASSIGN")
                .sessionName("Name")
                .exam(javaExam)
                .faculty(cseFaculty)
                .startDateTime(LocalDateTime.now().plusDays(5))
                .endDateTime(LocalDateTime.now().plusDays(5).plusHours(2))
                .venue("Lab A")
                .status(SessionStatus.SCHEDULED)
                .build());

        StudentAssignmentRequest assignRequest = StudentAssignmentRequest.builder()
                .studentIds(List.of(cseStudent.getId(), cseStudent.getId())) // Pass duplicate student IDs
                .build();

        // 1. Assign students. Ensure duplicates are filtered by Set.
        mockMvc.perform(post("/api/v1/exam-sessions/" + session.getId() + "/assign-students")
                        .header("Authorization", facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalStudents", is(1))); // duplicates ignored

        // 2. Fetch assigned students list
        mockMvc.perform(get("/api/v1/exam-sessions/" + session.getId() + "/students")
                        .header("Authorization", studentToken)) // student has access since assigned
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].registerNumber", is("REG2001")));
    }

    @Test
    public void testRoleAccessRestrictions() throws Exception {
        ExamSession session = examSessionRepository.save(ExamSession.builder()
                .sessionCode("SES-ROLE")
                .sessionName("Name")
                .exam(javaExam)
                .faculty(cseFaculty)
                .startDateTime(LocalDateTime.now().plusDays(5))
                .endDateTime(LocalDateTime.now().plusDays(5).plusHours(2))
                .venue("Lab A")
                .status(SessionStatus.SCHEDULED)
                .build());

        // Student tries to view session details without assignment -> Forbidden
        mockMvc.perform(get("/api/v1/exam-sessions/" + session.getId())
                        .header("Authorization", studentToken))
                .andExpect(status().isForbidden());

        // Assign student to the session
        session.getStudents().add(cseStudent);
        session.setTotalStudents(1);
        examSessionRepository.save(session);

        // Student now has access -> OK
        mockMvc.perform(get("/api/v1/exam-sessions/" + session.getId())
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionCode", is("SES-ROLE")));
    }

    @Test
    public void testLiveAndUpcomingEndpoints() throws Exception {
        // Create 1 live session and 1 upcoming session
        examSessionRepository.save(ExamSession.builder()
                .sessionCode("SES-LIVE")
                .sessionName("Live Exam")
                .exam(javaExam)
                .faculty(cseFaculty)
                .startDateTime(LocalDateTime.now().minusHours(1))
                .endDateTime(LocalDateTime.now().plusHours(2))
                .venue("Venue A")
                .status(SessionStatus.LIVE)
                .build());

        examSessionRepository.save(ExamSession.builder()
                .sessionCode("SES-UPCOMING")
                .sessionName("Upcoming Exam")
                .exam(javaExam)
                .faculty(cseFaculty)
                .startDateTime(LocalDateTime.now().plusDays(1))
                .endDateTime(LocalDateTime.now().plusDays(1).plusHours(3))
                .venue("Venue B")
                .status(SessionStatus.SCHEDULED)
                .build());

        // Query Live
        mockMvc.perform(get("/api/v1/exam-sessions/live")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].sessionCode", is("SES-LIVE")));

        // Query Upcoming
        mockMvc.perform(get("/api/v1/exam-sessions/upcoming")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].sessionCode", is("SES-UPCOMING")));
    }

    @Test
    public void testDashboardAPI_Metrics() throws Exception {
        // Create sessions with assigned student
        ExamSession liveSession = examSessionRepository.save(ExamSession.builder()
                .sessionCode("SES-LIVE")
                .sessionName("Live Exam")
                .exam(javaExam)
                .faculty(cseFaculty)
                .startDateTime(LocalDateTime.now().minusHours(1))
                .endDateTime(LocalDateTime.now().plusHours(2))
                .venue("Venue A")
                .totalStudents(10)
                .joinedStudents(8)
                .status(SessionStatus.LIVE)
                .build());

        // Fetch Admin Dashboard
        mockMvc.perform(get("/api/v1/exam-sessions/dashboard")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeSessionsCount", is(1)))
                .andExpect(jsonPath("$.data.totalSessionsCount", is(1)))
                .andExpect(jsonPath("$.data.attendanceSummary.totalAssigned", is(10)))
                .andExpect(jsonPath("$.data.attendanceSummary.totalJoined", is(8)))
                .andExpect(jsonPath("$.data.attendanceSummary.attendanceRate", closeTo(80.0, 0.01)));
    }
}
