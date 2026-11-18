package com.novabank.novapay.data.mapper;

import com.google.firebase.Timestamp;

import java.time.Instant;
import java.util.Map;

/**
 * Tiện ích chuyển đổi an toàn các giá trị từ Firestore sang kiểu Java 17 chuẩn
 * (Instant, long cho tiền tệ, double cho lãi suất/tỷ lệ).
 */
public final class FirestoreValueMapper {

    private FirestoreValueMapper() {
        // Cấm khởi tạo
    }

    public static Instant toInstant(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Timestamp) {
            Timestamp ts = (Timestamp) raw;
            return Instant.ofEpochSecond(ts.getSeconds(), ts.getNanoseconds());
        }
        if (raw instanceof Instant) {
            return (Instant) raw;
        }
        if (raw instanceof Number) {
            return Instant.ofEpochMilli(((Number) raw).longValue());
        }
        if (raw instanceof String) {
            String str = ((String) raw).trim();
            if (str.isEmpty()) {
                return null;
            }
            try {
                return Instant.parse(str);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    public static long toLong(Object raw, long defaultValue) {
        if (raw instanceof Long) {
            return (Long) raw;
        }
        if (raw instanceof Integer) {
            return ((Integer) raw).longValue();
        }
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        if (raw instanceof String) {
            try {
                return Long.parseLong(((String) raw).trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static int toInt(Object raw, int defaultValue) {
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        if (raw instanceof String) {
            try {
                return Integer.parseInt(((String) raw).trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static double toDouble(Object raw, double defaultValue) {
        if (raw instanceof Number) {
            return ((Number) raw).doubleValue();
        }
        if (raw instanceof String) {
            try {
                return Double.parseDouble(((String) raw).trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static String toStringOrEmpty(Object raw) {
        return raw != null ? String.valueOf(raw) : "";
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object raw) {
        if (raw instanceof Map<?, ?>) {
            return (Map<String, Object>) raw;
        }
        return null;
    }
}
