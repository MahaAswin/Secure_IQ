package com.secureiq.SecureIQ.faculty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secureiq.SecureIQ.security.jwt.JwtTokenProvider;
import com.secureiq.SecureIQ.faculty.dto.*;
import com.secureiq.SecureIQ.faculty.model.*;
import com.secureiq.SecureIQ.faculty.repository.*;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
import com.secureiq.SecureIQ.subject.model.Subject;
import com.secureiq.SecureIQ.subject.repository.SubjectRepository;
import com.secureiq.SecureIQ.student.model.Student;
import com.secureiq.SecureIQ.exam.model.*;
import com.secureiq.SecureIQ.student.model.RecentActivity;
import com.secureiq.SecureIQ.student.repository.StudentRepository;
import com.secureiq.SecureIQ.exam.repository.ExamRepository;
import com.secureiq.SecureIQ.student.repository.RecentActivityRepository;
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
public class FacultyControllerTest {

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
    private RecentActivityRepository recentActivityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;
    private User hodUser;
    private User facultyUser1;
    private User facultyUser2;
    private User studentUser;

    private String adminToken;
    private String hodToken;
    private String facultyToken1;
    private String facultyToken2;
    private String studentToken;

    private Department cseDept;
    private Department eceDept;

    private Subject javaSubject;
    private Subject dspSubject;

    private Faculty facultyProfile1;
    private Faculty facultyProfile2;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        cleanupAll();

        // Create Users
        adminUser = userRepository.save(User.builder().firstName("Admin").lastName("User").email("admin.fac@secureiq.com").password(passwordEncoder.encode("password")).role(Role.ADMIN).status(Status.ACTIVE).build());
        adminToken = "Bearer " + tokenProvider.generateToken(adminUser.getEmail());

        hodUser = userRepository.save(User.builder().firstName("Hod").lastName("User").email("hod.fac@secureiq.com").password(passwordEncoder.encode("password")).role(Role.HOD).status(Status.ACTIVE).build());
        hodToken = "Bearer " + tokenProvider.generateToken(hodUser.getEmail());

        facultyUser1 = userRepository.save(User.builder().firstName("FacultyOne").lastName("User").email("fac1@secureiq.com").password(passwordEncoder.encode("password")).role(Role.FACULTY).status(Status.ACTIVE).build());
        facultyToken1 = "Bearer " + tokenProvider.generateToken(facultyUser1.getEmail());

        facultyUser2 = userRepository.save(User.builder().firstName("FacultyTwo").lastName("User").email("fac2@secureiq.com").password(passwordEncoder.encode("password")).role(Role.FACULTY).status(Status.ACTIVE).build());
        facultyToken2 = "Bearer " + tokenProvider.generateToken(facultyUser2.getEmail());

        studentUser = userRepository.save(User.builder().firstName("Student").lastName("User").email("student.fac@secureiq.com").password(passwordEncoder.encode("password")).role(Role.STUDENT).status(Status.ACTIVE).build());
        studentToken = "Bearer " + tokenProvider.generateToken(studentUser.getEmail());

        // Create Departments
        cseDept = departmentRepository.save(Department.builder().departmentName("Computer Science").departmentCode("CSE").hod(hodUser).build());
        eceDept = departmentRepository.save(Department.builder().departmentName("Electronics").departmentCode("ECE").build());

        // Create Subjects
        javaSubject = subjectRepository.save(Subject.builder()
                .subjectCode("CSE101")
                .subjectName("Java Programming")
                .credits(4)
                .semester(1)
                .department(cseDept)
                .faculty(Set.of(facultyUser1))
                .build());

        dspSubject = subjectRepository.save(Subject.builder()
                .subjectCode("ECE201")
                .subjectName("DSP")
                .credits(3)
                .semester(2)
                .department(eceDept)
                .faculty(Set.of(facultyUser2))
                .build());

        // Create Faculty profiles
        facultyProfile1 = facultyRepository.save(Faculty.builder()
                .user(facultyUser1)
                .employeeId("EMP1001")
                .designation("Assistant Professor")
                .qualification("Ph.D")
                .specialization("Distributed Systems")
                .yearsOfExperience(8)
                .officeLocation("Room 101")
                .department(cseDept)
                .joiningDate(LocalDate.of(2020, 6, 1))
                .profileCompleted(true)
                .subjects(Set.of(javaSubject))
                .build());

