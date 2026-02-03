package org.example.service;

import jakarta.servlet.http.HttpServletRequest;
import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.dto.UserUpdateRequest;
import org.example.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    void registerUser(String username, String email, String password);

    LoginResponse authenticateUser(LoginRequest request, HttpServletRequest httpRequest);

    List<User> getAllUsers(String currentUsername);

    User getUserById(Long id, String currentUsername);

    User updateUser(Long id, UserUpdateRequest request, String currentUsername);

    void deleteUser(Long id, String currentUsername);

    boolean isAdminOrOwner(Long userId, String currentUsername);

    Optional<User> findByUsername(String username);

    String generateAuthToken(User user);
}
