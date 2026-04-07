package com.asaf.appointment_service.domain.valueobject;

public record ServiceDuration(int minutes) {

    private static final int MIN_DURATION = 15;
    private static final int MAX_DURATION = 360;

    public ServiceDuration {
        if (minutes < MIN_DURATION || minutes > MAX_DURATION) {
            throw new IllegalArgumentException(
                String.format("Hizmet süresi %d-%d dakika arasında olmalıdır", MIN_DURATION, MAX_DURATION));
        }
    }

    public static ServiceDuration of(int minutes) {
        return new ServiceDuration(minutes);
    }

    public static ServiceDuration ofHours(int hours) {
        return new ServiceDuration(hours * 60);
    }

    public int hours() {
        return minutes / 60;
    }

    public int remainingMinutes() {
        return minutes % 60;
    }

    public String displayFormat() {
        if (minutes < 60) {
            return minutes + " dk";
        }
        int h = hours();
        int m = remainingMinutes();
        return m > 0 ? String.format("%d sa %d dk", h, m) : String.format("%d saat", h);
    }

    public ServiceDuration withBuffer(int bufferMinutes) {
        return new ServiceDuration(Math.min(minutes + bufferMinutes, MAX_DURATION));
    }
}
