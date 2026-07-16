package com.secureiq.SecureIQ.electron.service;

import com.secureiq.SecureIQ.electron.dto.*;

public interface BrowserSessionService {
    BrowserSessionResponse connect(BrowserConnectRequest request);
    void disconnect(BrowserDisconnectRequest request);
    void heartbeat(BrowserHeartbeatRequest request);
    void recordEvent(BrowserEventRequest request);
    BrowserSessionResponse getSession(Long id);
    ElectronDashboardResponse getDashboard();
}
