package com.swiftchat.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for requesting a password reset.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Password reset request")
public class PasswordResetRequestDto {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "User email address", example = "user@example.com")
    private String email;
}
