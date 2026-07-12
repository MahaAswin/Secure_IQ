package com.secureiq.SecureIQ.student.model;

import com.secureiq.SecureIQ.common.entity.BaseEntity;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Table(name = "students")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE students SET deleted = true, register_number = concat(register_number, '_deleted_', cast(extract(epoch from now()) as varchar)), roll_number = concat(roll_number, '_deleted_', cast(extract(epoch from now()) as varchar)) WHERE id = ?")
@SQLRestriction("deleted = false")
public class Student extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "register_number", nullable = false, unique = true)
    private String registerNumber;

    @Column(name = "roll_number", nullable = false, unique = true)
    private String rollNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    private Integer semester;

    private String section;

    private String batch;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String gender;

    private String address;

    @Column(name = "parent_name")
    private String parentName;

    @Column(name = "parent_phone")
    private String parentPhone;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @Column(name = "profile_completed", nullable = false)
    @Builder.Default
    private boolean profileCompleted = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;
}
