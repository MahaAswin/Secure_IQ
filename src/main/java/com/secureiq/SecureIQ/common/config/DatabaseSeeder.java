package com.secureiq.SecureIQ.common.config;

import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
import com.secureiq.SecureIQ.faculty.model.Faculty;
import com.secureiq.SecureIQ.faculty.repository.FacultyRepository;
import com.secureiq.SecureIQ.student.model.Student;
import com.secureiq.SecureIQ.student.repository.StudentRepository;
import com.secureiq.SecureIQ.user.model.Role;
import com.secureiq.SecureIQ.user.model.Status;
import com.secureiq.SecureIQ.user.model.User;
import com.secureiq.SecureIQ.user.repository.UserRepository;
import com.secureiq.SecureIQ.subject.model.Subject;
import com.secureiq.SecureIQ.subject.repository.SubjectRepository;
import com.secureiq.SecureIQ.exam.model.Exam;
import com.secureiq.SecureIQ.exam.model.ExamStatus;
import com.secureiq.SecureIQ.exam.model.ExamType;
import com.secureiq.SecureIQ.exam.repository.ExamRepository;
import com.secureiq.SecureIQ.examsession.model.ExamSession;
import com.secureiq.SecureIQ.examsession.model.SessionStatus;
import com.secureiq.SecureIQ.examsession.repository.ExamSessionRepository;
import java.time.LocalDateTime;
import com.secureiq.SecureIQ.question.model.Question;
import com.secureiq.SecureIQ.question.model.QuestionType;
import com.secureiq.SecureIQ.question.model.Difficulty;
import com.secureiq.SecureIQ.question.repository.QuestionRepository;
import com.secureiq.SecureIQ.questionbank.model.QuestionBank;
import com.secureiq.SecureIQ.questionbank.repository.QuestionBankRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final ExamRepository examRepository;
    private final ExamSessionRepository examSessionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(UserRepository userRepository,
                          DepartmentRepository departmentRepository,
                          FacultyRepository facultyRepository,
                          StudentRepository studentRepository,
                          SubjectRepository subjectRepository,
                          ExamRepository examRepository,
                          ExamSessionRepository examSessionRepository,
                          QuestionRepository questionRepository,
                          QuestionBankRepository questionBankRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.facultyRepository = facultyRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.examRepository = examRepository;
        this.examSessionRepository = examSessionRepository;
        this.questionRepository = questionRepository;
        this.questionBankRepository = questionBankRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Only seed if database is unseeded (checking if admin user exists)
        if (userRepository.existsByEmail("admin@secureiq.edu")) {
            if (subjectRepository.count() == 0) {
                System.out.println("Seeding subjects and exams for existing database...");
                Department cseDept = departmentRepository.findByDepartmentCode("CS")
                        .orElseThrow(() -> new RuntimeException("CS department not found"));
                User facultyUser = userRepository.findByEmail("meera.krishnan@secureiq.edu")
                        .orElseThrow(() -> new RuntimeException("Faculty user not found"));
                Faculty facultyProfile = facultyRepository.findByUserId(facultyUser.getId())
                        .orElseThrow(() -> new RuntimeException("Faculty profile not found"));

                Subject dsSubject = Subject.builder()
                        .subjectCode("CS301")
                        .subjectName("Data Structures")
                        .credits(4)
                        .semester(3)
                        .department(cseDept)
                        .faculty(Set.of(facultyUser))
                        .build();
                dsSubject = subjectRepository.save(dsSubject);

                Subject algoSubject = Subject.builder()
                        .subjectCode("CS302")
                        .subjectName("Algorithms")
                        .credits(4)
                        .semester(3)
                        .department(cseDept)
                        .faculty(Set.of(facultyUser))
                        .build();
                algoSubject = subjectRepository.save(algoSubject);

                Subject dbSubject = Subject.builder()
                        .subjectCode("CS303")
                        .subjectName("Database Systems")
                        .credits(4)
                        .semester(3)
                        .department(cseDept)
                        .faculty(Set.of(facultyUser))
                        .build();
                dbSubject = subjectRepository.save(dbSubject);

                facultyProfile.setSubjects(new HashSet<>(Arrays.asList(dsSubject, algoSubject, dbSubject)));
                facultyRepository.save(facultyProfile);

                Exam dsExam = Exam.builder()
                        .examCode("CS301-MID-SEM")
                        .examTitle("Data Structures Mid-Semester")
                        .description("Mid-semester examination for Data Structures and Algorithms course.")
                        .subject(dsSubject)
                        .faculty(facultyProfile)
                        .department(cseDept)
                        .semester(3)
                        .examType(ExamType.MID_SEMESTER)
                        .totalMarks(100)
                        .durationMinutes(120)
                        .passingMarks(40)
                        .scheduledDate(LocalDate.now().plusDays(2))
                        .startTime(LocalTime.of(9, 30))
                        .endTime(LocalTime.of(11, 30))
                        .venue("Seminar Hall 1")
                        .instructions("1. No electronic items allowed.\n2. Bring your hall ticket.")
                        .status(ExamStatus.PUBLISHED)
                        .build();
                examRepository.save(dsExam);

                Exam dbExam = Exam.builder()
                        .examCode("CS303-QUIZ")
                        .examTitle("Database Systems Quiz")
                        .description("In-class quiz for Database Management Systems.")
                        .subject(dbSubject)
                        .faculty(facultyProfile)
                        .department(cseDept)
                        .semester(3)
                        .examType(ExamType.QUIZ)
                        .totalMarks(50)
                        .durationMinutes(60)
                        .passingMarks(20)
                        .scheduledDate(LocalDate.now().plusDays(5))
                        .startTime(LocalTime.of(14, 0))
                        .endTime(LocalTime.of(15, 0))
                        .venue("Lab 3 - Block B")
                        .instructions("1. Closed book quiz.\n2. System login credentials will be provided.")
                        .status(ExamStatus.PUBLISHED)
                        .build();
                examRepository.save(dbExam);

                System.out.println("Subjects and exams seeding completed!");
            }

            if (examSessionRepository.count() == 0) {
                Exam dsExam = examRepository.findByExamCode("CS301-MID-SEM").orElse(null);
                Exam dbExam = examRepository.findByExamCode("CS303-QUIZ").orElse(null);
                if (dsExam != null && dbExam != null) {
                    User studentUserObj = userRepository.findByEmail("arjun.sharma@secureiq.edu").orElse(null);
                    Student studentProfileObj = studentUserObj != null ? studentRepository.findByUserId(studentUserObj.getId()).orElse(null) : null;
                    Set<Student> studentsSet = new HashSet<>();
                    if (studentProfileObj != null) {
                        studentsSet.add(studentProfileObj);
                    }

                    User facultyUser = userRepository.findByEmail("meera.krishnan@secureiq.edu").orElse(null);
                    Faculty facultyProfile = facultyUser != null ? facultyRepository.findByUserId(facultyUser.getId()).orElse(null) : null;

                    if (facultyProfile != null) {
                        ExamSession dsSession = ExamSession.builder()
                                .sessionCode("CS301-MID-SEM-SESSION")
                                .sessionName("Data Structures Mid-Semester Exam Session")
                                .exam(dsExam)
                                .faculty(facultyProfile)
                                .startDateTime(LocalDateTime.of(dsExam.getScheduledDate(), dsExam.getStartTime()))
                                .endDateTime(LocalDateTime.of(dsExam.getScheduledDate(), dsExam.getEndTime()))
                                .venue(dsExam.getVenue())
                                .status(SessionStatus.LIVE)
                                .instructions("Read all instructions carefully.")
                                .students(studentsSet)
                                .build();
                        examSessionRepository.save(dsSession);

                        ExamSession dbSession = ExamSession.builder()
                                .sessionCode("CS303-QUIZ-SESSION")
                                .sessionName("Database Systems Quiz Session")
                                .exam(dbExam)
                                .faculty(facultyProfile)
                                .startDateTime(LocalDateTime.of(dbExam.getScheduledDate(), dbExam.getStartTime()))
                                .endDateTime(LocalDateTime.of(dbExam.getScheduledDate(), dbExam.getEndTime()))
                                .venue(dbExam.getVenue())
                                .status(SessionStatus.SCHEDULED)
                                .instructions("Closed book quiz.")
                                .students(studentsSet)
                                .build();
                        examSessionRepository.save(dbSession);
                        System.out.println("Seeded live exam sessions dynamically on startup");
                    }
                }
            }
            if (questionBankRepository.count() == 0) {
                Subject dsSubject = subjectRepository.findBySubjectCode("CS301").orElse(null);
                Department cseDept = departmentRepository.findByDepartmentCode("CS").orElse(null);
                User facultyUser = userRepository.findByEmail("meera.krishnan@secureiq.edu").orElse(null);

                if (dsSubject != null && cseDept != null && facultyUser != null) {
                    QuestionBank bank = QuestionBank.builder()
                            .bankName("Data Structures Question Bank")
                            .description("Default question bank for Data Structures (CS301)")
                            .subject(dsSubject)
                            .department(cseDept)
                            .createdBy(facultyUser)
                            .build();
                    bank = questionBankRepository.save(bank);

                    Question q1 = Question.builder()
                            .questionText("Which data structure uses LIFO (Last In First Out) ordering?")
                            .questionType(QuestionType.MCQ)
                            .difficulty(Difficulty.EASY)
                            .marks(5)
                            .options(Arrays.asList("Queue", "Stack", "Linked List", "Binary Tree"))
                            .correctAnswer("Stack")
                            .explanation("A stack is a Last In First Out (LIFO) data structure.")
                            .bank(bank)
                            .build();
                    questionRepository.save(q1);

                    Question q2 = Question.builder()
                            .questionText("What is the time complexity of binary search?")
                            .questionType(QuestionType.MCQ)
                            .difficulty(Difficulty.EASY)
                            .marks(5)
                            .options(Arrays.asList("O(n)", "O(n²)", "O(log n)", "O(n log n)"))
                            .correctAnswer("O(log n)")
                            .explanation("Binary search divides the search space in half at each step, resulting in O(log n) complexity.")
                            .bank(bank)
                            .build();
                    questionRepository.save(q2);

                    Question q3 = Question.builder()
                            .questionText("Which sorting algorithm has the best average case complexity?")
                            .questionType(QuestionType.MCQ)
                            .difficulty(Difficulty.MEDIUM)
                            .marks(5)
                            .options(Arrays.asList("Bubble Sort", "Quick Sort", "Merge Sort", "Insertion Sort"))
                            .correctAnswer("Merge Sort")
                            .explanation("Merge Sort has an average and worst-case time complexity of O(n log n), which is optimal for comparison-based sorting.")
                            .bank(bank)
                            .build();
                    questionRepository.save(q3);

                    Question q4 = Question.builder()
                            .questionText("What is the main advantage of a Hash Table?")
                            .questionType(QuestionType.MCQ)
                            .difficulty(Difficulty.MEDIUM)
                            .marks(5)
                            .options(Arrays.asList("Constant time search on average", "Ordered traversal of elements", "Uses minimum memory", "Easy to implement"))
                            .correctAnswer("Constant time search on average")
                            .explanation("Hash Tables offer average O(1) time complexity for search, insert, and delete operations.")
                            .bank(bank)
                            .build();
                    questionRepository.save(q4);

                    System.out.println("Seeded question bank and questions for CS301");
                }
            }

            // Force update student Arjun Sharma to Semester 3 so they see CS301-CS303 exams
            userRepository.findByEmail("arjun.sharma@secureiq.edu").ifPresent(studentUser -> {
                studentRepository.findByUserId(studentUser.getId()).ifPresent(studentProfile -> {
                    if (studentProfile.getSemester() != 3) {
                        studentProfile.setSemester(3);
                        studentRepository.save(studentProfile);
                        System.out.println("Updated student Arjun Sharma to Semester 3 to match subjects");
                    }

                    // Force associate student with sessions
                    examSessionRepository.findBySessionCode("CS301-MID-SEM-SESSION").ifPresent(session -> {
                        if (session.getStudents() == null) {
                            session.setStudents(new HashSet<>());
                        }
                        System.out.println("CS301-MID-SEM-SESSION has students count: " + session.getStudents().size());
                        session.getStudents().forEach(s -> System.out.println("  - Mapped student ID: " + s.getId() + ", email: " + s.getUser().getEmail() + ", sem: " + s.getSemester()));
                        if (session.getStudents().stream().noneMatch(s -> s.getId().equals(studentProfile.getId()))) {
                            session.getStudents().add(studentProfile);
                            session.setTotalStudents(session.getStudents().size());
                            examSessionRepository.save(session);
                            System.out.println("Force-mapped Arjun Sharma to CS301-MID-SEM-SESSION");
                        }
                    });

                    examSessionRepository.findBySessionCode("CS303-QUIZ-SESSION").ifPresent(session -> {
                        if (session.getStudents() == null) {
                            session.setStudents(new HashSet<>());
                        }
                        System.out.println("CS303-QUIZ-SESSION has students count: " + session.getStudents().size());
                        session.getStudents().forEach(s -> System.out.println("  - Mapped student ID: " + s.getId() + ", email: " + s.getUser().getEmail() + ", sem: " + s.getSemester()));
                        if (session.getStudents().stream().noneMatch(s -> s.getId().equals(studentProfile.getId()))) {
                            session.getStudents().add(studentProfile);
                            session.setTotalStudents(session.getStudents().size());
                            examSessionRepository.save(session);
                            System.out.println("Force-mapped Arjun Sharma to CS303-QUIZ-SESSION");
                        }
                    });
                });
            });

            return;
        }

        System.out.println("Seeding database with default demo data...");

        // 1. Create Departments
        Department cseDept = Department.builder()
                .departmentName("Computer Science")
                .departmentCode("CS")
                .description("Department of Computer Science and Engineering")
                .build();
        cseDept = departmentRepository.save(cseDept);

        Department eceDept = Department.builder()
                .departmentName("Electronics & Communication")
                .departmentCode("EC")
                .description("Department of Electronics and Communication Engineering")
                .build();
        departmentRepository.save(eceDept);

        Department meDept = Department.builder()
                .departmentName("Mechanical Engineering")
                .departmentCode("ME")
                .description("Department of Mechanical Engineering")
                .build();
        departmentRepository.save(meDept);

        Department ceDept = Department.builder()
                .departmentName("Civil Engineering")
                .departmentCode("CE")
                .description("Department of Civil Engineering")
                .build();
        departmentRepository.save(ceDept);

        // 2. Create Users
        User adminUser = User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@secureiq.edu")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .status(Status.ACTIVE)
                .build();
        userRepository.save(adminUser);

        User hodUser = User.builder()
                .firstName("Suresh")
                .lastName("Babu")
                .email("suresh.babu@secureiq.edu")
                .password(passwordEncoder.encode("hod12345"))
                .role(Role.HOD)
                .status(Status.ACTIVE)
                .build();
        hodUser = userRepository.save(hodUser);

        // Link HOD to Computer Science department
        cseDept.setHod(hodUser);
        departmentRepository.save(cseDept);

        User facultyUser = User.builder()
                .firstName("Meera")
                .lastName("Krishnan")
                .email("meera.krishnan@secureiq.edu")
                .password(passwordEncoder.encode("faculty123"))
                .role(Role.FACULTY)
                .status(Status.ACTIVE)
                .build();
        facultyUser = userRepository.save(facultyUser);

        User studentUser = User.builder()
                .firstName("Arjun")
                .lastName("Sharma")
                .email("arjun.sharma@secureiq.edu")
                .password(passwordEncoder.encode("student123"))
                .role(Role.STUDENT)
                .status(Status.ACTIVE)
                .build();
        studentUser = userRepository.save(studentUser);

        // 3. Create Faculty Profile
        Faculty facultyProfile = Faculty.builder()
                .user(facultyUser)
                .employeeId("EMP001")
                .designation("Associate Professor")
                .qualification("Ph.D")
                .specialization("Computer Science")
                .yearsOfExperience(10)
                .officeLocation("Block A, Room 302")
                .department(cseDept)
                .joiningDate(LocalDate.of(2018, 7, 1))
                .profileCompleted(true)
                .build();
        facultyProfile = facultyRepository.save(facultyProfile);

        // 4. Create and Link Subjects
        Subject dsSubject = Subject.builder()
                .subjectCode("CS301")
                .subjectName("Data Structures")
                .credits(4)
                .semester(3)
                .department(cseDept)
                .faculty(Set.of(facultyUser))
                .build();
        dsSubject = subjectRepository.save(dsSubject);

        Subject algoSubject = Subject.builder()
                .subjectCode("CS302")
                .subjectName("Algorithms")
                .credits(4)
                .semester(3)
                .department(cseDept)
                .faculty(Set.of(facultyUser))
                .build();
        algoSubject = subjectRepository.save(algoSubject);

        Subject dbSubject = Subject.builder()
                .subjectCode("CS303")
                .subjectName("Database Systems")
                .credits(4)
                .semester(3)
                .department(cseDept)
                .faculty(Set.of(facultyUser))
                .build();
        dbSubject = subjectRepository.save(dbSubject);

        facultyProfile.setSubjects(new HashSet<>(Arrays.asList(dsSubject, algoSubject, dbSubject)));
        facultyRepository.save(facultyProfile);

        // 5. Create Student Profile
        Student studentProfile = Student.builder()
                .user(studentUser)
                .registerNumber("CS2021001")
                .rollNumber("CS2021001")
                .department(cseDept)
                .academicYear("2021-2025")
                .semester(3)
                .section("A")
                .batch("2021-2025")
                .dateOfBirth(LocalDate.of(2003, 5, 15))
                .gender("Male")
                .profileCompleted(true)
                .build();
        studentRepository.save(studentProfile);

        // 6. Create Default Exams
        Exam dsExam = Exam.builder()
                .examCode("CS301-MID-SEM")
                .examTitle("Data Structures Mid-Semester")
                .description("Mid-semester examination for Data Structures and Algorithms course.")
                .subject(dsSubject)
                .faculty(facultyProfile)
                .department(cseDept)
                .semester(3)
                .examType(ExamType.MID_SEMESTER)
                .totalMarks(100)
                .durationMinutes(120)
                .passingMarks(40)
                .scheduledDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(9, 30))
                .endTime(LocalTime.of(11, 30))
                .venue("Seminar Hall 1")
                .instructions("1. No electronic items allowed.\n2. Bring your hall ticket.")
                .status(ExamStatus.PUBLISHED)
                .build();
        examRepository.save(dsExam);

        Exam dbExam = Exam.builder()
                .examCode("CS303-QUIZ")
                .examTitle("Database Systems Quiz")
                .description("In-class quiz for Database Management Systems.")
                .subject(dbSubject)
                .faculty(facultyProfile)
                .department(cseDept)
                .semester(3)
                .examType(ExamType.QUIZ)
                .totalMarks(50)
                .durationMinutes(60)
                .passingMarks(20)
                .scheduledDate(LocalDate.now().plusDays(5))
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 0))
                .venue("Lab 3 - Block B")
                .instructions("1. Closed book quiz.\n2. System login credentials will be provided.")
                .status(ExamStatus.PUBLISHED)
                .build();
        examRepository.save(dbExam);

        if (examSessionRepository.count() == 0) {
            Set<Student> studentsSet = new HashSet<>();
            if (studentProfile != null) {
                studentsSet.add(studentProfile);
            }

            ExamSession dsSession = ExamSession.builder()
                    .sessionCode("CS301-MID-SEM-SESSION")
                    .sessionName("Data Structures Mid-Semester Exam Session")
                    .exam(dsExam)
                    .faculty(facultyProfile)
                    .startDateTime(LocalDateTime.of(dsExam.getScheduledDate(), dsExam.getStartTime()))
                    .endDateTime(LocalDateTime.of(dsExam.getScheduledDate(), dsExam.getEndTime()))
                    .venue(dsExam.getVenue())
                    .status(SessionStatus.LIVE)
                    .instructions("Read all instructions carefully.")
                    .students(studentsSet)
                    .build();
            examSessionRepository.save(dsSession);

            ExamSession dbSession = ExamSession.builder()
                    .sessionCode("CS303-QUIZ-SESSION")
                    .sessionName("Database Systems Quiz Session")
                    .exam(dbExam)
                    .faculty(facultyProfile)
                    .startDateTime(LocalDateTime.of(dbExam.getScheduledDate(), dbExam.getStartTime()))
                    .endDateTime(LocalDateTime.of(dbExam.getScheduledDate(), dbExam.getEndTime()))
                    .venue(dbExam.getVenue())
                    .status(SessionStatus.SCHEDULED)
                    .instructions("Closed book quiz.")
                    .students(studentsSet)
                    .build();
            examSessionRepository.save(dbSession);
            System.out.println("Seeded live exam sessions for fresh database");
        }

        System.out.println("Database seeding completed successfully!");
    }
}
