package com.secureiq.SecureIQ.questionbank.mapper;

import com.secureiq.SecureIQ.department.mapper.DepartmentMapper;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.questionbank.dto.QuestionBankCreateRequest;
import com.secureiq.SecureIQ.questionbank.dto.QuestionBankPatchRequest;
import com.secureiq.SecureIQ.questionbank.dto.QuestionBankResponse;
import com.secureiq.SecureIQ.questionbank.dto.QuestionBankUpdateRequest;
import com.secureiq.SecureIQ.questionbank.model.QuestionBank;
import com.secureiq.SecureIQ.subject.mapper.SubjectMapper;
import com.secureiq.SecureIQ.subject.model.Subject;
import com.secureiq.SecureIQ.user.mapper.UserMapper;
import com.secureiq.SecureIQ.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class QuestionBankMapper {

    private final SubjectMapper subjectMapper;
    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;

    public QuestionBankMapper(SubjectMapper subjectMapper, DepartmentMapper departmentMapper, UserMapper userMapper) {
        this.subjectMapper = subjectMapper;
        this.departmentMapper = departmentMapper;
        this.userMapper = userMapper;
    }

    public QuestionBankResponse toResponse(QuestionBank bank) {
        if (bank == null) {
            return null;
        }
        return QuestionBankResponse.builder()
                .id(bank.getId())
                .bankName(bank.getBankName())
                .description(bank.getDescription())
                .subject(subjectMapper.toResponse(bank.getSubject()))
                .department(departmentMapper.toResponse(bank.getDepartment(), 0, 0))
                .createdBy(userMapper.toResponse(bank.getCreatedBy()))
                .createdAt(bank.getCreatedAt() != null ? bank.getCreatedAt().toString() : null)
                .updatedAt(bank.getUpdatedAt() != null ? bank.getUpdatedAt().toString() : null)
                .build();
    }

    public QuestionBank toEntity(QuestionBankCreateRequest request, Subject subject, Department department, User creator) {
        if (request == null) {
            return null;
        }
        return QuestionBank.builder()
                .bankName(request.getBankName())
                .description(request.getDescription())
                .subject(subject)
                .department(department)
                .createdBy(creator)
                .deleted(false)
                .build();
    }

    public void updateEntity(QuestionBankUpdateRequest request, Subject subject, Department department, QuestionBank bank) {
        if (request == null || bank == null) {
            return;
        }
        bank.setBankName(request.getBankName());
        bank.setDescription(request.getDescription());
        if (subject != null) {
            bank.setSubject(subject);
        }
        if (department != null) {
            bank.setDepartment(department);
        }
    }

    public void patchEntity(QuestionBankPatchRequest request, Subject subject, Department department, QuestionBank bank) {
        if (request == null || bank == null) {
            return;
        }
        if (request.getBankName() != null) {
            bank.setBankName(request.getBankName());
        }
        if (request.getDescription() != null) {
            bank.setDescription(request.getDescription());
        }
        if (subject != null) {
            bank.setSubject(subject);
        }
        if (department != null) {
            bank.setDepartment(department);
        }
    }
}
