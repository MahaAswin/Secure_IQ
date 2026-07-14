package com.secureiq.SecureIQ.faculty.model;

import com.secureiq.SecureIQ.common.entity.BaseEntity;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.subject.model.Subject;
import com.secureiq.SecureIQ.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "faculties")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE faculties SET deleted = true, employee_id = concat(employee_id, '_deleted_', cast(extract(epoch from now()) as varchar)) WHERE id = ?")
@SQLRestriction("deleted = false")
public class Faculty extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "employee_id", nullable = false, unique = true)
    private String employeeId;

    @Column(nullable = false)
    private String designation;

    @Column(nullable = false)
    private String qualification;

    @Column(nullable = false)
    private String specialization;

    @Column(name = "years_of_experience", nullable = false)
    private Integer yearsOfExperience;

    @Column(name = "office_location")
    private String officeLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(name = "profile_completed", nullable = false)
    @Builder.Default
    private boolean profileCompleted = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "faculty_subjects",
        joinColumns = @JoinColumn(name = "faculty_id"),
        inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    @Builder.Default
    private Set<Subject> subjects = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;
}
