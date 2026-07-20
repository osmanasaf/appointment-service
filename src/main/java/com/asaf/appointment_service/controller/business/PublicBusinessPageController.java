package com.asaf.appointment_service.controller.business;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.dto.business.PublicBusinessPageResponse;
import com.asaf.appointment_service.service.business.PublicBusinessPageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/businesses")
public class PublicBusinessPageController {

    private final PublicBusinessPageService pageService;

    public PublicBusinessPageController(PublicBusinessPageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping("/{slug}/page")
    public ResponseEntity<ApiResponse<PublicBusinessPageResponse>> getPage(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(pageService.getPageBySlug(slug)));
    }
}
