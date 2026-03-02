package com.project.reflash.backend.repository;

import com.project.reflash.backend.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Integer> {

    @Query("SELECT t from Teacher t WHERE t.username = :username")
    Optional<Teacher> findByUsername(@Param("username") String username);
}
