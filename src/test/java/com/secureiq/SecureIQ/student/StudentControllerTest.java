package com.secureiq.SecureIQ.student;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secureiq.SecureIQ.security.jwt.JwtTokenProvider;
import com.secureiq.SecureIQ.student.dto.*;
import com.secureiq.SecureIQ.student.model.*;
import com.secureiq.SecureIQ.student.repository.*;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class StudentControllerTest {

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
    private ExamRepository examRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private RecentActivityRepository recentActivityRepository;

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
    private Department eceDept;

    private Student studentProfile;
    private Student otherStudentProfile;

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

        otherStudentUser = userRepository.save(User.builder().firstName("Other").lastName("Student").email("otherstudent@secureiq.com").password(passwordEncoder.encode("password")).role(Role.STUDENT).status(Status.ACTIVE).build());
        otherStudentToken = "Bearer " + tokenProvider.generateToken(otherStudentUser.getEmail());

        // Create Departments
        cseDept = departmentRepository.save(Department.builder().departmentName("Computer Science").departmentCode("CSE").hod(hodUser).build());
        eceDept = departmentRepository.save(Department.builder().departmentName("Electronics").departmentCode("ECE").build());

        // Create Student profiles
        studentProfile = studentRepository.save(Student.builder()
                .user(studentUser)
                .registerNumber("REG1001")
                .rollNumber("ROLL1001")
                .department(cseDept)
                .academicYear("2026")
                .semester(1)
                .section("A")
                .batch("2026-2030")
                .dateOfBirth(LocalDate.of(2005, 5, 20))
                .gender("Male")
                .address("123 Street")
                .parentName("John Doe Senior")
                .parentPhone("+12345678901")
                .emergencyContact("+12345678902")
                .profileCompleted(true)
                .build());

        otherStudentProfile = studentRepository.save(Student.builder()
                .user(otherStudentUser)
                .registerNumber("REG2002")
                .rollNumber("ROLL2002")
                .department(eceDept)
                .academicYear("2026")
                .semester(1)
                .section("B")
                .batch("2026-2030")
                .dateOfBirth(LocalDate.of(2005, 8, 12))
                .gender("Female")
                .address("456 Street")
                .parentName("Jane Doe Senior")
                .parentPhone("+12345678903")
                .emergencyContact("+12345678904")
                .profileCompleted(true)
                .build());
    }

    @AfterEach
    public void cleanup() {
        cleanupAll();
    }

    private void cleanupAll() {
        jdbcTemplate.execute("ALTER TABLE departments DROP COLUMN IF EXISTS name CASCADE");
        jdbcTemplate.execute("ALTER TABLE departments DROP COLUMN IF EXISTS code CASCADE");
        
        recentActivityRepository.deleteAllInBatch();
        notificationRepository.deleteAllInBatch();
        examRepository.deleteAllInBatch();
        
        jdbcTemplate.execute("DELETE FROM students");
        jdbcTemplate.execute("DELETE FROM departments");
        
        // Since User is also soft-deletable, physically delete them in tests
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    public void testGetStudentsList_AdminAndFacultyAccess() throws Exception {
        // ADMIN can read all
        mockMvc.perform(get("/api/v1/students")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(2)));

        // FACULTY can read all
        mockMvc.perform(get("/api/v1/students")
                        .header("Authorization", facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));

        // STUDENT cannot list -> Forbidden
        mockMvc.perform(get("/api/v1/students")
                        .header("Authorization", studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetStudentsList_HODRestrictedToOwnDepartment() throws Exception {
        // HOD user is HOD of CSE department. So they can only read student1 (CSE), not student2 (ECE).
        mockMvc.perform(get("/api/v1/students")
                        .header("Authorization", hodToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].registerNumber", is("REG1001")));

        // HOD tries to request ECE department -> Forbidden
        mockMvc.perform(get("/api/v1/students?departmentId=" + eceDept.getId())
                        .header("Authorization", hodToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetStudentById_AuthorizationMatrix() throws Exception {
        // ADMIN reads other student profile
        mockMvc.perform(get("/api/v1/students/" + otherStudentProfile.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registerNumber", is("REG2002")));

        // FACULTY reads student profile
        mockMvc.perform(get("/api/v1/students/" + studentProfile.getId())
                        .header("Authorization", facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registerNumber", is("REG1001")));

        // HOD reads CSE student profile -> OK
        mockMvc.perform(get("/api/v1/students/" + studentProfile.getId())
                        .header("Authorization", hodToken))
                .andExpect(status().isOk());

        // HOD reads ECE student profile -> Forbidden
        mockMvc.perform(get("/api/v1/students/" + otherStudentProfile.getId())
                        .header("Authorization", hodToken))
                .andExpect(status().isForbidden());

        // STUDENT reads own profile -> OK
        mockMvc.perform(get("/api/v1/students/" + studentProfile.getId())
                        .header("Authorization", studentToken))
                .andExpect(status().isOk());

        // STUDENT reads other student's profile -> Forbidden
        mockMvc.perform(get("/api/v1/students/" + otherStudentProfile.getId())
                        .header("Authorization", studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreateStudent_SuccessAndConflictValidation() throws Exception {
        // Create an unlinked user for the new student
        User newUser = userRepository.save(User.builder().firstName("New").lastName("User").email("newstudent@secureiq.com").password("password").role(Role.STUDENT).status(Status.ACTIVE).build());

        StudentCreateRequest request = StudentCreateRequest.builder()
                .userId(newUser.getId())
                .registerNumber("REG3003")
                .rollNumber("ROLL3003")
                .departmentId(cseDept.getId())
                .academicYear("2026")
                .semester(1)
                .parentPhone("+12345678909")
                .build();

        // Admin successfully creates student
        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.registerNumber", is("REG3003")));

        // Duplicate Register Number -> Conflict
        request.setRollNumber("ROLL4004"); // different roll number
        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    public void testUpdateStudent_Permissions() throws Exception {
        StudentUpdateRequest request = StudentUpdateRequest.builder()
                .registerNumber("REG1001")
                .rollNumber("ROLL1001")
                .departmentId(cseDept.getId())
                .academicYear("2027") // Changed field
                .semester(2)
                .build();

        // STUDENT updates own student profile -> OK
        mockMvc.perform(put("/api/v1/students/" + studentProfile.getId())
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.academicYear", is("2027")));

        // STUDENT tries to update other profile -> Forbidden
        mockMvc.perform(put("/api/v1/students/" + otherStudentProfile.getId())
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // FACULTY tries to update student -> Forbidden
        mockMvc.perform(put("/api/v1/students/" + studentProfile.getId())
                        .header("Authorization", facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testPatchStudent_Success() throws Exception {
        StudentPatchRequest request = StudentPatchRequest.builder()
                .academicYear("2029")
                .build();

        // STUDENT patches own profile
        mockMvc.perform(patch("/api/v1/students/" + studentProfile.getId())
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.academicYear", is("2029")))
                .andExpect(jsonPath("$.data.registerNumber", is("REG1001"))); // Unchanged
    }

    @Test
    public void testDeleteStudent_AdminOnly() throws Exception {
        // FACULTY tries to delete -> Forbidden
        mockMvc.perform(delete("/api/v1/students/" + studentProfile.getId())
                        .header("Authorization", facultyToken))
                .andExpect(status().isForbidden());

        // ADMIN deletes -> OK
        mockMvc.perform(delete("/api/v1/students/" + studentProfile.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());

        // Verify soft deleted profile returns 404
        mockMvc.perform(get("/api/v1/students/" + studentProfile.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testStudentDashboardAndProfileApis() throws Exception {
        // Prepopulate metrics data
        // Upcoming exam
        examRepository.save(Exam.builder().title("Final Term Paper 1").scheduledAt(LocalDateTime.now().plusDays(2)).department(cseDept).build());
        // Completed exam
        examRepository.save(Exam.builder().title("Mid Term Paper 1").scheduledAt(LocalDateTime.now().minusDays(2)).department(cseDept).build());
        // Notification
        notificationRepository.save(Notification.builder().title("Holiday Notice").message("Tomorrow is a holiday").studentId(studentProfile.getId()).read(false).build());
        notificationRepository.save(Notification.builder().title("Fee Reminder").message("Pay tuition fees").studentId(studentProfile.getId()).read(true).build()); // Read notification
        // Activities
        recentActivityRepository.save(RecentActivity.builder().title("Logged In").description("Logged into console").studentId(studentProfile.getId()).timestamp(LocalDateTime.now()).build());

        // Verify profile API
        mockMvc.perform(get("/api/v1/students/profile")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registerNumber", is("REG1001")));

        // Verify Dashboard API counts
        mockMvc.perform(get("/api/v1/students/dashboard")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.upcomingExamsCount", is(1)))
                .andExpect(jsonPath("$.data.completedExamsCount", is(1)))
                .andExpect(jsonPath("$.data.notificationsCount", is(1)))
                .andExpect(jsonPath("$.data.recentActivities", hasSize(1)))
                .andExpect(jsonPath("$.data.recentActivities[0].title", is("Logged In")));
    }
}
