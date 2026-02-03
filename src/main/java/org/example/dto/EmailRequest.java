package org.example.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Email Request DTO
 * Generic email request for notification service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String to;

    @NotBlank(message = "Email subject is required")
    private String subject;

    private String text;

    private String templateName;

    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();

    @Builder.Default
    private boolean html = false;
}
