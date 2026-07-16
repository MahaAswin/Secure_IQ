package com.secureiq.SecureIQ.electron;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
import com.secureiq.SecureIQ.electron.dto.BrowserConnectRequest;
import com.secureiq.SecureIQ.electron.dto.BrowserDisconnectRequest;
import com.secureiq.SecureIQ.electron.dto.BrowserEventRequest;
import com.secureiq.SecureIQ.electron.dto.BrowserHeartbeatRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class BrowserSessionControllerTest {

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
    private Subject javaSubject;
    private Exam javaExam;
    private Faculty cseFaculty;
    private Student cseStudent;

    private ExamSession liveSession;
    private StudentExamAttempt studentAttempt;

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

        // Create Subject
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
                .students(Set.of(cseStudent))
                .build());

        // Create Attempt
        studentAttempt = studentExamAttemptRepository.save(StudentExamAttempt.builder()
                .attemptCode("ATT-STU")
                .student(cseStudent)
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
    public void testBrowserIntegrationLifecycle() throws Exception {
        BrowserConnectRequest connectRequest = BrowserConnectRequest.builder()
                .attemptCode("ATT-STU")
                .browserVersion("SecureBrowser/1.0.0 Chrome/120.0")
                .operatingSystem("Windows 11")
                .machineId("MAC-ADDR-XXXX")
                .ipAddress("192.168.1.100")
                .build();

        // 1. Connect proctoring browser
        String connectResponse = mockMvc.perform(post("/api/v1/browser/connect")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId", notNullValue()))
                .andExpect(jsonPath("$.data.operatingSystem", is("Windows 11")))
                .andExpect(jsonPath("$.data.active", is(true)))
                .andReturn().getResponse().getContentAsString();

        String sessionId = objectMapper.readTree(connectResponse).get("data").get("sessionId").asText();
        Long sessionDatabaseId = objectMapper.readTree(connectResponse).get("data").get("id").asLong();

        // 2. Send heartbeat
        BrowserHeartbeatRequest heartbeatReq = BrowserHeartbeatRequest.builder()
                .sessionId(sessionId)
                .build();

        mockMvc.perform(post("/api/v1/browser/heartbeat")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(heartbeatReq)))
                .andExpect(status().isOk());

        // 3. Record browser events & verify automatic violation creation
        BrowserEventRequest eventReq = BrowserEventRequest.builder()
                .sessionId(sessionId)
                .eventType("TAB_SWITCH")
                .description("Student switched window tab to browser")
                .build();

        mockMvc.perform(post("/api/v1/browser/event")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventReq)))
                .andExpect(status().isOk());

        // Verify violation is automatically logged on student exam attempt warning counters
        mockMvc.perform(get("/api/v1/attempts/" + studentAttempt.getId())
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.browserWarnings", is(1)))
                .andExpect(jsonPath("$.data.totalViolations", is(1)));

        // 4. Retrieve session details
        mockMvc.perform(get("/api/v1/browser/session/" + sessionDatabaseId)
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId", is(sessionId)));

        // 5. Faculty Dashboard
        mockMvc.perform(get("/api/v1/browser/dashboard")
                        .header("Authorization", facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.connectedStudentsCount", is(1)))
                .andExpect(jsonPath("$.data.disconnectedStudentsCount", is(0)))
                .andExpect(jsonPath("$.data.browserStatus", hasSize(1)))
                .andExpect(jsonPath("$.data.browserStatus[0].status", is("CONNECTED")));

        // 6. Admin Dashboard
        mockMvc.perform(get("/api/v1/browser/dashboard")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeBrowserSessionsCount", is(1)))
                .andExpect(jsonPath("$.data.browserVersionStatistics['SecureBrowser/1.0.0 Chrome/120.0']", is(1)))
                .andExpect(jsonPath("$.data.operatingSystemStatistics['Windows 11']", is(1)));

        // 7. Disconnect
        BrowserDisconnectRequest disconnectReq = BrowserDisconnectRequest.builder()
                .sessionId(sessionId)
                .build();

        mockMvc.perform(post("/api/v1/browser/disconnect")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disconnectReq)))
                .andExpect(status().isOk());
    }
}
