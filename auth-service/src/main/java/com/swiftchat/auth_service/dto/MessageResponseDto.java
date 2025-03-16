package com.swiftchat.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple DTO for returning a message in responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard message response")
public class MessageResponseDto {

    @Schema(description = "Response message", example = "Operation completed successfully")
    private String message;
}
