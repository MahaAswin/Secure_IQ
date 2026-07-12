package com.secureiq.SecureIQ.subject.model;

import com.secureiq.SecureIQ.common.entity.BaseEntity;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "subjects")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE subjects SET deleted = true, subject_code = concat(subject_code, '_deleted_', cast(extract(epoch from now()) as varchar)) WHERE id = ?")
@SQLRestriction("deleted = false")
public class Subject extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_code", nullable = false, unique = true)
    private String subjectCode;

    @Column(name = "subject_name", nullable = false)
    private String subjectName;

    private String description;

    @Column(nullable = false)
    private Integer credits;

    @Column(nullable = false)
    private Integer semester;

    private String regulation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "subject_faculty",
        joinColumns = @JoinColumn(name = "subject_id"),
        inverseJoinColumns = @JoinColumn(name = "faculty_id")
    )
    @Builder.Default
    private Set<User> faculty = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;
}
