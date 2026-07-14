package com.secureiq.SecureIQ.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secureiq.SecureIQ.security.jwt.JwtTokenProvider;
import com.secureiq.SecureIQ.user.dto.UserCreateRequest;
import com.secureiq.SecureIQ.user.dto.UserStatusUpdateRequest;
import com.secureiq.SecureIQ.user.dto.UserUpdateRequest;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class UserControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

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

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        cleanupAll();

        adminUser = User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@secureiq.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.ADMIN)
                .status(Status.ACTIVE)
                .build();
        adminUser = userRepository.save(adminUser);
        adminToken = "Bearer " + tokenProvider.generateToken(adminUser.getEmail());

        hodUser = User.builder()
                .firstName("Hod")
                .lastName("User")
                .email("hod@secureiq.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.HOD)
                .status(Status.ACTIVE)
                .build();
        hodUser = userRepository.save(hodUser);
        hodToken = "Bearer " + tokenProvider.generateToken(hodUser.getEmail());

        facultyUser = User.builder()
                .firstName("Faculty")
                .lastName("User")
                .email("faculty@secureiq.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.FACULTY)
                .status(Status.ACTIVE)
                .build();
        facultyUser = userRepository.save(facultyUser);
        facultyToken = "Bearer " + tokenProvider.generateToken(facultyUser.getEmail());

        studentUser = User.builder()
                .firstName("Student")
                .lastName("User")
                .email("student@secureiq.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.STUDENT)
                .status(Status.ACTIVE)
                .build();
        studentUser = userRepository.save(studentUser);
        studentToken = "Bearer " + tokenProvider.generateToken(studentUser.getEmail());
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
    public void testGetUsersList_AdminAndHODAccess() throws Exception {
        // ADMIN access
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(4)));

        // HOD access
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", hodToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // FACULTY forbidden
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", facultyToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetUserById_AuthorizationMatrix() throws Exception {
        // ADMIN can read HOD profile
        mockMvc.perform(get("/api/v1/users/" + hodUser.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email", is(hodUser.getEmail())));

        // HOD can read STUDENT profile
        mockMvc.perform(get("/api/v1/users/" + studentUser.getId())
                        .header("Authorization", hodToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email", is(studentUser.getEmail())));

        // FACULTY can read own profile
        mockMvc.perform(get("/api/v1/users/" + facultyUser.getId())
                        .header("Authorization", facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email", is(facultyUser.getEmail())));

        // FACULTY cannot read STUDENT profile
        mockMvc.perform(get("/api/v1/users/" + studentUser.getId())
                        .header("Authorization", facultyToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreateUser_ValidAndUniqueValidation() throws Exception {
        UserCreateRequest request = UserCreateRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@secureiq.com")
                .password("password123")
                .role(Role.STUDENT)
                .status(Status.ACTIVE)
                .build();

        // Admin successfully creates user
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("john.doe@secureiq.com")));

        // Email uniqueness validation -> Conflict
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // HOD cannot create user -> Forbidden
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", hodToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreateUser_ValidationFailures() throws Exception {
        UserCreateRequest request = UserCreateRequest.builder()
                .firstName("") // Blank
                .lastName("Doe")
                .email("invalid-email") // Invalid email format
                .password("pwd") // Less than 6 chars
                .role(null) // Null role
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Validation error")));
    }

    @Test
    public void testUpdateUser_AdminOnly() throws Exception {
        UserUpdateRequest request = UserUpdateRequest.builder()
                .firstName("FacultyUpdated")
                .lastName("User")
                .email("faculty.updated@secureiq.com")
                .role(Role.FACULTY)
                .status(Status.ACTIVE)
                .build();

        // Admin updates faculty
        mockMvc.perform(put("/api/v1/users/" + facultyUser.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName", is("FacultyUpdated")))
                .andExpect(jsonPath("$.data.email", is("faculty.updated@secureiq.com")));

        // HOD cannot update faculty -> Forbidden
        mockMvc.perform(put("/api/v1/users/" + facultyUser.getId())
                        .header("Authorization", hodToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testPatchUserStatus_AdminOnly() throws Exception {
        UserStatusUpdateRequest request = UserStatusUpdateRequest.builder()
                .status(Status.BLOCKED)
                .build();

        mockMvc.perform(patch("/api/v1/users/" + studentUser.getId() + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("BLOCKED")));
    }

    @Test
    public void testSoftDeleteAndEmailReusability() throws Exception {
        // Admin deletes faculty user
        mockMvc.perform(delete("/api/v1/users/" + facultyUser.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Retrieve deleted profile should return 404 (due to @SQLRestriction("deleted = false"))
        mockMvc.perform(get("/api/v1/users/" + facultyUser.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());

        // Try creating a new user with same email should work (because email suffix was appended in deleted user)
        UserCreateRequest request = UserCreateRequest.builder()
                .firstName("New")
                .lastName("Faculty")
                .email("faculty@secureiq.com") // Same email
                .password("password123")
                .role(Role.FACULTY)
                .status(Status.ACTIVE)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email", is("faculty@secureiq.com")));
    }

    @Test
    public void testPaginationAndSearchFilters() throws Exception {
        // Create matching/non-matching users
        User user1 = userRepository.save(User.builder().firstName("Alice").lastName("Smith").email("alice@secureiq.com").password("pass").role(Role.STUDENT).status(Status.ACTIVE).build());
        User user2 = userRepository.save(User.builder().firstName("Bob").lastName("Smith").email("bob@secureiq.com").password("pass").role(Role.STUDENT).status(Status.ACTIVE).build());

        // Search by name "alice"
        mockMvc.perform(get("/api/v1/users?name=alice")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].firstName", is("Alice")));

        // Search by last name "smith" (case-insensitive) -> should match Alice and Bob
        mockMvc.perform(get("/api/v1/users?name=SMITH")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));

        // Search by email
        mockMvc.perform(get("/api/v1/users?email=bob@")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].firstName", is("Bob")));

        // Pagination and Sorting: sort by firstName desc
        mockMvc.perform(get("/api/v1/users?sortBy=firstName&sortDir=desc&page=0&size=2")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].firstName", not(isEmptyOrNullString())));
    }

    @Test
    public void testUserRegisterWithFullName() throws Exception {
        java.util.Map<String, Object> signupRequest = new java.util.HashMap<>();
        signupRequest.put("name", "Alice Smith");
        signupRequest.put("email", "alicesmith@secureiq.com");
        signupRequest.put("password", "securepassword");
        signupRequest.put("role", "STUDENT");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", notNullValue()));

        // Verify user was stored with split names
        User registered = userRepository.findByEmail("alicesmith@secureiq.com").orElse(null);
        org.junit.jupiter.api.Assertions.assertNotNull(registered);
        org.junit.jupiter.api.Assertions.assertEquals("Alice", registered.getFirstName());
        org.junit.jupiter.api.Assertions.assertEquals("Smith", registered.getLastName());
    }

    @Test
    public void testUserRegisterWithSingleWordName() throws Exception {
        java.util.Map<String, Object> signupRequest = new java.util.HashMap<>();
        signupRequest.put("name", "Student");
        signupRequest.put("email", "studentname@secureiq.com");
        signupRequest.put("password", "securepassword");
        signupRequest.put("role", "STUDENT");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk());

        User registered = userRepository.findByEmail("studentname@secureiq.com").orElse(null);
        org.junit.jupiter.api.Assertions.assertNotNull(registered);
        org.junit.jupiter.api.Assertions.assertEquals("Student", registered.getFirstName());
        org.junit.jupiter.api.Assertions.assertEquals("", registered.getLastName());
    }

    @Test
    public void testUserRegisterWithSplitFirstAndLastName() throws Exception {
        java.util.Map<String, Object> signupRequest = new java.util.HashMap<>();
        signupRequest.put("firstName", "Bob");
        signupRequest.put("lastName", "Smith");
        signupRequest.put("email", "bobsmith@secureiq.com");
        signupRequest.put("password", "securepassword");
        signupRequest.put("role", "STUDENT");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk());

        User registered = userRepository.findByEmail("bobsmith@secureiq.com").orElse(null);
        org.junit.jupiter.api.Assertions.assertNotNull(registered);
        org.junit.jupiter.api.Assertions.assertEquals("Bob", registered.getFirstName());
        org.junit.jupiter.api.Assertions.assertEquals("Smith", registered.getLastName());
    }

    @Test
    public void testUpdateOwnProfile_Success() throws Exception {
        UserUpdateRequest request = UserUpdateRequest.builder()
                .firstName("FacultyNewName")
                .lastName("User")
                .email("faculty@secureiq.com")
                .role(Role.FACULTY)
                .status(Status.ACTIVE)
                .build();

        mockMvc.perform(put("/api/v1/users/" + facultyUser.getId())
                        .header("Authorization", facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.firstName", is("FacultyNewName")));
    }

    @Test
    public void testUpdateOwnProfile_RoleStatusChangeRejected() throws Exception {
        UserUpdateRequest request = UserUpdateRequest.builder()
                .firstName("FacultyNewName")
                .lastName("User")
                .email("faculty@secureiq.com")
                .role(Role.ADMIN)
                .status(Status.ACTIVE)
                .build();

        mockMvc.perform(put("/api/v1/users/" + facultyUser.getId())
                        .header("Authorization", facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testPatchOwnProfile_Success() throws Exception {
        com.secureiq.SecureIQ.user.dto.UserPatchRequest request = com.secureiq.SecureIQ.user.dto.UserPatchRequest.builder()
                .firstName("FacultyPatched")
                .build();

        mockMvc.perform(patch("/api/v1/users/" + facultyUser.getId())
                        .header("Authorization", facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.firstName", is("FacultyPatched")))
                .andExpect(jsonPath("$.data.email", is("faculty@secureiq.com")));
    }

    @Test
    public void testPatchOwnProfile_RoleStatusChangeRejected() throws Exception {
        com.secureiq.SecureIQ.user.dto.UserPatchRequest request = com.secureiq.SecureIQ.user.dto.UserPatchRequest.builder()
                .role(Role.ADMIN)
                .build();

        mockMvc.perform(patch("/api/v1/users/" + facultyUser.getId())
                        .header("Authorization", facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testPatchOtherProfile_Forbidden() throws Exception {
        com.secureiq.SecureIQ.user.dto.UserPatchRequest request = com.secureiq.SecureIQ.user.dto.UserPatchRequest.builder()
                .firstName("Blocked")
                .build();

        mockMvc.perform(patch("/api/v1/users/" + studentUser.getId())
                        .header("Authorization", facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}

