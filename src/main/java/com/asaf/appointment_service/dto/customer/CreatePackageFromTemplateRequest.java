package com.asaf.appointment_service.dto.customer;

import jakarta.validation.constraints.NotNull;

public record CreatePackageFromTemplateRequest(
    @NotNull(message = "Müşteri ID zorunludur")
    Long customerId,

    @NotNull(message = "Şablon ID zorunludur")
    Long templateId
) {}
