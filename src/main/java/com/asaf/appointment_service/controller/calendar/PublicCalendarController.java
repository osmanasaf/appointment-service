package com.asaf.appointment_service.controller.calendar;

import com.asaf.appointment_service.service.calendar.IcsService;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/appointments")
public class PublicCalendarController {

    private final IcsService icsService;

    public PublicCalendarController(IcsService icsService) {
        this.icsService = icsService;
    }

    @GetMapping(value = "/{token}/calendar.ics")
    public ResponseEntity<byte[]> downloadIcs(@PathVariable String token) {
        String ics = icsService.generateIcsForToken(token);
        byte[] body = ics.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"randevu.ics\"")
            .body(body);
    }
}
