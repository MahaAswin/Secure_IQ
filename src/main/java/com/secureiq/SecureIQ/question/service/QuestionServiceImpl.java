package com.secureiq.SecureIQ.question.service;

import com.secureiq.SecureIQ.common.exception.BadRequestException;
import com.secureiq.SecureIQ.common.exception.NotFoundException;
import com.secureiq.SecureIQ.common.exception.UnauthorizedException;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
import com.secureiq.SecureIQ.question.dto.*;
import com.secureiq.SecureIQ.question.mapper.QuestionMapper;
import com.secureiq.SecureIQ.question.model.Difficulty;
import com.secureiq.SecureIQ.question.model.Question;
import com.secureiq.SecureIQ.question.model.QuestionType;
import com.secureiq.SecureIQ.question.repository.QuestionRepository;
import com.secureiq.SecureIQ.questionbank.model.QuestionBank;
import com.secureiq.SecureIQ.questionbank.repository.QuestionBankRepository;
import com.secureiq.SecureIQ.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final DepartmentRepository departmentRepository;
    private final QuestionMapper questionMapper;

    public QuestionServiceImpl(QuestionRepository questionRepository,
                               QuestionBankRepository questionBankRepository,
                               DepartmentRepository departmentRepository,
                               QuestionMapper questionMapper) {
        this.questionRepository = questionRepository;
        this.questionBankRepository = questionBankRepository;
        this.departmentRepository = departmentRepository;
        this.questionMapper = questionMapper;
    }

    @Override
    public Page<QuestionResponse> getAll(Long bankId, Long subjectId, Long departmentId, Difficulty difficulty, QuestionType questionType, String keyword, Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        if (isStudent) {
            throw new AccessDeniedException("Access denied: Students do not have access to questions");
        }

        if (isAdmin) {
            Page<Question> questions = questionRepository.findAllFiltered(bankId, subjectId, departmentId, difficulty, questionType, keyword, pageable);
            return questions.map(questionMapper::toResponse);
        }

        User principal = (User) auth.getPrincipal();

        if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            if (departmentId != null && !departmentId.equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only query questions in their own department");
            }
            Page<Question> questions = questionRepository.findAllFiltered(bankId, subjectId, dept.getId(), difficulty, questionType, keyword, pageable);
            return questions.map(questionMapper::toResponse);
        }

        if (isFaculty) {
            Page<Question> questions = questionRepository.findAllFilteredForFaculty(principal.getId(), bankId, subjectId, departmentId, difficulty, questionType, keyword, pageable);
            return questions.map(questionMapper::toResponse);
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }

    @Override
    public QuestionResponse getById(Long id) {
        Question question = getEntityById(id);
        checkReadAccess(question);
        return questionMapper.toResponse(question);
    }

    @Override
    @Transactional
    public QuestionResponse create(QuestionCreateRequest request) {
        QuestionBank bank = questionBankRepository.findById(request.getBankId())
                .orElseThrow(() -> new NotFoundException("Question Bank not found with id: " + request.getBankId()));

        checkWriteAccess(bank);

        if (request.getQuestionType() == QuestionType.MCQ) {
            if (request.getOptions() == null || request.getOptions().size() < 2) {
                throw new BadRequestException("MCQ questions must have at least two options");
            }
        }

        Question question = questionMapper.toEntity(request, bank);
        Question savedQuestion = questionRepository.save(question);
        return questionMapper.toResponse(savedQuestion);
    }

    @Override
    @Transactional
    public QuestionResponse update(Long id, QuestionUpdateRequest request) {
        Question question = getEntityById(id);
        
        // Check write access on existing bank
        checkWriteAccess(question.getBank());

        QuestionBank targetBank = questionBankRepository.findById(request.getBankId())
                .orElseThrow(() -> new NotFoundException("Question Bank not found with id: " + request.getBankId()));

        // Check write access on target bank as well if changed
        if (!targetBank.getId().equals(question.getBank().getId())) {
            checkWriteAccess(targetBank);
        }

        if (request.getQuestionType() == QuestionType.MCQ) {
            if (request.getOptions() == null || request.getOptions().size() < 2) {
                throw new BadRequestException("MCQ questions must have at least two options");
            }
        }

        questionMapper.updateEntity(request, targetBank, question);
        Question savedQuestion = questionRepository.save(question);
        return questionMapper.toResponse(savedQuestion);
    }

    @Override
    @Transactional
    public QuestionResponse patch(Long id, QuestionPatchRequest request) {
        Question question = getEntityById(id);
        
        checkWriteAccess(question.getBank());

        QuestionBank targetBank = null;
        if (request.getBankId() != null) {
            targetBank = questionBankRepository.findById(request.getBankId())
                    .orElseThrow(() -> new NotFoundException("Question Bank not found with id: " + request.getBankId()));
            if (!targetBank.getId().equals(question.getBank().getId())) {
                checkWriteAccess(targetBank);
            }
        }

        QuestionType resolvedType = request.getQuestionType() != null ? request.getQuestionType() : question.getQuestionType();
        List<String> resolvedOptions = request.getOptions() != null ? request.getOptions() : question.getOptions();
        if (resolvedType == QuestionType.MCQ) {
            if (resolvedOptions == null || resolvedOptions.size() < 2) {
                throw new BadRequestException("MCQ questions must have at least two options");
            }
        }

        if (request.getMarks() != null && request.getMarks() <= 0) {
            throw new BadRequestException("Marks must be a positive number");
        }

        questionMapper.patchEntity(request, targetBank, question);
        Question savedQuestion = questionRepository.save(question);
        return questionMapper.toResponse(savedQuestion);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Question question = getEntityById(id);
        checkWriteAccess(question.getBank());
        questionRepository.delete(question);
    }

    @Override
    @Transactional
    public BulkImportResponse bulkImport(MultipartFile file, Long bankId) {
        QuestionBank bank = questionBankRepository.findById(bankId)
                .orElseThrow(() -> new NotFoundException("Question Bank not found with id: " + bankId));

        checkWriteAccess(bank);

        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();
        List<Question> toSave = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1) {
                    // Skip header line
                    continue;
                }
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                for (int i = 0; i < tokens.length; i++) {
                    String t = tokens[i].trim();
                    if (t.startsWith("\"") && t.endsWith("\"")) {
                        t = t.substring(1, t.length() - 1);
                    }
                    tokens[i] = t;
                }

                try {
                    if (tokens.length < 5) {
                        throw new BadRequestException("Insufficient fields. Expected at least: questionText, questionType, difficulty, marks, correctAnswer");
                    }

                    String questionText = tokens[0];
                    if (questionText.isEmpty()) {
                        throw new BadRequestException("Question text is required");
                    }

                    QuestionType questionType = QuestionType.fromString(tokens[1]);
                    if (questionType == null) {
                        throw new BadRequestException("Invalid question type: " + tokens[1]);
                    }

                    Difficulty difficulty = Difficulty.fromString(tokens[2]);
                    if (difficulty == null) {
                        throw new BadRequestException("Invalid difficulty: " + tokens[2]);
                    }

                    int marks;
                    try {
                        marks = Integer.parseInt(tokens[3]);
                    } catch (NumberFormatException e) {
                        throw new BadRequestException("Marks must be an integer: " + tokens[3]);
                    }
                    if (marks <= 0) {
                        throw new BadRequestException("Marks must be a positive number");
                    }

                    List<String> optionsList = new ArrayList<>();
                    if (tokens.length > 4 && !tokens[4].isEmpty()) {
                        String[] opts = tokens[4].split(";");
                        for (String o : opts) {
                            optionsList.add(o.trim());
                        }
                    }

                    if (questionType == QuestionType.MCQ && optionsList.size() < 2) {
                        throw new BadRequestException("MCQ questions must have at least two options");
                    }

                    String correctAnswer = tokens.length > 5 ? tokens[5] : "";
                    if (correctAnswer.isEmpty()) {
                        throw new BadRequestException("Correct answer is required");
                    }

                    String explanation = tokens.length > 6 ? tokens[6] : "";
                    String imageUrl = tokens.length > 7 ? tokens[7] : "";

                    Question question = Question.builder()
                            .questionText(questionText)
                            .questionType(questionType)
                            .difficulty(difficulty)
                            .marks(marks)
                            .options(optionsList)
                            .correctAnswer(correctAnswer)
                            .explanation(explanation)
                            .imageUrl(imageUrl)
                            .bank(bank)
                            .deleted(false)
                            .build();

                    toSave.add(question);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                }
            }

            if (!toSave.isEmpty()) {
                questionRepository.saveAll(toSave);
            }
        } catch (Exception e) {
            errors.add("Error reading CSV file: " + e.getMessage());
            failureCount = 1;
        }

        return BulkImportResponse.builder()
                .successCount(successCount)
                .failureCount(failureCount)
                .errors(errors)
                .build();
    }

    @Override
    public QuestionDashboardResponse getDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        if (isStudent) {
            throw new AccessDeniedException("Access denied: Students do not have dashboard access");
        }

        User principal = (User) auth.getPrincipal();

        long totalBanks = 0;
        long totalQuestions = 0;
        List<Object[]> subjectData = new ArrayList<>();
        List<Object[]> difficultyData = new ArrayList<>();

        if (isAdmin) {
            totalBanks = questionBankRepository.count();
            totalQuestions = questionRepository.countAllQuestions();
            subjectData = questionRepository.countQuestionsBySubject();
            difficultyData = questionRepository.countQuestionsByDifficulty();
        } else if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            totalBanks = questionBankRepository.findAllByDepartmentId(dept.getId()).size();
            totalQuestions = questionRepository.countQuestionsByDepartmentId(dept.getId());
            subjectData = questionRepository.countQuestionsBySubjectForDepartment(dept.getId());
            difficultyData = questionRepository.countQuestionsByDifficultyForDepartment(dept.getId());
        } else if (isFaculty) {
            totalBanks = questionBankRepository.findAllByFacultyId(principal.getId()).size();
            totalQuestions = questionRepository.countQuestionsByFacultyId(principal.getId());
            subjectData = questionRepository.countQuestionsBySubjectForFaculty(principal.getId());
            difficultyData = questionRepository.countQuestionsByDifficultyForFaculty(principal.getId());
        }

        Map<String, Long> questionsBySubject = new HashMap<>();
        for (Object[] row : subjectData) {
            String subjectCode = (String) row[0];
            Long count = (Long) row[1];
            questionsBySubject.put(subjectCode, count);
        }

        Map<String, Long> questionsByDifficulty = new HashMap<>();
        for (Object[] row : difficultyData) {
            Difficulty diff = (Difficulty) row[0];
            Long count = (Long) row[1];
            questionsByDifficulty.put(diff.name(), count);
        }

        return QuestionDashboardResponse.builder()
                .totalQuestionBanks(totalBanks)
                .totalQuestions(totalQuestions)
                .questionsBySubject(questionsBySubject)
                .questionsByDifficulty(questionsByDifficulty)
                .build();
    }

    private Question getEntityById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Question not found with id: " + id));
    }

    private void checkWriteAccess(QuestionBank bank) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));

        if (isAdmin) {
            return;
        }

        User principal = (User) auth.getPrincipal();

        if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            if (!bank.getDepartment().getId().equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only manage questions in their own department");
            }
            return;
        }

        if (isFaculty) {
            boolean isAssigned = bank.getSubject().getFaculty().stream().anyMatch(f -> f.getId().equals(principal.getId()));
            if (!isAssigned) {
                throw new AccessDeniedException("Access denied: Faculty can only manage questions for assigned subjects");
            }
            return;
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }

    private void checkReadAccess(Question question) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));

        if (isAdmin) {
            return;
        }

        User principal = (User) auth.getPrincipal();

        if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            if (!question.getBank().getDepartment().getId().equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only view questions in their own department");
            }
            return;
        }

        if (isFaculty) {
            boolean isAssigned = question.getBank().getSubject().getFaculty().stream().anyMatch(f -> f.getId().equals(principal.getId()));
            if (!isAssigned) {
                throw new AccessDeniedException("Access denied: Faculty can only view questions for assigned subjects");
            }
            return;
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }
}
