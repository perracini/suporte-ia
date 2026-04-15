package com.eneng.suporte.service.impl;

import com.eneng.suporte.domain.exception.BusinessRuleException;
import com.eneng.suporte.domain.exception.ResourceNotFoundException;
import com.eneng.suporte.domain.model.User;
import com.eneng.suporte.repository.UserRepository;
import com.eneng.suporte.security.JwtService;
import com.eneng.suporte.service.AuthService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public User registrar(RegisterCommand command) {
        if (userRepository.existsByUsername(command.username())) {
            throw new BusinessRuleException("Username ja existe");
        }
        if (userRepository.existsByEmail(command.email())) {
            throw new BusinessRuleException("Email ja existe");
        }
        User user = User.builder()
                .username(command.username())
                .email(command.email())
                .passwordHash(passwordEncoder.encode(command.password()))
                .role(command.role())
                .build();
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResult autenticar(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciais invalidas");
        }
        JwtService.IssuedToken token = jwtService.issue(user);
        return new LoginResult(token.token(), token.expiresInSeconds(), user);
    }
}
