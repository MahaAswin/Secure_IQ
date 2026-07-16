package com.secureiq.SecureIQ.question;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
import com.secureiq.SecureIQ.question.dto.*;
import com.secureiq.SecureIQ.question.model.Difficulty;
import com.secureiq.SecureIQ.question.model.Question;
import com.secureiq.SecureIQ.question.model.QuestionType;
import com.secureiq.SecureIQ.question.repository.QuestionRepository;
import com.secureiq.SecureIQ.questionbank.dto.QuestionBankCreateRequest;
import com.secureiq.SecureIQ.questionbank.dto.QuestionBankResponse;
import com.secureiq.SecureIQ.questionbank.model.QuestionBank;
import com.secureiq.SecureIQ.questionbank.repository.QuestionBankRepository;
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
import org.springframework.mock.web.MockMultipartFile;
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
public class QuestionControllerTest {

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
    private QuestionBankRepository questionBankRepository;

    @Autowired
    private QuestionRepository questionRepository;

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

    private QuestionBank cseBank;

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

        // Link student to CSE
        studentRepository.save(Student.builder()
                .user(studentUser)
                .registerNumber("REG1002")
                .rollNumber("ROLL1002")
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

        // Create initial Question Bank
        cseBank = questionBankRepository.save(QuestionBank.builder()
                .bankName("CSE Java QB")
                .description("CSE Java Question Bank Description")
                .subject(javaSubject)
                .department(cseDept)
                .createdBy(adminUser)
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

        jdbcTemplate.execute("DELETE FROM browser_sessions");
        jdbcTemplate.execute("DELETE FROM violations");
        jdbcTemplate.execute("DELETE FROM student_exam_attempts");
        jdbcTemplate.execute("DELETE FROM session_students");
        jdbcTemplate.execute("DELETE FROM exam_sessions");
        jdbcTemplate.execute("DELETE FROM question_options");
        jdbcTemplate.execute("DELETE FROM questions");
        jdbcTemplate.execute("DELETE FROM question_banks");
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
    public void testGetQuestionBanks_AccessPermissions() throws Exception {
        // ADMIN can see all question banks
        mockMvc.perform(get("/api/v1/question-banks")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)));

        // STUDENT is forbidden
        mockMvc.perform(get("/api/v1/question-banks")
                        .header("Authorization", studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreateQuestionBank_ValidationsAndSecurity() throws Exception {
        QuestionBankCreateRequest request = QuestionBankCreateRequest.builder()
                .bankName("CSE Java QB Unique")
                .description("Desc")
                .subjectId(javaSubject.getId())
                .departmentId(cseDept.getId())
                .build();

        // HOD creates in own department -> OK
        mockMvc.perform(post("/api/v1/question-banks")
                        .header("Authorization", hodToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bankName", is("CSE Java QB Unique")));

        // Duplicate name within same subject -> Conflict
        mockMvc.perform(post("/api/v1/question-banks")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // HOD creates in ECE department -> Forbidden
        request.setBankName("ECE DSP QB");
        request.setSubjectId(dspSubject.getId());
        request.setDepartmentId(eceDept.getId());
        mockMvc.perform(post("/api/v1/question-banks")
                        .header("Authorization", hodToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreateQuestion_MCQValidationAndCRUD() throws Exception {
        // MCQ request with only 1 option -> Bad Request
        QuestionCreateRequest badMcq = QuestionCreateRequest.builder()
                .questionText("What is Java?")
                .questionType(QuestionType.MCQ)
                .difficulty(Difficulty.EASY)
                .marks(5)
                .options(List.of("A programming language"))
                .correctAnswer("A programming language")
                .bankId(cseBank.getId())
                .build();

        mockMvc.perform(post("/api/v1/questions")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badMcq)))
                .andExpect(status().isBadRequest());

        // Valid MCQ question -> OK
        QuestionCreateRequest validMcq = QuestionCreateRequest.builder()
                .questionText("What is Java?")
                .questionType(QuestionType.MCQ)
                .difficulty(Difficulty.EASY)
                .marks(5)
                .options(List.of("Language", "Coffee", "Car", "Animal"))
                .correctAnswer("Language")
                .bankId(cseBank.getId())
                .build();

        mockMvc.perform(post("/api/v1/questions")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validMcq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questionText", is("What is Java?")))
                .andExpect(jsonPath("$.data.options", hasSize(4)));

        // Retrieve the question list
        mockMvc.perform(get("/api/v1/questions")
                        .header("Authorization", facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));
    }

    @Test
    public void testQuestionDashboard_Metrics() throws Exception {
        // Insert a couple of questions
        questionRepository.save(Question.builder()
                .questionText("Q1")
                .questionType(QuestionType.TRUE_FALSE)
                .difficulty(Difficulty.EASY)
                .marks(2)
                .correctAnswer("True")
                .bank(cseBank)
                .build());

        questionRepository.save(Question.builder()
                .questionText("Q2")
                .questionType(QuestionType.DESCRIPTIVE)
                .difficulty(Difficulty.HARD)
                .marks(10)
                .correctAnswer("Explanation")
                .bank(cseBank)
                .build());

        // Fetch Dashboard
        mockMvc.perform(get("/api/v1/questions/dashboard")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQuestionBanks", is(1)))
                .andExpect(jsonPath("$.data.totalQuestions", is(2)))
                .andExpect(jsonPath("$.data.questionsBySubject.CSE301", is(2)))
                .andExpect(jsonPath("$.data.questionsByDifficulty.EASY", is(1)))
                .andExpect(jsonPath("$.data.questionsByDifficulty.HARD", is(1)));
    }

    @Test
    public void testBulkImport_CSVPlaceholder() throws Exception {
        String csvContent = "questionText,questionType,difficulty,marks,options,correctAnswer,explanation,imageUrl\n" +
                "\"Which keyword is used for inheritance in Java?\",MCQ,EASY,2,\"extends;implements;imports;inherits\",extends,\"Inheritance uses extends\",\n" +
                "\"Java is multi-threaded.\",TRUE_FALSE,EASY,2,\"\",True,\"Java supports multi-threading\",\n" +
                "\"Explain JVM.\",DESCRIPTIVE,MEDIUM,5,\"\",JVM stands for Java Virtual Machine,JVM executes bytecode,\n";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "questions.csv",
                MediaType.TEXT_PLAIN_VALUE,
                csvContent.getBytes()
        );

        mockMvc.perform(multipart("/api/v1/questions/bulk-import")
                        .file(file)
                        .param("bankId", cseBank.getId().toString())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount", is(3)))
                .andExpect(jsonPath("$.data.failureCount", is(0)));

        // Assert size of questions in repo
        mockMvc.perform(get("/api/v1/questions")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(3)));
    }
}
