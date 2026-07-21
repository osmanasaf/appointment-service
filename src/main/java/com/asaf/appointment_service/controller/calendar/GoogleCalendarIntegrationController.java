package com.asaf.appointment_service.controller.calendar;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.service.CurrentUserService;
import com.asaf.appointment_service.dto.calendar.GoogleCalendarConnectUrlResponse;
import com.asaf.appointment_service.dto.calendar.GoogleCalendarStatusResponse;
import com.asaf.appointment_service.entity.calendar.CalendarConnection;
import com.asaf.appointment_service.integration.email.EmailProperties;
import com.asaf.appointment_service.service.calendar.CalendarConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * `/callback` hariç tüm uçlar kimlik doğrulamalı panel çağrılarıdır. `/callback`
 * Google'ın tarayıcıyı yönlendirdiği public uç noktadır (bkz. SecurityConfig permitAll).
 */
@RestController
@RequestMapping("/api/v1/integrations/google")
public class GoogleCalendarIntegrationController {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarIntegrationController.class);

    private final CalendarConnectionService connectionService;
    private final CurrentUserService currentUserService;
    private final EmailProperties emailProperties;

    public GoogleCalendarIntegrationController(CalendarConnectionService connectionService,
                                                CurrentUserService currentUserService,
                                                EmailProperties emailProperties) {
        this.connectionService = connectionService;
        this.currentUserService = currentUserService;
        this.emailProperties = emailProperties;
    }

    @GetMapping("/connect-url")
    public ResponseEntity<ApiResponse<GoogleCalendarConnectUrlResponse>> getConnectUrl(
            @RequestParam Long employeeId) {
        Long businessId = currentUserService.getCurrentBusinessId();
        String url = connectionService.buildConnectUrl(businessId, employeeId);
        return ResponseEntity.ok(ApiResponse.success(new GoogleCalendarConnectUrlResponse(url)));
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestParam String state, @RequestParam String code) {
        String redirectBase = emailProperties.baseUrl() + "/settings/integrations/google";
        try {
            connectionService.handleCallback(state, code);
            return redirectTo(redirectBase + "?connected=true");
        } catch (Exception e) {
            log.error("Google Takvim OAuth callback başarısız: {}", e.getMessage());
            return redirectTo(redirectBase + "?connected=false");
        }
    }

    @PostMapping("/disconnect")
    public ResponseEntity<ApiResponse<Void>> disconnect(@RequestParam Long employeeId) {
        Long businessId = currentUserService.getCurrentBusinessId();
        connectionService.disconnect(businessId, employeeId);
        return ResponseEntity.ok(ApiResponse.success(null, "Google Takvim bağlantısı kesildi"));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<GoogleCalendarStatusResponse>> getStatus(@RequestParam Long employeeId) {
        CalendarConnection connection = connectionService.findByEmployeeIdOrNull(employeeId);
        if (connection == null) {
            return ResponseEntity.ok(ApiResponse.success(GoogleCalendarStatusResponse.notConnected()));
        }
        return ResponseEntity.ok(ApiResponse.success(new GoogleCalendarStatusResponse(
            true, connection.getStatus(), connection.getLastSyncedAt(), connection.getBrokenReason())));
    }

    private ResponseEntity<Void> redirectTo(String location) {
        return ResponseEntity.status(302).header("Location", location).build();
    }
}
