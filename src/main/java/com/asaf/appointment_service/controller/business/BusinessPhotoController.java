package com.asaf.appointment_service.controller.business;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.service.CurrentUserService;
import com.asaf.appointment_service.dto.business.AddBusinessPhotoRequest;
import com.asaf.appointment_service.dto.business.BusinessPhotoResponse;
import com.asaf.appointment_service.entity.business.BusinessPhoto;
import com.asaf.appointment_service.mapper.business.BusinessPhotoMapper;
import com.asaf.appointment_service.service.business.BusinessPhotoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/businesses/photos")
public class BusinessPhotoController {

    private final BusinessPhotoService photoService;
    private final CurrentUserService currentUserService;

    public BusinessPhotoController(BusinessPhotoService photoService, CurrentUserService currentUserService) {
        this.photoService = photoService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BusinessPhotoResponse>>> getPhotos() {
        Long businessId = currentUserService.getCurrentBusinessId();
        List<BusinessPhoto> photos = photoService.findByBusinessId(businessId);
        return ResponseEntity.ok(ApiResponse.success(BusinessPhotoMapper.toResponseList(photos)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BusinessPhotoResponse>> addPhoto(
            @Valid @RequestBody AddBusinessPhotoRequest request) {
        Long businessId = currentUserService.getCurrentBusinessId();
        BusinessPhoto photo = photoService.addPhoto(businessId, request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(BusinessPhotoMapper.toResponse(photo), "Fotoğraf eklendi"));
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(@PathVariable Long photoId) {
        Long businessId = currentUserService.getCurrentBusinessId();
        photoService.deletePhoto(businessId, photoId);
        return ResponseEntity.ok(ApiResponse.success(null, "Fotoğraf silindi"));
    }
}
