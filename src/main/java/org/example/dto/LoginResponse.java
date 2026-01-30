package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.entity.Role;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private Role role;

    // MFA fields
    private Boolean mfaRequired = false;
    private String tempToken;
    private Boolean mfaEnabled = false;

    // Account lock fields
    private Boolean accountLocked = false;
    private Map<String, Object> lockDetails;

    public LoginResponse(String token, Long id, String username, String email, Role role) {
        this.token = token;
        this.type = "Bearer";
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.mfaRequired = false;
        this.mfaEnabled = false;
        this.accountLocked = false;
    }
}
