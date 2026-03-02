package com.project.reflash.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "teachers")
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Integer id;

    @Column(name="firstname", nullable=false)
    private String firstName;

    @Column(name="lastname", nullable = false)
    private String lastName;

    @Column(name="username", nullable=false, unique = true)
    private String username;

    @Column(name="password", nullable=false)
    private String password;


    @ManyToMany(mappedBy = "teachers", fetch = FetchType.LAZY)
    private List<Course> courses;
}
