package com.novabank.novapay.domain.model.user;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.regex.Pattern;

public final class CustomerValidation {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^(0|\\+84)[0-9]{9,10}$");
    private static final Pattern NATIONAL_ID_PATTERN =
            Pattern.compile("^[0-9]{9}([0-9]{3})?$");
    private static final DateTimeFormatter ISO_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.US)
                    .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter VN_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.US)
                    .withResolverStyle(ResolverStyle.STRICT);

    private CustomerValidation() {
    }

    public static boolean isValidFullName(String raw) {
        return raw != null && raw.trim().length() >= 2;
    }

    public static boolean isValidEmail(String raw) {
        if (raw == null) {
            return false;
        }
        return EMAIL_PATTERN.matcher(raw.trim()).matches();
    }

    public static String normalizePhone(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("[\\s.-]", "");
    }

    public static boolean isValidPhone(String raw) {
        String normalized = normalizePhone(raw);
        return PHONE_PATTERN.matcher(normalized).matches();
    }

    public static boolean isValidNationalId(String raw) {
        if (raw == null) {
            return false;
        }
        return NATIONAL_ID_PATTERN.matcher(raw.trim()).matches();
    }

    public static String normalizeDobToIso(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        LocalDate parsed = parseStrictDate(trimmed, ISO_DATE_FORMATTER);
        if (parsed == null) {
            parsed = parseStrictDate(trimmed, VN_DATE_FORMATTER);
        }
        if (parsed == null || parsed.getYear() < 1900 || parsed.isAfter(LocalDate.now())) {
            return null;
        }
        return parsed.format(ISO_DATE_FORMATTER);
    }

    public static boolean isValidDob(String raw) {
        return normalizeDobToIso(raw) != null;
    }

    public static boolean isValidAddress(String raw) {
        return raw != null && !raw.trim().isEmpty();
    }

    public static boolean isValidInitialPassword(String raw) {
        return raw != null && raw.length() >= 6;
    }

    public static boolean matchesQuery(CustomerProfile profile, String query) {
        if (profile == null) {
            return false;
        }
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        return containsIgnoreCase(profile.getFullName(), q)
                || containsIgnoreCase(profile.getEmail(), q)
                || containsIgnoreCase(profile.getPhone(), q)
                || containsIgnoreCase(profile.getNationalId(), q);
    }

    private static boolean containsIgnoreCase(String value, String lowerQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerQuery);
    }

    private static LocalDate parseStrictDate(String input, DateTimeFormatter formatter) {
        try {
            return LocalDate.parse(input, formatter);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
