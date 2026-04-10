package com.asaf.appointment_service.integration.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MetaMessageResponse(
    @JsonProperty("messaging_product") String messagingProduct,
    List<Contact> contacts,
    List<Message> messages,
    MetaError error
) {
    public boolean isSuccess() {
        return error == null && messages != null && !messages.isEmpty();
    }

    public String getFirstMessageId() {
        if (messages == null || messages.isEmpty()) return null;
        return messages.get(0).id();
    }

    public String getErrorMessage() {
        if (error == null) return null;
        return error.message() + (error.errorData() != null ? " - " + error.errorData().details() : "");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(
        String input,
        @JsonProperty("wa_id") String waId
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MetaError(
        String message,
        String type,
        int code,
        @JsonProperty("error_subcode") int errorSubcode,
        @JsonProperty("fbtrace_id") String fbtraceId,
        @JsonProperty("error_data") ErrorData errorData
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErrorData(String details) {}
}
