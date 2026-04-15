package com.eneng.suporte.service;

import com.eneng.suporte.domain.model.Role;
import com.eneng.suporte.domain.model.User;

public interface AuthService {

    record RegisterCommand(String username, String email, String password, Role role) {
    }

    record LoginResult(String token, long expiresInSeconds, User user) {
    }

    User registrar(RegisterCommand command);

    LoginResult autenticar(String username, String password);
}
