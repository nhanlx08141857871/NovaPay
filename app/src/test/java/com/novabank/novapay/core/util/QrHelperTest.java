package com.novabank.novapay.core.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class QrHelperTest {

    @Test
    public void buildAndParseTransferUri_preservesVietnameseUtf8AndFields() {
        String uri = QrHelper.buildTransferQrUri(
                "1000123456",
                "NOVAPAY",
                "Nguyễn Văn Anh",
                1_250_000L,
                "Chuyển tiền ăn trưa Quận 7"
        );

        assertTrue(uri.startsWith("novapay://transfer?"));

        QrHelper.QrTransferPayload parsed = QrHelper.parseTransferQrPayload(uri);
        assertNotNull(parsed);
        assertEquals("1000123456", parsed.getAccountNumber());
        assertEquals("NOVAPAY", parsed.getBankCode());
        assertEquals("Nguyễn Văn Anh", parsed.getRecipientName());
        assertEquals(1_250_000L, parsed.getAmount());
        assertEquals("Chuyển tiền ăn trưa Quận 7", parsed.getNote());
    }

    @Test
    public void parseTransferQrPayload_supportsQrAliasUriAndOptionalFields() {
        QrHelper.QrTransferPayload parsed = QrHelper.parseTransferQrPayload(
                "novapay://qr?acc=1000999888&bank=VCB&name=Tran%20Minh&amount=500000"
        );

        assertNotNull(parsed);
        assertEquals("1000999888", parsed.getAccountNumber());
        assertEquals("VCB", parsed.getBankCode());
        assertEquals("Tran Minh", parsed.getRecipientName());
        assertEquals(500_000L, parsed.getAmount());
        assertNull(parsed.getNote());
    }

    @Test
    public void parseTransferQrPayload_supportsJsonPayload() {
        String json = "{\"toAccountNumber\":\"1000555666\",\"bankCode\":\"TCB\",\"name\":\"Lê Thị Hoa\",\"amount\":250000,\"note\":\"Trả nợ\"}";

        QrHelper.QrTransferPayload parsed = QrHelper.parseTransferQrPayload(json);
        assertNotNull(parsed);
        assertEquals("1000555666", parsed.getAccountNumber());
        assertEquals("TCB", parsed.getBankCode());
        assertEquals("Lê Thị Hoa", parsed.getRecipientName());
        assertEquals(250_000L, parsed.getAmount());
        assertEquals("Trả nợ", parsed.getNote());
    }

    @Test
    public void parseTransferQrPayload_supportsPlainAccountNumberFallback() {
        QrHelper.QrTransferPayload parsed = QrHelper.parseTransferQrPayload("  NP10002233  ");

        assertNotNull(parsed);
        assertEquals("NP10002233", parsed.getAccountNumber());
        assertNull(parsed.getBankCode());
        assertNull(parsed.getRecipientName());
        assertEquals(0L, parsed.getAmount());
        assertNull(parsed.getNote());
    }

    @Test
    public void parseTransferQrPayload_rejectsInvalidBlankNegativeOrWrongScheme() {
        assertNull(QrHelper.parseTransferQrPayload(null));
        assertNull(QrHelper.parseTransferQrPayload("   "));
        assertNull(QrHelper.parseTransferQrPayload("123")); // quá ngắn (< 4 ký tự)
        assertNull(QrHelper.parseTransferQrPayload("https://example.com?accountNumber=1000123456"));
        assertNull(QrHelper.parseTransferQrPayload("novapay://home?accountNumber=1000123456"));
        assertNull(QrHelper.parseTransferQrPayload("novapay://transfer?accountNumber=1000123456&amount=-100"));
        assertNull(QrHelper.parseTransferQrPayload("novapay://transfer?accountNumber=1000123456&amount=abc"));
        assertNull(QrHelper.parseTransferQrPayload("{\"accountNumber\":\"1000123456\",\"amount\":-50}"));
        assertNull(QrHelper.parseTransferQrPayload("{\"bankCode\":\"VCB\",\"amount\":1000}"));
    }

    @Test
    public void encodeQrMatrix_producesValidIsoMatrixWithFinderAndTimingPatterns() {
        String shortPayload = "1000123456";
        boolean[][] v1 = QrHelper.encodeQrMatrix(shortPayload);
        assertEquals(21, v1.length);
        assertEquals(21, v1[0].length);
        assertFinderPatternsAndTimingValid(v1);

        String longUri = QrHelper.buildTransferQrUri(
                "1000123456",
                "NOVAPAY",
                "Nguyễn Hoàng Minh Quân",
                6_500_000L,
                "Thanh toán tiền thuê căn hộ tháng 11 tại TP.HCM"
        );
        boolean[][] larger = QrHelper.encodeQrMatrix(longUri);
        assertTrue(larger.length > 21);
        assertEquals(larger.length, larger[0].length);
        assertEquals(1, (larger.length - 17) % 4 == 0 ? 1 : 0);
        assertFinderPatternsAndTimingValid(larger);
    }

    private static void assertFinderPatternsAndTimingValid(boolean[][] m) {
        int size = m.length;
        // 3 finder patterns (7x7) at top-left, top-right, bottom-left
        assertFinderAt(m, 0, 0);
        assertFinderAt(m, 0, size - 7);
        assertFinderAt(m, size - 7, 0);

        // White separators
        for (int i = 0; i < 8; i++) {
            assertFalse(m[7][i]);
            assertFalse(m[i][7]);
            assertFalse(m[7][size - 8 + i]);
            assertFalse(m[i][size - 8]);
            assertFalse(m[size - 8][i]);
            assertFalse(m[size - 8 + i][7]);
        }

        // Dark module at (size - 8, 8)
        assertTrue(m[size - 8][8]);

        // Timing patterns along row 6 and col 6
        for (int i = 8; i < size - 8; i++) {
            boolean expected = (i % 2 == 0);
            assertEquals(expected, m[6][i]);
            assertEquals(expected, m[i][6]);
        }
    }

    private static void assertFinderAt(boolean[][] m, int top, int left) {
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                boolean expectedDark = r == 0 || r == 6 || c == 0 || c == 6
                        || (r >= 2 && r <= 4 && c >= 2 && c <= 4);
                assertEquals("Finder mismatch at (" + (top + r) + "," + (left + c) + ")",
                        expectedDark, m[top + r][left + c]);
            }
        }
    }
}
