package com.project.reflash.backend.repository;

import com.project.reflash.backend.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Integer> {
    public Optional<AppUser> findByUsername(String username);
    public boolean existsByUsername(String username);


    @Query("SELECT u.uid from AppUser u WHERE u.username = :username")
    public int getUserIdFromUsername(String username);
}
