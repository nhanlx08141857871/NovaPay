package com.novabank.novapay.core.util;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public final class QrHelper {

    private static final Pattern PLAIN_ACCOUNT_PATTERN = Pattern.compile("^[A-Za-z0-9]{4,24}$");
    private static final int QUIET_ZONE_MODULES = 4;

    private QrHelper() {
    }

    public static final class QrTransferPayload {
        private final String accountNumber;
        private final String bankCode;
        private final String recipientName;
        private final long amount;
        private final String note;

        public QrTransferPayload(
                String accountNumber,
                String bankCode,
                String recipientName,
                long amount,
                String note
        ) {
            this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber must not be null").trim();
            this.bankCode = trimToNull(bankCode);
            this.recipientName = trimToNull(recipientName);
            this.amount = Math.max(0L, amount);
            this.note = trimToNull(note);
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public String getBankCode() {
            return bankCode;
        }

        public String getRecipientName() {
            return recipientName;
        }

        public long getAmount() {
            return amount;
        }

        public String getNote() {
            return note;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof QrTransferPayload that)) return false;
            return amount == that.amount
                    && Objects.equals(accountNumber, that.accountNumber)
                    && Objects.equals(bankCode, that.bankCode)
                    && Objects.equals(recipientName, that.recipientName)
                    && Objects.equals(note, that.note);
        }

        @Override
        public int hashCode() {
            return Objects.hash(accountNumber, bankCode, recipientName, amount, note);
        }
    }

    public static String buildTransferQrUri(String accountNumber, String bankCode, long amount, String note) {
        return buildTransferQrUri(accountNumber, bankCode, null, amount, note);
    }

    public static String buildTransferQrUri(
            String accountNumber,
            String bankCode,
            String recipientName,
            long amount,
            String note
    ) {
        String cleanAccount = trimToNull(accountNumber);
        if (cleanAccount == null || !PLAIN_ACCOUNT_PATTERN.matcher(cleanAccount).matches()) {
            throw new IllegalArgumentException("accountNumber không hợp lệ.");
        }
        if (amount < 0L) {
            throw new IllegalArgumentException("amount không được âm.");
        }

        StringBuilder sb = new StringBuilder("novapay://transfer?accountNumber=")
                .append(encodeQueryParam(cleanAccount));

        String cleanBank = trimToNull(bankCode);
        if (cleanBank != null) {
            sb.append("&bankCode=").append(encodeQueryParam(cleanBank));
        }
        String cleanName = trimToNull(recipientName);
        if (cleanName != null) {
            sb.append("&recipientName=").append(encodeQueryParam(cleanName));
        }
        if (amount > 0L) {
            sb.append("&amount=").append(amount);
        }
        String cleanNote = trimToNull(note);
        if (cleanNote != null) {
            sb.append("&note=").append(encodeQueryParam(cleanNote));
        }
        return sb.toString();
    }

    /**
     * Phân tích chuỗi quét từ mã QR thành {@link QrTransferPayload}.
     * Hỗ trợ:
     * (a) URI `novapay://transfer?...` hoặc `novapay://qr?...`
     * (b) JSON `{"accountNumber":..., "toAccountNumber":..., "bankCode":..., "recipientName":..., "amount":..., "note":...}`
     * (c) Số tài khoản thuần `^[A-Za-z0-9]{4,24}$`
     * Trả về {@code null} nếu dữ liệu rỗng, sai định dạng hoặc số tiền âm/không hợp lệ.
     */
    public static QrTransferPayload parseTransferQrPayload(String rawPayload) {
        String trimmed = trimToNull(rawPayload);
        if (trimmed == null) {
            return null;
        }

        if (trimmed.startsWith("novapay://")) {
            return parseNovaPayUri(trimmed);
        }

        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return parseJsonPayload(trimmed);
        }

        if (PLAIN_ACCOUNT_PATTERN.matcher(trimmed).matches()) {
            return new QrTransferPayload(trimmed, null, null, 0L, null);
        }

        return null;
    }

    private static QrTransferPayload parseNovaPayUri(String rawUri) {
        try {
            URI uri = URI.create(rawUri);
            if (!"novapay".equalsIgnoreCase(uri.getScheme())) {
                return null;
            }
            String host = uri.getHost();
            if (!"transfer".equalsIgnoreCase(host) && !"qr".equalsIgnoreCase(host)) {
                return null;
            }
            Map<String, String> params = parseQueryParams(uri.getRawQuery());
            String accountNumber = firstNonBlank(params.get("accountNumber"), params.get("acc"));
            if (accountNumber == null || !PLAIN_ACCOUNT_PATTERN.matcher(accountNumber).matches()) {
                return null;
            }
            String bankCode = firstNonBlank(params.get("bankCode"), params.get("bank"));
            String recipientName = firstNonBlank(params.get("recipientName"), params.get("name"));
            String note = params.get("note");

            long amount = 0L;
            String amountStr = trimToNull(params.get("amount"));
            if (amountStr != null) {
                amount = Long.parseLong(amountStr);
                if (amount < 0L) {
                    return null;
                }
            }
            return new QrTransferPayload(accountNumber, bankCode, recipientName, amount, note);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static QrTransferPayload parseJsonPayload(String rawJson) {
        try {
            JsonElement parsed = JsonParser.parseString(rawJson);
            if (!parsed.isJsonObject()) {
                return null;
            }
            JsonObject obj = parsed.getAsJsonObject();
            String accountNumber = firstNonBlank(
                    getJsonString(obj, "accountNumber"),
                    firstNonBlank(getJsonString(obj, "toAccountNumber"), getJsonString(obj, "acc"))
            );
            if (accountNumber == null || !PLAIN_ACCOUNT_PATTERN.matcher(accountNumber).matches()) {
                return null;
            }
            String bankCode = firstNonBlank(getJsonString(obj, "bankCode"), getJsonString(obj, "bank"));
            String recipientName = firstNonBlank(getJsonString(obj, "recipientName"), getJsonString(obj, "name"));
            String note = getJsonString(obj, "note");

            long amount = 0L;
            if (obj.has("amount") && !obj.get("amount").isJsonNull()) {
                JsonElement amountEl = obj.get("amount");
                if (!amountEl.isJsonPrimitive()) {
                    return null;
                }
                if (amountEl.getAsJsonPrimitive().isNumber()) {
                    String numText = amountEl.getAsString().trim();
                    if (numText.contains(".") || numText.contains("e") || numText.contains("E")) {
                        double d = amountEl.getAsDouble();
                        if (d < 0 || d != Math.floor(d)) {
                            return null;
                        }
                        amount = (long) d;
                    } else {
                        amount = Long.parseLong(numText);
                    }
                } else {
                    String amtText = trimToNull(amountEl.getAsString());
                    if (amtText != null) {
                        amount = Long.parseLong(amtText);
                    }
                }
                if (amount < 0L) {
                    return null;
                }
            }
            return new QrTransferPayload(accountNumber, bankCode, recipientName, amount, note);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Map<String, String> parseQueryParams(String rawQuery) {
        Map<String, String> map = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return map;
        }
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) {
                continue;
            }
            int idx = pair.indexOf('=');
            String rawKey = idx >= 0 ? pair.substring(0, idx) : pair;
            String rawVal = idx >= 0 ? pair.substring(idx + 1) : "";
            String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
            String val = URLDecoder.decode(rawVal, StandardCharsets.UTF_8);
            map.put(key, val);
        }
        return map;
    }

    private static String encodeQueryParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String getJsonString(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        JsonElement el = obj.get(key);
        return el.isJsonPrimitive() ? trimToNull(el.getAsString()) : null;
    }

    private static String firstNonBlank(String a, String b) {
        String ca = trimToNull(a);
        return ca != null ? ca : trimToNull(b);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static final int[] TOTAL_DATA_CODEWORDS_M = {
            0, 16, 28, 44, 64, 86, 108, 124, 154, 182, 216, 254, 290, 334, 365, 415
    };
    private static final int[] EC_CODEWORDS_PER_BLOCK_M = {
            0, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24
    };
    private static final int[] BLOCKS_G1_M = {
            0, 1, 1, 1, 2, 2, 4, 4, 2, 3, 4, 1, 6, 8, 4, 5
    };
    private static final int[] BLOCKS_G2_M = {
            0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 1, 4, 2, 1, 5, 5
    };
    private static final int[] REMAINDER_BITS = {
            0, 0, 7, 7, 7, 7, 7, 0, 0, 0, 0, 0, 0, 0, 3, 3
    };
    private static final int[][] ALIGNMENT_POSITIONS = {
            {},
            {},
            {6, 18},
            {6, 22},
            {6, 26},
            {6, 30},
            {6, 34},
            {6, 22, 38},
            {6, 24, 42},
            {6, 26, 46},
            {6, 28, 50},
            {6, 30, 54},
            {6, 32, 58},
            {6, 34, 62},
            {6, 26, 46, 66},
            {6, 26, 48, 70}
    };

    private static final int[] GF_EXP = new int[512];
    private static final int[] GF_LOG = new int[256];

    static {
        int x = 1;
        for (int i = 0; i < 255; i++) {
            GF_EXP[i] = x;
            GF_LOG[x] = i;
            x <<= 1;
            if (x >= 256) {
                x ^= 0x11D;
            }
        }
        for (int i = 255; i < 512; i++) {
            GF_EXP[i] = GF_EXP[i - 255];
        }
    }

    public static boolean[][] encodeQrMatrix(String content) {
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("Nội dung QR không được rỗng.");
        }
        byte[] utf8 = content.getBytes(StandardCharsets.UTF_8);
        int version = selectVersion(utf8.length);
        int size = 17 + 4 * version;

        int[] dataCodewords = packByteModeData(utf8, version);
        int[] interleavedBits = buildInterleavedCodewords(dataCodewords, version);

        boolean[][] baseModules = new boolean[size][size];
        boolean[][] isFunction = new boolean[size][size];

        drawFunctionPatterns(baseModules, isFunction, version, size);
        placeDataBits(baseModules, isFunction, interleavedBits, size, REMAINDER_BITS[version]);

        int bestMask = 0;
        int minPenalty = Integer.MAX_VALUE;
        boolean[][] bestMatrix = null;

        for (int mask = 0; mask < 8; mask++) {
            boolean[][] candidate = copyMatrix(baseModules, size);
            applyMask(candidate, isFunction, mask, size);
            drawFormatInfo(candidate, mask, size);
            if (version >= 7) {
                drawVersionInfo(candidate, version, size);
            }
            int penalty = calculatePenalty(candidate, size);
            if (penalty < minPenalty) {
                minPenalty = penalty;
                bestMask = mask;
                bestMatrix = candidate;
            }
        }
        return bestMatrix != null ? bestMatrix : baseModules;
    }

    public static Bitmap generateQrBitmap(String content, int sizePx) {
        if (sizePx <= 0) {
            throw new IllegalArgumentException("Kích thước Bitmap phải lớn hơn 0.");
        }
        boolean[][] matrix = encodeQrMatrix(content);
        int moduleCount = matrix.length;
        int totalModules = moduleCount + QUIET_ZONE_MODULES * 2;
        int scale = Math.max(1, sizePx / totalModules);
        int renderedSize = totalModules * scale;

        int[] pixels = new int[renderedSize * renderedSize];
        for (int y = 0; y < renderedSize; y++) {
            int moduleY = (y / scale) - QUIET_ZONE_MODULES;
            for (int x = 0; x < renderedSize; x++) {
                int moduleX = (x / scale) - QUIET_ZONE_MODULES;
                boolean dark = moduleY >= 0 && moduleY < moduleCount
                        && moduleX >= 0 && moduleX < moduleCount
                        && matrix[moduleY][moduleX];
                pixels[y * renderedSize + x] = dark ? Color.BLACK : Color.WHITE;
            }
        }

        Bitmap raw = Bitmap.createBitmap(pixels, renderedSize, renderedSize, Bitmap.Config.ARGB_8888);
        if (renderedSize == sizePx) {
            return raw;
        }
        Bitmap scaled = Bitmap.createScaledBitmap(raw, sizePx, sizePx, false);
        if (scaled != raw) {
            raw.recycle();
        }
        return scaled;
    }

    private static int selectVersion(int byteLength) {
        for (int v = 1; v <= 15; v++) {
            int countBits = v <= 9 ? 8 : 16;
            int availableBits = TOTAL_DATA_CODEWORDS_M[v] * 8;
            int requiredBits = 4 + countBits + byteLength * 8;
            if (requiredBits <= availableBits) {
                return v;
            }
        }
        throw new IllegalArgumentException("Nội dung QR vượt quá dung lượng tối đa (" + byteLength + " bytes).");
    }

    private static int[] packByteModeData(byte[] data, int version) {
        int totalCodewords = TOTAL_DATA_CODEWORDS_M[version];
        int totalBits = totalCodewords * 8;
        int[] bits = new int[totalBits];
        int bitIndex = 0;

        bitIndex = appendBits(bits, bitIndex, 0b0100, 4);
        int countBits = version <= 9 ? 8 : 16;
        bitIndex = appendBits(bits, bitIndex, data.length, countBits);
        for (byte b : data) {
            bitIndex = appendBits(bits, bitIndex, b & 0xFF, 8);
        }
        int terminator = Math.min(4, totalBits - bitIndex);
        bitIndex += terminator;
        while (bitIndex % 8 != 0 && bitIndex < totalBits) {
            bitIndex++;
        }

        int[] codewords = new int[totalCodewords];
        for (int i = 0; i < bitIndex / 8; i++) {
            int val = 0;
            for (int b = 0; b < 8; b++) {
                val = (val << 1) | bits[i * 8 + b];
            }
            codewords[i] = val;
        }
        int padIndex = 0;
        for (int i = bitIndex / 8; i < totalCodewords; i++) {
            codewords[i] = (padIndex % 2 == 0) ? 0xEC : 0x11;
            padIndex++;
        }
        return codewords;
    }

    private static int appendBits(int[] buffer, int offset, int value, int length) {
        for (int i = length - 1; i >= 0; i--) {
            buffer[offset++] = (value >>> i) & 1;
        }
        return offset;
    }

    private static int[] buildInterleavedCodewords(int[] dataCodewords, int version) {
        int g1Blocks = BLOCKS_G1_M[version];
        int g2Blocks = BLOCKS_G2_M[version];
        int totalBlocks = g1Blocks + g2Blocks;
        int ecPerBlock = EC_CODEWORDS_PER_BLOCK_M[version];

        int totalData = TOTAL_DATA_CODEWORDS_M[version];
        int shortLen = (totalData - g2Blocks) / totalBlocks;
        int longLen = shortLen + (g2Blocks > 0 ? 1 : 0);

        int[][] dataBlocks = new int[totalBlocks][];
        int[][] ecBlocks = new int[totalBlocks][];
        int[] rsGen = rsGeneratorPoly(ecPerBlock);

        int offset = 0;
        for (int b = 0; b < totalBlocks; b++) {
            int len = b < g1Blocks ? shortLen : shortLen + 1;
            int[] block = new int[len];
            System.arraycopy(dataCodewords, offset, block, 0, len);
            offset += len;
            dataBlocks[b] = block;
            ecBlocks[b] = rsComputeRemainder(block, rsGen, ecPerBlock);
        }

        int totalCodewords = totalData + totalBlocks * ecPerBlock;
        int[] interleaved = new int[totalCodewords];
        int idx = 0;

        for (int col = 0; col < longLen; col++) {
            for (int b = 0; b < totalBlocks; b++) {
                if (col < dataBlocks[b].length) {
                    interleaved[idx++] = dataBlocks[b][col];
                }
            }
        }
        for (int col = 0; col < ecPerBlock; col++) {
            for (int b = 0; b < totalBlocks; b++) {
                interleaved[idx++] = ecBlocks[b][col];
            }
        }
        return interleaved;
    }

    private static int gfMul(int a, int b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        return GF_EXP[GF_LOG[a] + GF_LOG[b]];
    }

    private static int[] rsGeneratorPoly(int degree) {
        int[] poly = new int[]{1};
        for (int i = 0; i < degree; i++) {
            int[] next = new int[poly.length + 1];
            int factor = GF_EXP[i];
            for (int j = 0; j < poly.length; j++) {
                next[j] ^= poly[j];
                next[j + 1] ^= gfMul(poly[j], factor);
            }
            poly = next;
        }
        return poly;
    }

    private static int[] rsComputeRemainder(int[] data, int[] gen, int ecLen) {
        int[] work = new int[data.length + ecLen];
        System.arraycopy(data, 0, work, 0, data.length);
        for (int i = 0; i < data.length; i++) {
            int factor = work[i];
            if (factor != 0) {
                for (int j = 1; j < gen.length; j++) {
                    work[i + j] ^= gfMul(gen[j], factor);
                }
            }
        }
        int[] ec = new int[ecLen];
        System.arraycopy(work, data.length, ec, 0, ecLen);
        return ec;
    }

    private static void drawFunctionPatterns(
            boolean[][] matrix,
            boolean[][] isFunction,
            int version,
            int size
    ) {
        drawFinderPattern(matrix, isFunction, 0, 0, size);
        drawFinderPattern(matrix, isFunction, 0, size - 7, size);
        drawFinderPattern(matrix, isFunction, size - 7, 0, size);

        int[] align = ALIGNMENT_POSITIONS[version];
        for (int r : align) {
            for (int c : align) {
                if ((r <= 8 && c <= 8) || (r <= 8 && c >= size - 8) || (r >= size - 8 && c <= 8)) {
                    continue;
                }
                drawAlignmentPattern(matrix, isFunction, r, c);
            }
        }

        for (int i = 8; i < size - 8; i++) {
            boolean dark = (i % 2 == 0);
            matrix[6][i] = dark;
            isFunction[6][i] = true;
            matrix[i][6] = dark;
            isFunction[i][6] = true;
        }

        matrix[size - 8][8] = true;
        isFunction[size - 8][8] = true;

        for (int i = 0; i <= 8; i++) {
            if (i != 6) {
                isFunction[8][i] = true;
                isFunction[i][8] = true;
            }
        }
        for (int i = size - 8; i < size; i++) {
            isFunction[8][i] = true;
        }
        for (int i = size - 7; i < size; i++) {
            isFunction[i][8] = true;
        }

        if (version >= 7) {
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 3; j++) {
                    isFunction[size - 11 + j][i] = true;
                    isFunction[i][size - 11 + j] = true;
                }
            }
        }
    }

    private static void drawFinderPattern(
            boolean[][] matrix,
            boolean[][] isFunction,
            int topRow,
            int leftCol,
            int size
    ) {
        for (int dr = -1; dr <= 7; dr++) {
            int r = topRow + dr;
            if (r < 0 || r >= size) continue;
            for (int dc = -1; dc <= 7; dc++) {
                int c = leftCol + dc;
                if (c < 0 || c >= size) continue;
                boolean dark = dr >= 0 && dr <= 6 && dc >= 0 && dc <= 6
                        && (dr == 0 || dr == 6 || dc == 0 || dc == 6
                        || (dr >= 2 && dr <= 4 && dc >= 2 && dc <= 4));
                matrix[r][c] = dark;
                isFunction[r][c] = true;
            }
        }
    }

    private static void drawAlignmentPattern(
            boolean[][] matrix,
            boolean[][] isFunction,
            int centerRow,
            int centerCol
    ) {
        for (int dr = -2; dr <= 2; dr++) {
            for (int dc = -2; dc <= 2; dc++) {
                int dist = Math.max(Math.abs(dr), Math.abs(dc));
                matrix[centerRow + dr][centerCol + dc] = (dist != 1);
                isFunction[centerRow + dr][centerCol + dc] = true;
            }
        }
    }

    private static void placeDataBits(
            boolean[][] matrix,
            boolean[][] isFunction,
            int[] codewords,
            int size,
            int remainderBits
    ) {
        int totalBits = codewords.length * 8 + remainderBits;
        int bitIdx = 0;
        int dir = -1;
        int col = size - 1;

        while (col > 0) {
            if (col == 6) {
                col--;
            }
            for (int step = 0; step < size; step++) {
                int row = (dir == -1) ? (size - 1 - step) : step;
                for (int offset = 0; offset < 2; offset++) {
                    int c = col - offset;
                    if (!isFunction[row][c]) {
                        boolean bit = false;
                        if (bitIdx < codewords.length * 8) {
                            int cw = codewords[bitIdx / 8];
                            bit = ((cw >>> (7 - (bitIdx % 8))) & 1) == 1;
                        }
                        if (bitIdx < totalBits) {
                            matrix[row][c] = bit;
                            bitIdx++;
                        }
                    }
                }
            }
            dir = -dir;
            col -= 2;
        }
    }

    private static void applyMask(boolean[][] matrix, boolean[][] isFunction, int mask, int size) {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (!isFunction[r][c] && isMasked(mask, r, c)) {
                    matrix[r][c] = !matrix[r][c];
                }
            }
        }
    }

    private static boolean isMasked(int mask, int r, int c) {
        return switch (mask) {
            case 0 -> (r + c) % 2 == 0;
            case 1 -> r % 2 == 0;
            case 2 -> c % 3 == 0;
            case 3 -> (r + c) % 3 == 0;
            case 4 -> (r / 2 + c / 3) % 2 == 0;
            case 5 -> (r * c) % 2 + (r * c) % 3 == 0;
            case 6 -> ((r * c) % 2 + (r * c) % 3) % 2 == 0;
            case 7 -> ((r + c) % 2 + (r * c) % 3) % 2 == 0;
            default -> false;
        };
    }

    private static void drawFormatInfo(boolean[][] matrix, int mask, int size) {
        int data = (0b00 << 3) | (mask & 0x7);
        int rem = data << 10;
        for (int i = 14; i >= 10; i--) {
            if (((rem >>> i) & 1) != 0) {
                rem ^= (0x537 << (i - 10));
            }
        }
        int bits = ((data << 10) | rem) ^ 0x5412;

        for (int i = 0; i <= 5; i++) {
            matrix[8][i] = bitAt(bits, 14 - i);
        }
        matrix[8][7] = bitAt(bits, 8);
        matrix[8][8] = bitAt(bits, 7);
        matrix[7][8] = bitAt(bits, 6);
        for (int i = 0; i <= 5; i++) {
            matrix[5 - i][8] = bitAt(bits, 5 - i);
        }

        for (int i = 0; i < 7; i++) {
            matrix[size - 1 - i][8] = bitAt(bits, 14 - i);
        }
        for (int i = 0; i < 8; i++) {
            matrix[8][size - 8 + i] = bitAt(bits, 7 - i);
        }
    }

    private static void drawVersionInfo(boolean[][] matrix, int version, int size) {
        int rem = version << 12;
        for (int i = 17; i >= 12; i--) {
            if (((rem >>> i) & 1) != 0) {
                rem ^= (0x1F25 << (i - 12));
            }
        }
        int bits = (version << 12) | rem;
        for (int i = 0; i < 18; i++) {
            boolean bit = bitAt(bits, i);
            int a = size - 11 + (i % 3);
            int b = i / 3;
            matrix[a][b] = bit;
            matrix[b][a] = bit;
        }
    }

    private static boolean bitAt(int value, int bitIndex) {
        return ((value >>> bitIndex) & 1) == 1;
    }

    private static boolean[][] copyMatrix(boolean[][] source, int size) {
        boolean[][] copy = new boolean[size][size];
        for (int i = 0; i < size; i++) {
            System.arraycopy(source[i], 0, copy[i], 0, size);
        }
        return copy;
    }

    private static int calculatePenalty(boolean[][] matrix, int size) {
        int penalty = 0;
        for (int r = 0; r < size; r++) {
            int runLen = 1;
            for (int c = 1; c < size; c++) {
                if (matrix[r][c] == matrix[r][c - 1]) {
                    runLen++;
                    if (runLen == 5) {
                        penalty += 3;
                    } else if (runLen > 5) {
                        penalty += 1;
                    }
                } else {
                    runLen = 1;
                }
            }
        }
        for (int c = 0; c < size; c++) {
            int runLen = 1;
            for (int r = 1; r < size; r++) {
                if (matrix[r][c] == matrix[r - 1][c]) {
                    runLen++;
                    if (runLen == 5) {
                        penalty += 3;
                    } else if (runLen > 5) {
                        penalty += 1;
                    }
                } else {
                    runLen = 1;
                }
            }
        }
        for (int r = 0; r < size - 1; r++) {
            for (int c = 0; c < size - 1; c++) {
                boolean color = matrix[r][c];
                if (color == matrix[r][c + 1] && color == matrix[r + 1][c] && color == matrix[r + 1][c + 1]) {
                    penalty += 3;
                }
            }
        }
        for (int r = 0; r < size; r++) {
            for (int c = 0; c <= size - 11; c++) {
                if (matchesFinderLikeRow(matrix, r, c)) {
                    penalty += 40;
                }
            }
        }
        for (int c = 0; c < size; c++) {
            for (int r = 0; r <= size - 11; r++) {
                if (matchesFinderLikeCol(matrix, r, c)) {
                    penalty += 40;
                }
            }
        }
        int darkCount = 0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (matrix[r][c]) {
                    darkCount++;
                }
            }
        }
        int total = size * size;
        int percent = (darkCount * 100) / total;
        int prevMultiple = (percent / 5) * 5;
        int nextMultiple = prevMultiple + 5;
        penalty += Math.min(Math.abs(prevMultiple - 50) / 5, Math.abs(nextMultiple - 50) / 5) * 10;
        return penalty;
    }

    private static boolean matchesFinderLikeRow(boolean[][] m, int r, int c) {
        boolean p1 = m[r][c] && !m[r][c + 1] && m[r][c + 2] && m[r][c + 3] && m[r][c + 4]
                && !m[r][c + 5] && m[r][c + 6] && !m[r][c + 7] && !m[r][c + 8] && !m[r][c + 9] && !m[r][c + 10];
        boolean p2 = !m[r][c] && !m[r][c + 1] && !m[r][c + 2] && !m[r][c + 3] && m[r][c + 4]
                && !m[r][c + 5] && m[r][c + 6] && m[r][c + 7] && m[r][c + 8] && !m[r][c + 9] && m[r][c + 10];
        return p1 || p2;
    }

    private static boolean matchesFinderLikeCol(boolean[][] m, int r, int c) {
        boolean p1 = m[r][c] && !m[r + 1][c] && m[r + 2][c] && m[r + 3][c] && m[r + 4][c]
                && !m[r + 5][c] && m[r + 6][c] && !m[r + 7][c] && !m[r + 8][c] && !m[r + 9][c] && !m[r + 10][c];
        boolean p2 = !m[r][c] && !m[r + 1][c] && !m[r + 2][c] && !m[r + 3][c] && m[r + 4][c]
                && !m[r + 5][c] && m[r + 6][c] && m[r + 7][c] && m[r + 8][c] && !m[r + 9][c] && m[r + 10][c];
        return p1 || p2;
    }
}
