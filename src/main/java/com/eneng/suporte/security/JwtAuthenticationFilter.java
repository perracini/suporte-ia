package com.eneng.suporte.security;

import com.eneng.suporte.domain.model.User;
import com.eneng.suporte.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserLookup userLookup;

    public JwtAuthenticationFilter(JwtService jwtService, UserLookup userLookup) {
        this.jwtService = jwtService;
        this.userLookup = userLookup;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                JwtService.ParsedToken parsed = jwtService.parse(token);
                User user = userLookup.byUsername(parsed.username());
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        user, null, AuthenticatedUser.authoritiesOf(user));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("Token invalido: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    @Component
    public static class UserLookup {
        private final UserRepository userRepository;

        public UserLookup(UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        @Cacheable(cacheNames = "users-by-username", key = "#username")
        public User byUsername(String username) {
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("Usuario do token nao existe: " + username));
        }
    }
}
