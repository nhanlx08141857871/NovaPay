package com.novabank.novapay.core.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class CurrencyFormatter {

    private static final Locale LOCALE_VI_VN = Locale.forLanguageTag("vi-VN");

    private CurrencyFormatter() {
    }

    public static String formatVnd(long amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(LOCALE_VI_VN);
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        formatter.setGroupingUsed(true);
        formatter.setGroupingSize(3);
        if (amount < 0L) {
            // Tránh tràn số với Long.MIN_VALUE bằng chuỗi chuẩn
            if (amount == Long.MIN_VALUE) {
                return "-9.223.372.036.854.775.808 ₫";
            }
            return "-" + formatter.format(-amount) + " ₫";
        }
        return formatter.format(amount) + " ₫";
    }

    public static String formatSignedVnd(long amount, boolean isInflow) {
        long absAmount = Math.abs(amount);
        String formatted = formatVnd(absAmount);
        if (absAmount == 0L) {
            return formatted;
        }
        return (isInflow ? "+" : "-") + formatted;
    }

    public static long parseVndDigits(String rawInput) {
        if (rawInput == null || rawInput.isEmpty()) {
            return 0L;
        }
        String digitsOnly = rawInput.replaceAll("[^0-9]", "");
        if (digitsOnly.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(digitsOnly);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
