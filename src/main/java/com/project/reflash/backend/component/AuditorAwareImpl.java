package com.project.reflash.backend.component;

import com.project.reflash.backend.entity.AppUser;
import com.project.reflash.backend.service.security.AppUserDetails;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditAwareImpl")
public class AuditorAwareImpl implements AuditorAware<AppUser> {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<AppUser> getCurrentAuditor() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        AppUserDetails appUserDetails =
                (AppUserDetails) authentication.getPrincipal();

        Integer userId = appUserDetails.getUserId();

        // IMPORTANT: this does NOT hit the DB
        AppUser userRef = entityManager.getReference(AppUser.class, userId);

        return Optional.of(userRef);
    }
}