        facultyProfile2 = facultyRepository.save(Faculty.builder()
                .user(facultyUser2)
                .employeeId("EMP2002")
                .designation("Associate Professor")
                .qualification("M.Tech")
                .specialization("VLSI")
                .yearsOfExperience(12)
                .officeLocation("Room 202")
                .department(eceDept)
                .joiningDate(LocalDate.of(2018, 7, 15))
                .profileCompleted(true)
                .subjects(Set.of(dspSubject))
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
    public void testGetFacultiesList_AdminAndHODAccess() throws Exception {
        // ADMIN can read all
        mockMvc.perform(get("/api/v1/faculties")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(2)));

        // HOD user is HOD of CSE department -> can only read facultyProfile1 (CSE)
        mockMvc.perform(get("/api/v1/faculties")
                        .header("Authorization", hodToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].employeeId", is("EMP1001")));
    }

    @Test
    public void testGetFacultyById_AuthorizationMatrix() throws Exception {
        // ADMIN reads any faculty
        mockMvc.perform(get("/api/v1/faculties/" + facultyProfile2.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeId", is("EMP2002")));

        // FACULTY reads own profile
        mockMvc.perform(get("/api/v1/faculties/" + facultyProfile1.getId())
                        .header("Authorization", facultyToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeId", is("EMP1001")));

        // FACULTY tries to read other department's faculty -> Forbidden
        mockMvc.perform(get("/api/v1/faculties/" + facultyProfile2.getId())
                        .header("Authorization", facultyToken1))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreateFaculty_SuccessAndValidation() throws Exception {
        // Create a new unlinked user with role FACULTY
        User newFacultyUser = userRepository.save(User.builder().firstName("New").lastName("Faculty").email("newfac@secureiq.com").password("password").role(Role.FACULTY).status(Status.ACTIVE).build());

        FacultyCreateRequest request = FacultyCreateRequest.builder()
                .userId(newFacultyUser.getId())
                .employeeId("EMP3003")
                .designation("Professor")
                .qualification("Ph.D")
                .specialization("Algorithms")
                .yearsOfExperience(15)
                .officeLocation("Room 303")
                .departmentId(cseDept.getId())
                .joiningDate(LocalDate.now().minusYears(1))
                .subjectIds(List.of(javaSubject.getId()))
                .build();

        // Admin successfully creates faculty profile
        mockMvc.perform(post("/api/v1/faculties")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.employeeId", is("EMP3003")));

        // Duplicate employeeId -> Conflict
        request.setUserId(newFacultyUser.getId());
        mockMvc.perform(post("/api/v1/faculties")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // Joining date in future -> Bad Request (Validation failure)
        request.setEmployeeId("EMP4004");
        request.setJoiningDate(LocalDate.now().plusDays(5));
        mockMvc.perform(post("/api/v1/faculties")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateFaculty_Success() throws Exception {
        FacultyUpdateRequest request = FacultyUpdateRequest.builder()
                .employeeId("EMP1001")
                .designation("Senior Professor") // Updated field
                .qualification("Ph.D")
                .specialization("Distributed Systems")
                .yearsOfExperience(9) // Updated field
                .officeLocation("Room 101-B") // Updated field
                .departmentId(cseDept.getId())
                .joiningDate(LocalDate.of(2020, 6, 1))
                .profileCompleted(true)
                .subjectIds(List.of(javaSubject.getId()))
                .build();

        // Faculty updates own profile -> OK
        mockMvc.perform(put("/api/v1/faculties/" + facultyProfile1.getId())
                        .header("Authorization", facultyToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.designation", is("Senior Professor")))
                .andExpect(jsonPath("$.data.officeLocation", is("Room 101-B")));

        // Faculty tries to update other faculty profile -> Forbidden
        mockMvc.perform(put("/api/v1/faculties/" + facultyProfile2.getId())
                        .header("Authorization", facultyToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testDeleteFaculty_SoftDeleteAndPermissions() throws Exception {
        // Faculty tries to delete -> Forbidden
        mockMvc.perform(delete("/api/v1/faculties/" + facultyProfile1.getId())
                        .header("Authorization", facultyToken1))
                .andExpect(status().isForbidden());

        // Admin soft deletes facultyProfile1 -> OK
        mockMvc.perform(delete("/api/v1/faculties/" + facultyProfile1.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());

        // Verify soft-deleted faculty profile returns 404
        mockMvc.perform(get("/api/v1/faculties/" + facultyProfile1.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testFacultyDashboardApis() throws Exception {
        // Set up Student in CSE department
        Student cseStudent = studentRepository.save(Student.builder()
                .user(studentUser)
                .registerNumber("REG7007")
                .rollNumber("ROLL7007")
                .department(cseDept)
                .academicYear("2026")
                .semester(1)
                .build());

        // Upcoming Exam in CSE department
        examRepository.save(Exam.builder()
                .examCode("EX-MIDTERM")
                .examTitle("Mid Term Exam")
                .subject(javaSubject)
                .faculty(facultyProfile1)
                .semester(1)
                .examType(ExamType.MID_SEMESTER)
                .totalMarks(50)
                .passingMarks(20)
                .durationMinutes(90)
                .scheduledDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .status(ExamStatus.ACTIVE)
                .department(cseDept)
                .build());

        // Recent student activity for cseStudent
        recentActivityRepository.save(RecentActivity.builder()
                .title("Assigned Activity")
                .description("Solved quiz 1")
                .studentId(cseStudent.getId())
                .timestamp(LocalDateTime.now())
                .build());

        // Verify combined dashboard endpoint
        mockMvc.perform(get("/api/v1/faculties/dashboard")
                        .header("Authorization", facultyToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.assignedSubjectsCount", is(1)))
                .andExpect(jsonPath("$.data.assignedStudentsCount", is(1)))
                .andExpect(jsonPath("$.data.upcomingExamsCount", is(1)))
                .andExpect(jsonPath("$.data.recentActivities", hasSize(1)));

        // Verify sub-endpoints
        mockMvc.perform(get("/api/v1/faculties/dashboard/subjects")
                        .header("Authorization", facultyToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].subjectCode", is("CSE101")));

        mockMvc.perform(get("/api/v1/faculties/dashboard/students")
                        .header("Authorization", facultyToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].registerNumber", is("REG7007")));

        mockMvc.perform(get("/api/v1/faculties/dashboard/exams")
                        .header("Authorization", facultyToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title", is("Mid Term Exam")));

        mockMvc.perform(get("/api/v1/faculties/dashboard/activities")
                        .header("Authorization", facultyToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title", is("Assigned Activity")));
    }
}
