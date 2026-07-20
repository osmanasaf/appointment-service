package com.asaf.appointment_service.dto.business;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddBusinessPhotoRequest(
    @NotBlank(message = "Fotoğraf URL'si zorunludur")
    @Size(max = 2048, message = "Fotoğraf URL'si çok uzun")
    String url,

    @Size(max = 200, message = "Açıklama en fazla 200 karakter olabilir")
    String caption
) {}
