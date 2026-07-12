package com.secureiq.SecureIQ.department;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secureiq.SecureIQ.security.jwt.JwtTokenProvider;
import com.secureiq.SecureIQ.department.dto.*;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
import com.secureiq.SecureIQ.student.model.Student;
import com.secureiq.SecureIQ.student.repository.StudentRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class DepartmentControllerTest {

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

        // Create Student profiles linked to departments
        studentRepository.save(Student.builder()
                .user(studentUser)
                .registerNumber("REG1001")
                .rollNumber("ROLL1001")
                .department(cseDept)
                .academicYear("2026")
                .profileCompleted(true)
                .build());

        studentRepository.save(Student.builder()
                .user(otherStudentUser)
                .registerNumber("REG2002")
                .rollNumber("ROLL2002")
                .department(eceDept)
                .academicYear("2026")
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
        
        jdbcTemplate.execute("DELETE FROM students");
        jdbcTemplate.execute("DELETE FROM departments");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    public void testGetDepartmentsList_AdminAndFacultyAccess() throws Exception {
        // ADMIN can see both
        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(2)));

        // FACULTY can see both
        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    public void testGetDepartmentsList_HODAndStudentRestricted() throws Exception {
        // HOD sees only CSE (1 department)
        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", hodToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].departmentCode", is("CSE")));

        // STUDENT sees only CSE (their own department)
        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].departmentCode", is("CSE")));
    }

    @Test
    public void testGetDepartmentById_Permissions() throws Exception {
        // HOD reads CSE department details -> OK
        mockMvc.perform(get("/api/v1/departments/" + cseDept.getId())
                        .header("Authorization", hodToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.departmentCode", is("CSE")));

        // HOD reads ECE department details -> Forbidden
        mockMvc.perform(get("/api/v1/departments/" + eceDept.getId())
                        .header("Authorization", hodToken))
                .andExpect(status().isForbidden());

        // STUDENT reads CSE department details -> OK
        mockMvc.perform(get("/api/v1/departments/" + cseDept.getId())
                        .header("Authorization", studentToken))
                .andExpect(status().isOk());

        // STUDENT reads ECE department details -> Forbidden
        mockMvc.perform(get("/api/v1/departments/" + eceDept.getId())
                        .header("Authorization", studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreateDepartment_SuccessAndConflict() throws Exception {
        DepartmentCreateRequest request = DepartmentCreateRequest.builder()
                .departmentName("Mechanical")
                .departmentCode("MECH")
                .description("Mech Engineering")
                .build();

        // ADMIN creates department -> OK
        mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.departmentCode", is("MECH")));

        // Duplicate code -> Conflict (409)
        mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // FACULTY tries to create -> Forbidden
        mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testDeleteDepartment_AdminOnly() throws Exception {
        // FACULTY tries to delete -> Forbidden
        mockMvc.perform(delete("/api/v1/departments/" + eceDept.getId())
                        .header("Authorization", facultyToken))
                .andExpect(status().isForbidden());

        // ADMIN deletes -> OK
        mockMvc.perform(delete("/api/v1/departments/" + eceDept.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());

        // Verify deleted department returns 404
        mockMvc.perform(get("/api/v1/departments/" + eceDept.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDepartmentDashboardAPI() throws Exception {
        // Fetch dashboard for CSE department (student count should be 1)
        mockMvc.perform(get("/api/v1/departments/" + cseDept.getId() + "/dashboard")
                        .header("Authorization", hodToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalStudents", is(1)))
                .andExpect(jsonPath("$.data.hod.email", is("hod@secureiq.com")));

        // Fetch dashboard for ECE department by HOD -> Forbidden
        mockMvc.perform(get("/api/v1/departments/" + eceDept.getId() + "/dashboard")
                        .header("Authorization", hodToken))
                .andExpect(status().isForbidden());
    }
}
