package com.asaf.appointment_service.entity.token;

public enum PublicActionTokenType {
    /** subjectId = appointmentId */
    RESCHEDULE,
    /** subjectId = waitlistEntryId */
    WAITLIST_OFFER,
    /** subjectId = appointmentId */
    REVIEW,
    /** subjectId = appointmentId; tekrar tekrar indirilebilir olması için hiç consume edilmez */
    CALENDAR_ICS
}
