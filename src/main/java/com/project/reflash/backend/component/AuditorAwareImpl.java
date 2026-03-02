package com.project.reflash.backend.component;

import com.project.reflash.backend.entity.Student;
import com.project.reflash.backend.auth.user_details.StudentUserDetails;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditAwareImpl")
public class AuditorAwareImpl implements AuditorAware<Student> {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Student> getCurrentAuditor() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        StudentUserDetails studentUserDetails =
                (StudentUserDetails) authentication.getPrincipal();

        Integer userId = studentUserDetails.getId();

        // IMPORTANT: this does NOT hit the DB
        Student userRef = entityManager.getReference(Student.class, userId);

        return Optional.of(userRef);
    }
}
