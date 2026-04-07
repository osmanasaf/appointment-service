package com.asaf.appointment_service.common.exception;

public class EntityNotFoundException extends BusinessException {

    public EntityNotFoundException(String entityName, Long id) {
        super("ENTITY_NOT_FOUND", entityName + " bulunamadı: " + id);
    }

    public EntityNotFoundException(String entityName, String identifier) {
        super("ENTITY_NOT_FOUND", entityName + " bulunamadı: " + identifier);
    }
}
