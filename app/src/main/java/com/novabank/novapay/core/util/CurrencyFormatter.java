package com.novabank.novapay.core.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Định dạng tiền tệ thống nhất cho toàn bộ ứng dụng NovaPay.
 * BẮT BUỘC chỉ sử dụng kiểu nguyên thủy long / Long (đơn vị VNĐ), tuyệt đối cấm double/float.
 */
public final class CurrencyFormatter {

    private static final Locale LOCALE_VI_VN = Locale.forLanguageTag("vi-VN");

    private CurrencyFormatter() {
        // Cấm khởi tạo
    }

    /**
     * Định dạng số tiền nguyên VNĐ sang chuỗi chuẩn Việt Nam (ví dụ: 1250000L -> "1.250.000 ₫").
     */
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

    /**
     * Định dạng số tiền có kèm dấu biến động số dư ("+1.250.000 ₫" hoặc "-500.000 ₫").
     */
    public static String formatSignedVnd(long amount, boolean isInflow) {
        long absAmount = Math.abs(amount);
        String formatted = formatVnd(absAmount);
        if (absAmount == 0L) {
            return formatted;
        }
        return (isInflow ? "+" : "-") + formatted;
    }

    /**
     * Tách lọc các chữ số từ chuỗi người dùng nhập vào EditText thành kiểu long an toàn.
     * Trả về 0L nếu chuỗi rỗng hoặc không hợp lệ.
     */
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
