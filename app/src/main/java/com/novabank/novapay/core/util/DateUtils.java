package com.novabank.novapay.core.util;

import com.novabank.novapay.core.common.Constants;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DateUtils {

    public static final ZoneId VIETNAM_ZONE = ZoneId.of(Constants.ZONE_HO_CHI_MINH);
    private static final Locale LOCALE_VI_VN = Locale.forLanguageTag("vi-VN");

    private static final DateTimeFormatter FORMATTER_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", LOCALE_VI_VN).withZone(VIETNAM_ZONE);
    private static final DateTimeFormatter FORMATTER_DATE_ONLY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE_VI_VN).withZone(VIETNAM_ZONE);
    private static final DateTimeFormatter FORMATTER_MONTH_YEAR =
            DateTimeFormatter.ofPattern("MM/yyyy", LOCALE_VI_VN).withZone(VIETNAM_ZONE);

    private DateUtils() {
    }

    public static String formatDateTime(Instant instant) {
        if (instant == null) {
            return "";
        }
        return FORMATTER_DATE_TIME.format(instant);
    }

    public static String formatDate(Instant instant) {
        if (instant == null) {
            return "";
        }
        return FORMATTER_DATE_ONLY.format(instant);
    }

    public static String formatMonthYear(Instant instant) {
        if (instant == null) {
            return "";
        }
        return FORMATTER_MONTH_YEAR.format(instant);
    }

    public static Instant startOfTodayVietnam() {
        ZonedDateTime startOfDay = LocalDate.now(VIETNAM_ZONE).atStartOfDay(VIETNAM_ZONE);
        return startOfDay.toInstant();
    }
}
