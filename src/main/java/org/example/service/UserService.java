package org.example.service;

import org.example.dto.*;

import java.util.List;

public interface UserService {

    UserRegistrationResponse registerUser(UserRegistrationRequest request);

    LoginResponse authenticateUser(LoginRequest request);

    List<UserResponse> getAllUsers(String currentUsername);

    UserResponse getUserById(Long id, String currentUsername);

    UserResponse updateUser(Long id, UserUpdateRequest request, String currentUsername);

    void deleteUser(Long id, String currentUsername);

    boolean isAdminOrOwner(Long userId, String currentUsername);
}
