package com.secureiq.SecureIQ.exam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secureiq.SecureIQ.security.jwt.JwtTokenProvider;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
import com.secureiq.SecureIQ.faculty.model.Faculty;
import com.secureiq.SecureIQ.faculty.repository.FacultyRepository;
import com.secureiq.SecureIQ.subject.model.Subject;
import com.secureiq.SecureIQ.subject.repository.SubjectRepository;
import com.secureiq.SecureIQ.student.model.Student;
import com.secureiq.SecureIQ.student.repository.StudentRepository;
import com.secureiq.SecureIQ.exam.dto.*;
import com.secureiq.SecureIQ.exam.model.*;
import com.secureiq.SecureIQ.exam.repository.ExamRepository;
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
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ExamControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ExamRepository examRepository;

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
    private Subject dspSubject;

    private Faculty facultyProfile;

    private Student studentProfile;

    private Exam exam1;
    private Exam exam2;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        cleanupAll();

        // Create Users
        adminUser = userRepository.save(User.builder().firstName("Admin").lastName("User").email("admin.exam@secureiq.com").password(passwordEncoder.encode("password")).role(Role.ADMIN).status(Status.ACTIVE).build());
        adminToken = "Bearer " + tokenProvider.generateToken(adminUser.getEmail());

        hodUser = userRepository.save(User.builder().firstName("Hod").lastName("User").email("hod.exam@secureiq.com").password(passwordEncoder.encode("password")).role(Role.HOD).status(Status.ACTIVE).build());
        hodToken = "Bearer " + tokenProvider.generateToken(hodUser.getEmail());

        facultyUser = userRepository.save(User.builder().firstName("Faculty").lastName("User").email("faculty.exam@secureiq.com").password(passwordEncoder.encode("password")).role(Role.FACULTY).status(Status.ACTIVE).build());
        facultyToken = "Bearer " + tokenProvider.generateToken(facultyUser.getEmail());

        studentUser = userRepository.save(User.builder().firstName("Student").lastName("User").email("student.exam@secureiq.com").password(passwordEncoder.encode("password")).role(Role.STUDENT).status(Status.ACTIVE).build());
        studentToken = "Bearer " + tokenProvider.generateToken(studentUser.getEmail());

        // Create Departments
        cseDept = departmentRepository.save(Department.builder().departmentName("Computer Science").departmentCode("CSE").hod(hodUser).build());
        eceDept = departmentRepository.save(Department.builder().departmentName("Electronics").departmentCode("ECE").build());

        // Create Subjects
        javaSubject = subjectRepository.save(Subject.builder()
                .subjectCode("CSE301")
                .subjectName("Java Object Oriented Design")
                .credits(4)
                .semester(1)
                .department(cseDept)
                .build());

        dspSubject = subjectRepository.save(Subject.builder()
                .subjectCode("ECE301")
                .subjectName("DSP Theory")
                .credits(3)
                .semester(1)
                .department(eceDept)
                .build());

        // Create Faculty Profile
        facultyProfile = facultyRepository.save(Faculty.builder()
                .user(facultyUser)
                .employeeId("FAC-EM-100")
                .designation("Professor")
                .qualification("Ph.D")
                .specialization("Distributed Computing")
                .yearsOfExperience(10)
                .department(cseDept)
                .joiningDate(LocalDate.now().minusYears(3))
                .build());

        // Create Student Profile
        studentProfile = studentRepository.save(Student.builder()
                .user(studentUser)
                .registerNumber("REG-STUD-01")
                .rollNumber("ROLL-STUD-01")
                .department(cseDept)
                .academicYear("2026")
                .semester(1)
                .build());

        // Create Exams
        exam1 = examRepository.save(Exam.builder()
                .examCode("EX-1")
                .examTitle("Java Midterm")
                .description("Mid Semester Java Exam")
                .subject(javaSubject)
                .faculty(facultyProfile)
                .department(cseDept)
                .semester(1)
                .examType(ExamType.MID_SEMESTER)
                .totalMarks(50)
                .passingMarks(20)
                .durationMinutes(90)
                .scheduledDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .status(ExamStatus.PUBLISHED)
                .build());

        exam2 = examRepository.save(Exam.builder()
                .examCode("EX-2")
                .examTitle("DSP Midterm")
                .description("Mid Semester DSP Exam")
                .subject(dspSubject)
                .faculty(facultyProfile)
                .department(eceDept)
                .semester(1)
                .examType(ExamType.MID_SEMESTER)
                .totalMarks(50)
                .passingMarks(20)
                .durationMinutes(90)
                .scheduledDate(LocalDate.now().plusDays(3))
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 30))
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
        jdbcTemplate.execute("DELETE FROM exams");
        jdbcTemplate.execute("DELETE FROM students");
        jdbcTemplate.execute("DELETE FROM subject_faculty");
        jdbcTemplate.execute("DELETE FROM faculty_subjects");
        jdbcTemplate.execute("DELETE FROM subjects");
        jdbcTemplate.execute("DELETE FROM faculties");
        jdbcTemplate.execute("DELETE FROM departments");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    public void testGetExamsList_AdminAndHODAccess() throws Exception {
        // ADMIN can read all
        mockMvc.perform(get("/api/v1/exams")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(2)));

        // HOD user is HOD of CSE department -> can only view exam1 (CSE)
        mockMvc.perform(get("/api/v1/exams")
                        .header("Authorization", hodToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].examCode", is("EX-1")));
    }

    @Test
    public void testGetExamsList_StudentAccess() throws Exception {
        // STUDENT is in CSE department -> can only view exam1 (CSE)
        mockMvc.perform(get("/api/v1/exams")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].examCode", is("EX-1")));
    }

    @Test
    public void testGetExamById_AuthorizationCheck() throws Exception {
        // Student can view exam1 (in their CSE department)
        mockMvc.perform(get("/api/v1/exams/" + exam1.getId())
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.examCode", is("EX-1")));

        // Student cannot view exam2 (in ECE department) -> Forbidden
        mockMvc.perform(get("/api/v1/exams/" + exam2.getId())
                        .header("Authorization", studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreateExam_BusinessValidationRules() throws Exception {
        ExamCreateRequest request = ExamCreateRequest.builder()
                .examCode("EX-3")
                .examTitle("Java Practical")
                .subjectId(javaSubject.getId())
                .facultyId(facultyProfile.getId())
                .departmentId(cseDept.getId())
                .semester(1)
                .examType(ExamType.PRACTICAL)
                .totalMarks(100)
                .passingMarks(110) // INVALID: passingMarks > totalMarks
                .durationMinutes(120)
                .scheduledDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .build();

        // Passing marks validation check -> Bad Request
        mockMvc.perform(post("/api/v1/exams")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Passing marks cannot exceed total marks")));

        // Duration validation check -> Bad Request
        request.setPassingMarks(40);
        request.setDurationMinutes(-10); // INVALID: duration <= 0
        mockMvc.perform(post("/api/v1/exams")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Date validation check -> Bad Request
        request.setDurationMinutes(120);
        request.setScheduledDate(LocalDate.now().minusDays(1)); // INVALID: past date
        mockMvc.perform(post("/api/v1/exams")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Time validation check -> Bad Request
        request.setScheduledDate(LocalDate.now().plusDays(1));
        request.setStartTime(LocalTime.of(13, 0));
        request.setEndTime(LocalTime.of(12, 0)); // INVALID: startTime >= endTime
        mockMvc.perform(post("/api/v1/exams")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Valid Request -> Success
        request.setEndTime(LocalTime.of(15, 0));
        mockMvc.perform(post("/api/v1/exams")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.examCode", is("EX-3")));
    }

    @Test
    public void testUpdateExam_Permissions() throws Exception {
        ExamUpdateRequest request = ExamUpdateRequest.builder()
                .examCode("EX-1")
                .examTitle("Java Midterm - Rescheduled")
                .subjectId(javaSubject.getId())
                .facultyId(facultyProfile.getId())
                .departmentId(cseDept.getId())
                .semester(1)
                .examType(ExamType.MID_SEMESTER)
                .totalMarks(50)
                .passingMarks(20)
                .durationMinutes(90)
                .scheduledDate(LocalDate.now().plusDays(5)) // Rescheduled date
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .status(ExamStatus.PUBLISHED)
                .build();

        // Faculty updates own exam -> Success
        mockMvc.perform(put("/api/v1/exams/" + exam1.getId())
                        .header("Authorization", facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.examTitle", is("Java Midterm - Rescheduled")));
    }

    @Test
    public void testDeleteExam_SoftDelete() throws Exception {
        // Faculty deletes own exam -> Success
        mockMvc.perform(delete("/api/v1/exams/" + exam1.getId())
                        .header("Authorization", facultyToken))
                .andExpect(status().isOk());

        // Retrieve soft deleted exam -> Not Found
        mockMvc.perform(get("/api/v1/exams/" + exam1.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDashboardAndStatsEndpoints() throws Exception {
        // Retrieve upcoming exams list
        mockMvc.perform(get("/api/v1/exams/upcoming")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].examCode", is("EX-1")));

        // Retrieve today's exams list (none scheduled today)
        mockMvc.perform(get("/api/v1/exams/today")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // Retrieve dashboard API for Student
        mockMvc.perform(get("/api/v1/exams/dashboard")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role", is("STUDENT")))
                .andExpect(jsonPath("$.data.upcomingExams", hasSize(1)));

        // Retrieve dashboard API for Admin
        mockMvc.perform(get("/api/v1/exams/dashboard")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role", is("ADMIN")))
                .andExpect(jsonPath("$.data.totalExams", is(2)));
    }
}
