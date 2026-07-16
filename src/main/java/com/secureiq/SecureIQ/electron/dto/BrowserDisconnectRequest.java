package com.secureiq.SecureIQ.electron.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowserDisconnectRequest {
    @NotBlank(message = "Session ID is required")
    private String sessionId;
}
