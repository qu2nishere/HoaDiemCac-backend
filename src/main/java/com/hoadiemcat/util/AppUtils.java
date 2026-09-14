package com.hoadiemcat.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class AppUtils {

    private AppUtils() {
        // Prevent instantiation
    }

    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

    public static String formatDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.format(FORMATTER);
    }

    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, FORMATTER);
    }
}
