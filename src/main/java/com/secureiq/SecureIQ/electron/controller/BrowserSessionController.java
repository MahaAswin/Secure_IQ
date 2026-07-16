package com.secureiq.SecureIQ.electron.controller;

import com.secureiq.SecureIQ.common.dto.ApiResponse;
import com.secureiq.SecureIQ.electron.dto.*;
import com.secureiq.SecureIQ.electron.service.BrowserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/browser")
@Tag(name = "Electron Proctoring Browser Integration", description = "APIs for integration between Electron Secure Browser client and proctoring backend")
public class BrowserSessionController {

    private final BrowserSessionService browserSessionService;

    public BrowserSessionController(BrowserSessionService browserSessionService) {
        this.browserSessionService = browserSessionService;
    }

    @PostMapping("/connect")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Establish connection for the Electron Browser", description = "Establishes a new browser connection session for an active exam attempt.")
    public ApiResponse<BrowserSessionResponse> connect(@Valid @RequestBody BrowserConnectRequest request) {
        BrowserSessionResponse response = browserSessionService.connect(request);
        return ApiResponse.success(response, "Browser connected successfully");
    }

    @PostMapping("/disconnect")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Close Electron Browser connection session", description = "Disconnects and closes the browser proctoring session.")
    public ApiResponse<Void> disconnect(@Valid @RequestBody BrowserDisconnectRequest request) {
        browserSessionService.disconnect(request);
        return ApiResponse.success("Browser disconnected successfully");
    }

    @PostMapping("/heartbeat")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Send heartbeat ping from the browser extension", description = "Keeps the browser session active during the exam.")
    public ApiResponse<Void> heartbeat(@Valid @RequestBody BrowserHeartbeatRequest request) {
        browserSessionService.heartbeat(request);
        return ApiResponse.success("Heartbeat registered successfully");
    }

    @PostMapping("/event")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Record browser event and automatically log proctoring violations", description = "Receives browser event triggers (e.g. TAB_SWITCH, WINDOW_BLUR) and converts them into violations.")
    public ApiResponse<Void> recordEvent(@Valid @RequestBody BrowserEventRequest request) {
        browserSessionService.recordEvent(request);
        return ApiResponse.success("Event processed successfully");
    }

    @GetMapping("/session/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get browser session log details by ID", description = "Retrieves browser session details.")
    public ApiResponse<BrowserSessionResponse> getSession(@PathVariable Long id) {
        BrowserSessionResponse response = browserSessionService.getSession(id);
        return ApiResponse.success(response, "Browser session retrieved successfully");
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    @Operation(summary = "Get proctoring browser dashboards", description = "Retrieves stats on connected students, inactive sessions, browser versions and operating systems.")
    public ApiResponse<ElectronDashboardResponse> getDashboard() {
        ElectronDashboardResponse response = browserSessionService.getDashboard();
        return ApiResponse.success(response, "Browser session dashboard retrieved successfully");
    }
}
