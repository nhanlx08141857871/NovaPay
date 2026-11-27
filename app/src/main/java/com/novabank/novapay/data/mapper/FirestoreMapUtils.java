package com.novabank.novapay.data.mapper;

import com.google.firebase.Timestamp;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

public final class FirestoreMapUtils {

    private FirestoreMapUtils() {
    }

    public static Instant toInstant(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Timestamp timestamp) {
            return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanoseconds());
        }
        if (raw instanceof Instant instant) {
            return instant;
        }
        if (raw instanceof Date date) {
            return date.toInstant();
        }
        if (raw instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        if (raw instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                return Instant.parse(trimmed);
            } catch (Exception ignored) {
                try {
                    return Instant.ofEpochMilli(Long.parseLong(trimmed));
                } catch (NumberFormatException ignored2) {
                    return null;
                }
            }
        }
        return null;
    }

    public static long toLong(Object raw, long defaultValue) {
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static int toInt(Object raw, int defaultValue) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static double toDouble(Object raw, double defaultValue) {
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        if (raw instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static boolean toBoolean(Object raw, boolean defaultValue) {
        if (raw instanceof Boolean bool) {
            return bool;
        }
        if (raw instanceof String text) {
            return Boolean.parseBoolean(text.trim());
        }
        return defaultValue;
    }

    public static String toStringOrNull(Object raw) {
        if (raw == null) {
            return null;
        }
        String text = String.valueOf(raw).trim();
        return text.isEmpty() ? null : text;
    }

    public static String toStringOrEmpty(Object raw) {
        String s = toStringOrNull(raw);
        return s != null ? s : "";
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toStringMap(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }
}
