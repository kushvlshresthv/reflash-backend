package com.project.reflash.backend.repository;

import com.project.reflash.backend.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Integer> {
    @Query("SELECT u from AppUser u WHERE u.grade = :grade AND u.section = :section AND u.roll = :roll")
    public Optional<AppUser> findUserByGradeSectionAndRoll(String grade, String section, String roll);
}
