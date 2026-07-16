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
public class BrowserConnectRequest {

    @NotBlank(message = "Attempt code is required")
    private String attemptCode;

    private String browserVersion;

    private String operatingSystem;

    private String machineId;

    private String ipAddress;
}
