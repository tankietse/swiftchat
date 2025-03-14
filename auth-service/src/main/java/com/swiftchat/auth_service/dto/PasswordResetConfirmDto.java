package com.swiftchat.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for confirming a password reset.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Password reset confirmation")
public class PasswordResetConfirmDto {

    @NotBlank(message = "Reset token is required")
    @Schema(description = "Password reset token from email", example = "abcdef123456")
    private String resetToken;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Schema(description = "New password", example = "S3cur3P@ssw0rd")
    private String newPassword;
}
