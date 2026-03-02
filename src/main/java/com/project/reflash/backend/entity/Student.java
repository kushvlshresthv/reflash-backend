package com.project.reflash.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "students")
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "firstname", nullable=false)
    String firstName;

    @Column(name = "lastname", nullable=false)
    String lastName;

    @Column(name = "password", nullable=false)
    String password;

    @Column(name = "grade", nullable = false)
    private String grade;

    @Column(name = "section", nullable = false)
    private String section = "NONE";

    @Column(name = "roll", nullable = false)
    private String roll;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    @ManyToMany(mappedBy = "students", fetch = FetchType.LAZY)
    private List<Course> courses;
}
