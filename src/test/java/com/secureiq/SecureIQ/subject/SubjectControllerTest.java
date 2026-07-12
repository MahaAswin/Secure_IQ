package com.secureiq.SecureIQ.subject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secureiq.SecureIQ.security.jwt.JwtTokenProvider;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
import com.secureiq.SecureIQ.student.model.Student;
import com.secureiq.SecureIQ.student.repository.StudentRepository;
import com.secureiq.SecureIQ.subject.dto.*;
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

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class SubjectControllerTest {

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

        // Link student to CSE and Semester 3
        studentRepository.save(Student.builder()
                .user(studentUser)
                .registerNumber("REG1001")
                .rollNumber("ROLL1001")
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

        dspSubject = subjectRepository.save(Subject.builder()
                .subjectCode("ECE401")
                .subjectName("Digital Signal Processing")
                .credits(3)
                .semester(4)
                .regulation("R2021")
                .department(eceDept)
                .faculty(Set.of())
                .build());
    }

    @AfterEach
    public void cleanup() {
        cleanupAll();
    }

    private void cleanupAll() {
        jdbcTemplate.execute("DELETE FROM subject_faculty");
        jdbcTemplate.execute("DELETE FROM subjects");
        jdbcTemplate.execute("DELETE FROM students");
        jdbcTemplate.execute("DELETE FROM departments");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    public void testGetSubjectsList_AdminAccess() throws Exception {
        // ADMIN can read all
        mockMvc.perform(get("/api/v1/subjects")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    public void testGetSubjectsList_FacultyRestrictedToAssigned() throws Exception {
        // Faculty is assigned to Java Programming (CSE301), but not DSP (ECE401).
        mockMvc.perform(get("/api/v1/subjects")
                        .header("Authorization", facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].subjectCode", is("CSE301")));
    }

    @Test
    public void testGetSubjectsList_StudentRestrictedToOwnDeptAndSem() throws Exception {
        // Student is CSE and Semester 3. Can see CSE301 (Java), but not ECE401 (DSP, sem 4).
        mockMvc.perform(get("/api/v1/subjects")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].subjectCode", is("CSE301")));

        // Student tries to query ECE -> Forbidden
        mockMvc.perform(get("/api/v1/subjects?departmentId=" + eceDept.getId())
                        .header("Authorization", studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreateSubject_HODPermissionsAndConflict() throws Exception {
        SubjectCreateRequest request = SubjectCreateRequest.builder()
                .subjectCode("CSE302")
                .subjectName("Data Structures")
                .credits(4)
                .semester(3)
                .departmentId(cseDept.getId())
                .build();

        // HOD creates subject in their own department (CSE) -> OK
        mockMvc.perform(post("/api/v1/subjects")
                        .header("Authorization", hodToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subjectCode", is("CSE302")));

        // HOD tries to create in ECE department -> Forbidden
        request.setSubjectCode("ECE402");
        request.setDepartmentId(eceDept.getId());
        mockMvc.perform(post("/api/v1/subjects")
                        .header("Authorization", hodToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Duplicate code -> Conflict (409)
        request.setSubjectCode("CSE301"); // existing code
        request.setDepartmentId(cseDept.getId());
        mockMvc.perform(post("/api/v1/subjects")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    public void testCreateSubject_ValidationConstraints() throws Exception {
        // Invalid Credits (negative) and invalid Semester (9)
        SubjectCreateRequest request = SubjectCreateRequest.builder()
                .subjectCode("CSE303")
                .subjectName("") // Invalid: Blank
                .credits(-1) // Invalid: Negative
                .semester(9) // Invalid: Max 8
                .departmentId(cseDept.getId())
                .build();

        mockMvc.perform(post("/api/v1/subjects")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDeleteSubject_Permissions() throws Exception {
        // HOD tries to delete ECE subject -> Forbidden
        mockMvc.perform(delete("/api/v1/subjects/" + dspSubject.getId())
                        .header("Authorization", hodToken))
                .andExpect(status().isForbidden());

        // HOD deletes own department subject (Java) -> OK
        mockMvc.perform(delete("/api/v1/subjects/" + javaSubject.getId())
                        .header("Authorization", hodToken))
                .andExpect(status().isOk());

        // Fetch soft-deleted subject -> NotFound (404)
        mockMvc.perform(get("/api/v1/subjects/" + javaSubject.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSubjectDashboardAPI() throws Exception {
        // ADMIN dashboard details
        mockMvc.perform(get("/api/v1/subjects/dashboard")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSubjects", is(2)))
                .andExpect(jsonPath("$.data.subjectsBySemester['3']", is(1)))
                .andExpect(jsonPath("$.data.subjectsByDepartment.CSE", is(1)));

        // STUDENT dashboard details (can only see CSE / Sem 3)
        mockMvc.perform(get("/api/v1/subjects/dashboard")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSubjects", is(1)))
                .andExpect(jsonPath("$.data.subjectsBySemester['3']", is(1)))
                .andExpect(jsonPath("$.data.subjectsByDepartment.CSE", is(1)));
    }
}
