package com.project.reflash.backend.entity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "courses")
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="name", nullable = false)
    String courseName;

    @Column(name="grade", nullable=false)
    String grade;

    @Column(name="academic_year", nullable=false)
    String academicYear;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "course_teacher",
            inverseJoinColumns = {
                    @JoinColumn(name = "teacher_id", referencedColumnName = "id"),
            },

            joinColumns = {
                    @JoinColumn(name = "course_id", referencedColumnName = "id"),
            }
    )
    List<Teacher> teachers;


    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "course_student",
            inverseJoinColumns = {
                    @JoinColumn(name = "student_id", referencedColumnName = "id"),
            },

            joinColumns = {
                    @JoinColumn(name = "course_id", referencedColumnName = "id"),
            }
    )
    List<Student> students;

    @OneToMany(mappedBy="course", cascade= CascadeType.ALL, orphanRemoval = true)
    List<Deck> decks;
}
