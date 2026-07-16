package com.secureiq.SecureIQ.violation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
import com.secureiq.SecureIQ.exam.model.Exam;
import com.secureiq.SecureIQ.exam.model.ExamStatus;
import com.secureiq.SecureIQ.exam.model.ExamType;
import com.secureiq.SecureIQ.exam.repository.ExamRepository;
import com.secureiq.SecureIQ.examattempt.model.AttemptStatus;
import com.secureiq.SecureIQ.examattempt.model.StudentExamAttempt;
import com.secureiq.SecureIQ.examattempt.repository.StudentExamAttemptRepository;
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
import com.secureiq.SecureIQ.violation.dto.*;
import com.secureiq.SecureIQ.violation.model.Severity;
import com.secureiq.SecureIQ.violation.model.Source;
import com.secureiq.SecureIQ.violation.model.ViolationType;
import com.secureiq.SecureIQ.violation.repository.ViolationRepository;
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
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ViolationControllerTest {

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
    private StudentExamAttemptRepository studentExamAttemptRepository;

    @Autowired
    private ViolationRepository violationRepository;

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
    private User otherStudentUser;

    private String adminToken;
    private String hodToken;
    private String facultyToken;
    private String studentToken;
    private String otherStudentToken;

    private Department cseDept;
    private Subject javaSubject;
    private Exam javaExam;
    private Faculty cseFaculty;
    private Student cseStudent;
    private Student otherStudent;

    private ExamSession liveSession;
    private StudentExamAttempt studentAttempt;
    private StudentExamAttempt otherAttempt;

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

        otherStudentUser = userRepository.save(User.builder().firstName("Other").lastName("Student").email("other@secureiq.com").password(passwordEncoder.encode("password")).role(Role.STUDENT).status(Status.ACTIVE).build());
        otherStudentToken = "Bearer " + tokenProvider.generateToken(otherStudentUser.getEmail());

        // Create Department
        cseDept = departmentRepository.save(Department.builder().departmentName("Computer Science").departmentCode("CSE").hod(hodUser).build());

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

        // Create Student Profiles
        cseStudent = studentRepository.save(Student.builder()
                .user(studentUser)
                .registerNumber("REG2001")
                .rollNumber("ROLL2001")
                .department(cseDept)
                .academicYear("2026")
                .semester(3)
                .profileCompleted(true)
                .build());

        otherStudent = studentRepository.save(Student.builder()
                .user(otherStudentUser)
                .registerNumber("REG2002")
                .rollNumber("ROLL2002")
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

        // Create Session
        liveSession = examSessionRepository.save(ExamSession.builder()
                .sessionCode("SES-LIVE")
                .sessionName("Live Java Session")
                .exam(javaExam)
                .faculty(cseFaculty)
                .startDateTime(LocalDateTime.now().minusHours(1))
                .endDateTime(LocalDateTime.now().plusHours(2))
                .venue("Seminar Hall")
                .status(SessionStatus.LIVE)
                .students(Set.of(cseStudent, otherStudent))
                .build());

        // Create Attempts
        studentAttempt = studentExamAttemptRepository.save(StudentExamAttempt.builder()
                .attemptCode("ATT-STU")
                .student(cseStudent)
                .examSession(liveSession)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(3))
                .status(AttemptStatus.IN_PROGRESS)
                .totalMarks(100)
                .build());

        otherAttempt = studentExamAttemptRepository.save(StudentExamAttempt.builder()
                .attemptCode("ATT-OTH")
                .student(otherStudent)
                .examSession(liveSession)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(3))
                .status(AttemptStatus.IN_PROGRESS)
                .totalMarks(100)
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
    public void testRecordViolation_WarningCounterIncrements() throws Exception {
        ViolationCreateRequest request = ViolationCreateRequest.builder()
                .violationCode("VIO-001")
                .studentExamAttemptId(studentAttempt.getId())
                .violationType(ViolationType.TAB_SWITCH)
                .severity(Severity.HIGH)
                .source(Source.ELECTRON)
                .description("Tab switch warning")
                .confidenceScore(100.0)
                .detectedAt(LocalDateTime.now())
                .build();

        // 1. Log violation under student credentials
        mockMvc.perform(post("/api/v1/violations")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.violationCode", is("VIO-001")))
                .andExpect(jsonPath("$.data.source", is("ELECTRON")));

        // Verify warning counter increment on the student's exam attempt
        mockMvc.perform(get("/api/v1/attempts/" + studentAttempt.getId())
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.browserWarnings", is(1)))
                .andExpect(jsonPath("$.data.totalViolations", is(1)));

        // 2. Student logs violation against ANOTHER student's attempt -> Forbidden
        request.setViolationCode("VIO-002");
        request.setStudentExamAttemptId(otherAttempt.getId());
        mockMvc.perform(post("/api/v1/violations")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testUpdateAndResolveViolation_Permissions() throws Exception {
        ViolationCreateRequest createRequest = ViolationCreateRequest.builder()
                .violationCode("VIO-RESOLVE")
                .studentExamAttemptId(studentAttempt.getId())
                .violationType(ViolationType.DEVTOOLS_OPENED)
                .severity(Severity.CRITICAL)
                .source(Source.SYSTEM)
                .detectedAt(LocalDateTime.now())
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/violations")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        Long violationId = mapper.readTree(responseJson).get("data").get("id").asLong();

        ViolationUpdateRequest resolveRequest = ViolationUpdateRequest.builder()
                .actionTaken("Resolved with warning issued")
                .resolved(true)
                .build();

        // 1. Student attempts to resolve -> Forbidden
        mockMvc.perform(patch("/api/v1/violations/" + violationId)
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveRequest)))
                .andExpect(status().isForbidden());

        // 2. Faculty resolves -> OK
        mockMvc.perform(patch("/api/v1/violations/" + violationId)
                        .header("Authorization", facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resolved", is(true)))
                .andExpect(jsonPath("$.data.actionTaken", is("Resolved with warning issued")));
    }

    @Test
    public void testDashboardMetrics() throws Exception {
        // Create 1 critical violation
        ViolationCreateRequest req = ViolationCreateRequest.builder()
                .violationCode("VIO-CRIT")
                .studentExamAttemptId(studentAttempt.getId())
                .violationType(ViolationType.PHONE_DETECTED)
                .severity(Severity.CRITICAL)
                .source(Source.AI_ENGINE)
                .detectedAt(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/api/v1/violations")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // 1. Student Dashboard
        mockMvc.perform(get("/api/v1/violations/dashboard")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalViolationsCount", is(1)));

        // 2. Faculty Dashboard
        mockMvc.perform(get("/api/v1/violations/dashboard")
                        .header("Authorization", facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liveViolationsCount", is(1)))
                .andExpect(jsonPath("$.data.criticalViolationsCount", is(1)));

        // 3. Admin Dashboard
        mockMvc.perform(get("/api/v1/violations/dashboard")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.severityStatistics.CRITICAL", is(1)))
                .andExpect(jsonPath("$.data.typeStatistics.PHONE_DETECTED", is(1)))
                .andExpect(jsonPath("$.data.dailyReports", hasSize(greaterThan(0))));
    }
}
