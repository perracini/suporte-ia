package com.eneng.suporte.security;

import com.eneng.suporte.domain.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;

public record AuthenticatedUser(User user) {

    public static User current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User u)) {
            throw new IllegalStateException("Usuario nao autenticado");
        }
        return u;
    }

    public static Collection<? extends GrantedAuthority> authoritiesOf(User user) {
        return List.of(() -> "ROLE_" + user.getRole().name());
    }
}
