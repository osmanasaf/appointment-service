package com.asaf.appointment_service.dto.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
    @NotBlank(message = "Müşteri adı zorunludur")
    @Size(min = 2, max = 100, message = "Ad 2-100 karakter arasında olmalıdır")
    String name,

    @Email(message = "Geçerli bir e-posta adresi giriniz")
    String email,

    @Size(max = 500, message = "Notlar en fazla 500 karakter olabilir")
    String notes,

    @Size(max = 500, message = "Adres en fazla 500 karakter olabilir")
    String address,

    @Size(max = 11, message = "TC kimlik numarası en fazla 11 haneli olmalıdır")
    @Pattern(regexp = "|\\d{11}", message = "TC kimlik numarası boş veya 11 rakam olmalıdır")
    String nationalId
) {}
