package com.secureiq.SecureIQ.questionbank.service;

import com.secureiq.SecureIQ.common.exception.BadRequestException;
import com.secureiq.SecureIQ.common.exception.ConflictException;
import com.secureiq.SecureIQ.common.exception.NotFoundException;
import com.secureiq.SecureIQ.common.exception.UnauthorizedException;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.department.repository.DepartmentRepository;
import com.secureiq.SecureIQ.questionbank.dto.QuestionBankCreateRequest;
import com.secureiq.SecureIQ.questionbank.dto.QuestionBankPatchRequest;
import com.secureiq.SecureIQ.questionbank.dto.QuestionBankResponse;
import com.secureiq.SecureIQ.questionbank.dto.QuestionBankUpdateRequest;
import com.secureiq.SecureIQ.questionbank.mapper.QuestionBankMapper;
import com.secureiq.SecureIQ.questionbank.model.QuestionBank;
import com.secureiq.SecureIQ.questionbank.repository.QuestionBankRepository;
import com.secureiq.SecureIQ.subject.model.Subject;
import com.secureiq.SecureIQ.subject.repository.SubjectRepository;
import com.secureiq.SecureIQ.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class QuestionBankServiceImpl implements QuestionBankService {

    private final QuestionBankRepository questionBankRepository;
    private final SubjectRepository subjectRepository;
    private final DepartmentRepository departmentRepository;
    private final QuestionBankMapper questionBankMapper;

    public QuestionBankServiceImpl(QuestionBankRepository questionBankRepository,
                                   SubjectRepository subjectRepository,
                                   DepartmentRepository departmentRepository,
                                   QuestionBankMapper questionBankMapper) {
        this.questionBankRepository = questionBankRepository;
        this.subjectRepository = subjectRepository;
        this.departmentRepository = departmentRepository;
        this.questionBankMapper = questionBankMapper;
    }

    @Override
    public Page<QuestionBankResponse> getAll(String bankName, Long subjectId, Long departmentId, Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        if (isStudent) {
            throw new AccessDeniedException("Access denied: Students do not have access to question banks");
        }

        if (isAdmin) {
            Page<QuestionBank> banks = questionBankRepository.findAllFiltered(bankName, subjectId, departmentId, pageable);
            return banks.map(questionBankMapper::toResponse);
        }

        User principal = (User) auth.getPrincipal();

        if (isHod) {
            Department dept = departmentRepository.findByHodId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied: HOD is not assigned to any department"));
            
            if (departmentId != null && !departmentId.equals(dept.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only query question banks in their own department");
            }
            Page<QuestionBank> banks = questionBankRepository.findAllFiltered(bankName, subjectId, dept.getId(), pageable);
            return banks.map(questionBankMapper::toResponse);
        }

        if (isFaculty) {
            Page<QuestionBank> banks = questionBankRepository.findAllFilteredForFaculty(principal.getId(), bankName, subjectId, departmentId, pageable);
            return banks.map(questionBankMapper::toResponse);
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }

    @Override
    public QuestionBankResponse getById(Long id) {
        QuestionBank bank = getEntityById(id);
        checkReadAccess(bank);
        return questionBankMapper.toResponse(bank);
    }

    @Override
    @Transactional
    public QuestionBankResponse create(QuestionBankCreateRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        User principal = (User) auth.getPrincipal();

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new NotFoundException("Subject not found with id: " + request.getSubjectId()));
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + request.getDepartmentId()));

        if (!subject.getDepartment().getId().equals(department.getId())) {
            throw new BadRequestException("Subject does not belong to the specified department");
        }

        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        if (isStudent) {
            throw new AccessDeniedException("Access denied: Students cannot create question banks");
        }

        if (isHod) {
            if (department.getHod() == null || !department.getHod().getId().equals(principal.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only create question banks in their own department");
            }
        }

        if (isFaculty) {
            boolean isAssigned = subject.getFaculty().stream().anyMatch(f -> f.getId().equals(principal.getId()));
            if (!isAssigned) {
                throw new AccessDeniedException("Access denied: Faculty can only create question banks for assigned subjects");
            }
        }

        if (questionBankRepository.existsByBankNameAndSubjectId(request.getBankName(), request.getSubjectId())) {
            throw new ConflictException("Question Bank name must be unique within a subject");
        }

        QuestionBank bank = questionBankMapper.toEntity(request, subject, department, principal);
        QuestionBank savedBank = questionBankRepository.save(bank);
        return questionBankMapper.toResponse(savedBank);
    }

    @Override
    @Transactional
    public QuestionBankResponse update(Long id, QuestionBankUpdateRequest request) {
        QuestionBank bank = getEntityById(id);
        checkWriteAccess(bank);

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new NotFoundException("Subject not found with id: " + request.getSubjectId()));
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + request.getDepartmentId()));

        if (!subject.getDepartment().getId().equals(department.getId())) {
            throw new BadRequestException("Subject does not belong to the specified department");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) auth.getPrincipal();
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));

        if (isHod) {
            if (department.getHod() == null || !department.getHod().getId().equals(principal.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only update/move question banks to their own department");
            }
        }

        if (isFaculty) {
            boolean isAssigned = subject.getFaculty().stream().anyMatch(f -> f.getId().equals(principal.getId()));
            if (!isAssigned) {
                throw new AccessDeniedException("Access denied: Faculty can only update question banks for assigned subjects");
            }
        }

        if (questionBankRepository.existsByBankNameAndSubjectIdAndIdNot(request.getBankName(), request.getSubjectId(), id)) {
            throw new ConflictException("Question Bank name must be unique within a subject");
        }

        questionBankMapper.updateEntity(request, subject, department, bank);
        QuestionBank savedBank = questionBankRepository.save(bank);
        return questionBankMapper.toResponse(savedBank);
    }

    @Override
    @Transactional
    public QuestionBankResponse patch(Long id, QuestionBankPatchRequest request) {
        QuestionBank bank = getEntityById(id);
        checkWriteAccess(bank);

        Subject subject = bank.getSubject();
        if (request.getSubjectId() != null) {
            subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new NotFoundException("Subject not found with id: " + request.getSubjectId()));
        }

        Department department = bank.getDepartment();
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new NotFoundException("Department not found with id: " + request.getDepartmentId()));
        }

        if (!subject.getDepartment().getId().equals(department.getId())) {
            throw new BadRequestException("Subject does not belong to the specified department");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) auth.getPrincipal();
        boolean isHod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isFaculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));

        if (isHod) {
            if (department.getHod() == null || !department.getHod().getId().equals(principal.getId())) {
                throw new AccessDeniedException("Access denied: HOD can only update/move question banks to their own department");
            }
        }

        if (isFaculty) {
            boolean isAssigned = subject.getFaculty().stream().anyMatch(f -> f.getId().equals(principal.getId()));
            if (!isAssigned) {
                throw new AccessDeniedException("Access denied: Faculty can only update question banks for assigned subjects");
            }
        }

        String targetBankName = request.getBankName() != null ? request.getBankName() : bank.getBankName();
        Long targetSubjectId = request.getSubjectId() != null ? request.getSubjectId() : bank.getSubject().getId();
        if (questionBankRepository.existsByBankNameAndSubjectIdAndIdNot(targetBankName, targetSubjectId, id)) {
            throw new ConflictException("Question Bank name must be unique within a subject");
        }

        questionBankMapper.patchEntity(request, subject, department, bank);
        QuestionBank savedBank = questionBankRepository.save(bank);
        return questionBankMapper.toResponse(savedBank);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        QuestionBank bank = getEntityById(id);
        checkWriteAccess(bank);
        questionBankRepository.delete(bank);
    }

    private QuestionBank getEntityById(Long id) {
        return questionBankRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Question Bank not found with id: " + id));
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
                throw new AccessDeniedException("Access denied: HOD can only manage question banks in their own department");
            }
            return;
        }

        if (isFaculty) {
            boolean isAssigned = bank.getSubject().getFaculty().stream().anyMatch(f -> f.getId().equals(principal.getId()));
            if (!isAssigned) {
                throw new AccessDeniedException("Access denied: Faculty can only manage question banks for assigned subjects");
            }
            return;
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }

    private void checkReadAccess(QuestionBank bank) {
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
                throw new AccessDeniedException("Access denied: HOD can only view question banks in their own department");
            }
            return;
        }

        if (isFaculty) {
            boolean isAssigned = bank.getSubject().getFaculty().stream().anyMatch(f -> f.getId().equals(principal.getId()));
            if (!isAssigned) {
                throw new AccessDeniedException("Access denied: Faculty can only view question banks for assigned subjects");
            }
            return;
        }

        throw new AccessDeniedException("Access denied: Unauthorized role");
    }
}
