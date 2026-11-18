package com.novabank.novapay.core.util;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Bộ tạo mã QR chuẩn ISO/IEC 18004 (Byte Mode, Error Correction Level M, Version 1–10)
 * viết hoàn toàn bằng Pure Java 17 (không phụ thuộc thư viện ngoài ZXing) để hiển thị
 * mã QR nhận tiền cá nhân tại CheckingDetailFragment.
 * Không chạm tới QrHelper.java (thuộc quyền sở hữu của Module B).
 */
public final class QrBitmapEncoder {

    private static final int QUIET_ZONE_MODULES = 4;

    // Bảng thông số EC Level M cho Version 1..10:
    // {totalDataCodewords, eccPerBlock, group1Blocks, group1Data, group2Blocks, group2Data}
    private static final int[][] EC_M_SPECS = {
            null, // 1-indexed
            {16, 10, 1, 16, 0, 0},   // V1 (21x21)
            {28, 16, 1, 28, 0, 0},   // V2 (25x25)
            {44, 26, 1, 44, 0, 0},   // V3 (29x29)
            {64, 18, 2, 32, 0, 0},   // V4 (33x33)
            {86, 24, 2, 43, 0, 0},   // V5 (37x37)
            {108, 16, 4, 27, 0, 0},  // V6 (41x41)
            {124, 18, 4, 31, 0, 0},  // V7 (45x45)
            {154, 22, 2, 38, 2, 39}, // V8 (49x49)
            {182, 22, 3, 36, 2, 37}, // V9 (53x53)
            {216, 26, 4, 43, 1, 44}  // V10 (57x57)
    };

    private static final int[][] ALIGNMENT_CENTERS = {
            null,
            {},
            {6, 18},
            {6, 22},
            {6, 26},
            {6, 30},
            {6, 34},
            {6, 22, 38},
            {6, 24, 42},
            {6, 26, 46},
            {6, 28, 50}
    };

    private static final int[] GF_EXP = new int[512];
    private static final int[] GF_LOG = new int[256];

    static {
        int x = 1;
        for (int i = 0; i < 255; i++) {
            GF_EXP[i] = x;
            GF_LOG[x] = i;
            x <<= 1;
            if ((x & 0x100) != 0) {
                x ^= 0x11D;
            }
        }
        for (int i = 255; i < 512; i++) {
            GF_EXP[i] = GF_EXP[i - 255];
        }
    }

    private QrBitmapEncoder() {
        // Cấm khởi tạo
    }

    /**
     * Mã hóa nội dung thành ma trận boolean[][] chuẩn QR (bao gồm vùng lề trắng 4 ô).
     * Phương thức Pure Java hoàn toàn — kiểm thử được trực tiếp trên JVM JUnit 4.
     */
    public static boolean[][] encodeMatrix(String content) {
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("QR content must not be empty");
        }
        byte[] utf8 = content.getBytes(StandardCharsets.UTF_8);
        int version = selectVersion(utf8.length);
        int[] spec = EC_M_SPECS[version];

        byte[] dataCodewords = encodeByteModeData(utf8, version, spec[0]);
        byte[] finalCodewords = interleaveWithEcc(dataCodewords, spec);

        int size = 17 + 4 * version;
        boolean[][] bestModules = null;
        int bestPenalty = Integer.MAX_VALUE;

        for (int mask = 0; mask < 8; mask++) {
            boolean[][] modules = new boolean[size][size];
            boolean[][] isFunction = new boolean[size][size];

            drawFunctionPatterns(modules, isFunction, version, size);
            placeCodewords(modules, isFunction, finalCodewords, size);
            applyMask(modules, isFunction, mask, size);
            drawFormatBits(modules, mask, size);
            if (version >= 7) {
                drawVersionBits(modules, version, size);
            }

            int penalty = computePenaltyScore(modules, size);
            if (penalty < bestPenalty) {
                bestPenalty = penalty;
                bestModules = modules;
            }
        }

        int paddedSize = size + QUIET_ZONE_MODULES * 2;
        boolean[][] padded = new boolean[paddedSize][paddedSize];
        for (int r = 0; r < size; r++) {
            System.arraycopy(bestModules[r], 0, padded[r + QUIET_ZONE_MODULES], QUIET_ZONE_MODULES, size);
        }
        return padded;
    }

    /**
     * Kết xuất ma trận QR thành Android Bitmap sắc nét (ARGB_8888).
     */
    public static Bitmap encodeToBitmap(String content, int sizePx) {
        if (sizePx <= 0) {
            throw new IllegalArgumentException("sizePx must be positive");
        }
        boolean[][] matrix = encodeMatrix(content);
        int matrixSize = matrix.length;
        int scale = Math.max(1, sizePx / matrixSize);
        int rawSize = matrixSize * scale;
        int[] pixels = new int[rawSize * rawSize];

        for (int mr = 0; mr < matrixSize; mr++) {
            for (int mc = 0; mc < matrixSize; mc++) {
                int color = matrix[mr][mc] ? Color.BLACK : Color.WHITE;
                int yBase = mr * scale;
                int xBase = mc * scale;
                for (int dy = 0; dy < scale; dy++) {
                    int rowOffset = (yBase + dy) * rawSize + xBase;
                    Arrays.fill(pixels, rowOffset, rowOffset + scale, color);
                }
            }
        }

        Bitmap crisp = Bitmap.createBitmap(pixels, rawSize, rawSize, Bitmap.Config.ARGB_8888);
        if (rawSize == sizePx) {
            return crisp;
        }
        Bitmap scaled = Bitmap.createScaledBitmap(crisp, sizePx, sizePx, false);
        if (scaled != crisp) {
            crisp.recycle();
        }
        return scaled;
    }

    private static int selectVersion(int byteLength) {
        for (int v = 1; v <= 10; v++) {
            int dataBytes = EC_M_SPECS[v][0];
            int countBits = v <= 9 ? 8 : 16;
            int headerBits = 4 + countBits;
            int capacityBytes = (dataBytes * 8 - headerBits) / 8;
            if (byteLength <= capacityBytes) {
                return v;
            }
        }
        throw new IllegalArgumentException("QR content exceeds Version 10-M capacity (" + byteLength + " bytes)");
    }

    private static byte[] encodeByteModeData(byte[] data, int version, int totalDataCodewords) {
        int totalBits = totalDataCodewords * 8;
        int[] bits = new int[totalBits];
        int bitPos = 0;

        // Mode indicator: Byte = 0100
        bitPos = appendBits(bits, bitPos, 0b0100, 4);
        int countBits = version <= 9 ? 8 : 16;
        bitPos = appendBits(bits, bitPos, data.length, countBits);

        for (byte b : data) {
            bitPos = appendBits(bits, bitPos, b & 0xFF, 8);
        }

        // Terminator up to 4 zero bits
        int terminator = Math.min(4, totalBits - bitPos);
        bitPos += terminator;

        // Pad to byte boundary
        while (bitPos % 8 != 0) {
            bitPos++;
        }

        byte[] codewords = new byte[totalDataCodewords];
        int byteCount = bitPos / 8;
        for (int i = 0; i < byteCount; i++) {
            int val = 0;
            for (int b = 0; b < 8; b++) {
                val = (val << 1) | bits[i * 8 + b];
            }
            codewords[i] = (byte) val;
        }

        // Pad codewords alternating 0xEC, 0x11
        boolean toggle = true;
        for (int i = byteCount; i < totalDataCodewords; i++) {
            codewords[i] = toggle ? (byte) 0xEC : (byte) 0x11;
            toggle = !toggle;
        }
        return codewords;
    }

    private static int appendBits(int[] buffer, int pos, int value, int numBits) {
        for (int i = numBits - 1; i >= 0; i--) {
            buffer[pos++] = (value >>> i) & 1;
        }
        return pos;
    }

    private static byte[] interleaveWithEcc(byte[] data, int[] spec) {
        int eccLen = spec[1];
        int g1Blocks = spec[2];
        int g1Data = spec[3];
        int g2Blocks = spec[4];
        int g2Data = spec[5];
        int totalBlocks = g1Blocks + g2Blocks;

        byte[][] dataBlocks = new byte[totalBlocks][];
        byte[][] eccBlocks = new byte[totalBlocks][];
        int[] genPoly = buildRsGenerator(eccLen);

        int offset = 0;
        for (int b = 0; b < totalBlocks; b++) {
            int len = b < g1Blocks ? g1Data : g2Data;
            byte[] block = new byte[len];
            System.arraycopy(data, offset, block, 0, len);
            offset += len;
            dataBlocks[b] = block;
            eccBlocks[b] = computeRsEcc(block, genPoly, eccLen);
        }

        int maxDataLen = Math.max(g1Data, g2Data);
        byte[] result = new byte[data.length + totalBlocks * eccLen];
        int out = 0;

        for (int i = 0; i < maxDataLen; i++) {
            for (int b = 0; b < totalBlocks; b++) {
                if (i < dataBlocks[b].length) {
                    result[out++] = dataBlocks[b][i];
                }
            }
        }
        for (int i = 0; i < eccLen; i++) {
            for (int b = 0; b < totalBlocks; b++) {
                result[out++] = eccBlocks[b][i];
            }
        }
        return result;
    }

    private static int[] buildRsGenerator(int degree) {
        int[] gen = new int[degree];
        gen[degree - 1] = 1;
        int root = 1;
        for (int i = 0; i < degree; i++) {
            for (int j = 0; j < degree; j++) {
                gen[j] = gfMul(gen[j], root);
                if (j + 1 < degree) {
                    gen[j] ^= gen[j + 1];
                }
            }
            root = gfMul(root, 0x02);
        }
        return gen;
    }

    private static byte[] computeRsEcc(byte[] data, int[] genPoly, int eccLen) {
        int[] rem = new int[eccLen];
        for (byte b : data) {
            int factor = (b & 0xFF) ^ rem[0];
            System.arraycopy(rem, 1, rem, 0, eccLen - 1);
            rem[eccLen - 1] = 0;
            for (int i = 0; i < eccLen; i++) {
                rem[i] ^= gfMul(genPoly[i], factor);
            }
        }
        byte[] ecc = new byte[eccLen];
        for (int i = 0; i < eccLen; i++) {
            ecc[i] = (byte) rem[i];
        }
        return ecc;
    }

    private static int gfMul(int a, int b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        return GF_EXP[GF_LOG[a] + GF_LOG[b]];
    }

    private static void drawFunctionPatterns(
            boolean[][] modules,
            boolean[][] isFunction,
            int version,
            int size
    ) {
        // Timing patterns
        for (int i = 0; i < size; i++) {
            boolean dark = i % 2 == 0;
            setFunctionModule(modules, isFunction, 6, i, dark);
            setFunctionModule(modules, isFunction, i, 6, dark);
        }

        // Finder patterns + separators
        drawFinderPattern(modules, isFunction, 3, 3, size);
        drawFinderPattern(modules, isFunction, 3, size - 4, size);
        drawFinderPattern(modules, isFunction, size - 4, 3, size);

        // Alignment patterns
        int[] centers = ALIGNMENT_CENTERS[version];
        for (int r : centers) {
            for (int c : centers) {
                if ((r == 6 && c == 6) || (r == 6 && c == size - 7) || (r == size - 7 && c == 6)) {
                    continue;
                }
                drawAlignmentPattern(modules, isFunction, r, c);
            }
        }

        // Dark module
        setFunctionModule(modules, isFunction, size - 8, 8, true);

        // Reserve format info strips (filled later)
        for (int i = 0; i <= 8; i++) {
            if (i != 6) {
                isFunction[8][i] = true;
                isFunction[i][8] = true;
            }
        }
        for (int i = 0; i < 8; i++) {
            isFunction[8][size - 1 - i] = true;
            isFunction[size - 1 - i][8] = true;
        }

        // Reserve version info areas for V7+
        if (version >= 7) {
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 3; j++) {
                    isFunction[i][size - 11 + j] = true;
                    isFunction[size - 11 + j][i] = true;
                }
            }
        }
    }

    private static void drawFinderPattern(
            boolean[][] modules,
            boolean[][] isFunction,
            int centerR,
            int centerC,
            int size
    ) {
        for (int dr = -4; dr <= 4; dr++) {
            for (int dc = -4; dc <= 4; dc++) {
                int r = centerR + dr;
                int c = centerC + dc;
                if (r < 0 || r >= size || c < 0 || c >= size) {
                    continue;
                }
                int dist = Math.max(Math.abs(dr), Math.abs(dc));
                boolean dark = dist != 2 && dist != 4;
                setFunctionModule(modules, isFunction, r, c, dark);
            }
        }
    }

    private static void drawAlignmentPattern(
            boolean[][] modules,
            boolean[][] isFunction,
            int centerR,
            int centerC
    ) {
        for (int dr = -2; dr <= 2; dr++) {
            for (int dc = -2; dc <= 2; dc++) {
                int dist = Math.max(Math.abs(dr), Math.abs(dc));
                setFunctionModule(modules, isFunction, centerR + dr, centerC + dc, dist != 1);
            }
        }
    }

    private static void setFunctionModule(
            boolean[][] modules,
            boolean[][] isFunction,
            int r,
            int c,
            boolean dark
    ) {
        modules[r][c] = dark;
        isFunction[r][c] = true;
    }

    private static void placeCodewords(
            boolean[][] modules,
            boolean[][] isFunction,
            byte[] codewords,
            int size
    ) {
        int bitIndex = 0;
        int totalBits = codewords.length * 8;
        boolean upward = true;

        for (int right = size - 1; right >= 1; right -= 2) {
            if (right == 6) {
                right = 5;
            }
            for (int vert = 0; vert < size; vert++) {
                int r = upward ? (size - 1 - vert) : vert;
                for (int j = 0; j < 2; j++) {
                    int c = right - j;
                    if (!isFunction[r][c]) {
                        boolean dark = false;
                        if (bitIndex < totalBits) {
                            dark = ((codewords[bitIndex >>> 3] >>> (7 - (bitIndex & 7))) & 1) != 0;
                            bitIndex++;
                        }
                        modules[r][c] = dark;
                    }
                }
            }
            upward = !upward;
        }
    }

    private static void applyMask(
            boolean[][] modules,
            boolean[][] isFunction,
            int mask,
            int size
    ) {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (isFunction[r][c]) {
                    continue;
                }
                boolean invert;
                switch (mask) {
                    case 0:
                        invert = (r + c) % 2 == 0;
                        break;
                    case 1:
                        invert = r % 2 == 0;
                        break;
                    case 2:
                        invert = c % 3 == 0;
                        break;
                    case 3:
                        invert = (r + c) % 3 == 0;
                        break;
                    case 4:
                        invert = ((r / 2) + (c / 3)) % 2 == 0;
                        break;
                    case 5:
                        invert = ((r * c) % 2) + ((r * c) % 3) == 0;
                        break;
                    case 6:
                        invert = (((r * c) % 2) + ((r * c) % 3)) % 2 == 0;
                        break;
                    default:
                        invert = (((r + c) % 2) + ((r * c) % 3)) % 2 == 0;
                        break;
                }
                if (invert) {
                    modules[r][c] = !modules[r][c];
                }
            }
        }
    }

    private static void drawFormatBits(boolean[][] modules, int mask, int size) {
        // EC Level M = 00
        int data = mask & 0x7;
        int rem = data;
        for (int i = 0; i < 10; i++) {
            rem = (rem << 1) ^ (((rem >>> 9) & 1) * 0x537);
        }
        int bits = ((data << 10) | rem) ^ 0x5412;

        for (int i = 0; i <= 5; i++) {
            modules[8][i] = ((bits >>> i) & 1) != 0;
        }
        modules[8][7] = ((bits >>> 6) & 1) != 0;
        modules[8][8] = ((bits >>> 7) & 1) != 0;
        modules[7][8] = ((bits >>> 8) & 1) != 0;
        for (int i = 9; i < 15; i++) {
            modules[14 - i][8] = ((bits >>> i) & 1) != 0;
        }

        for (int i = 0; i < 8; i++) {
            modules[size - 1 - i][8] = ((bits >>> i) & 1) != 0;
        }
        for (int i = 8; i < 15; i++) {
            modules[8][size - 15 + i] = ((bits >>> i) & 1) != 0;
        }
    }

    private static void drawVersionBits(boolean[][] modules, int version, int size) {
        int rem = version;
        for (int i = 0; i < 12; i++) {
            rem = (rem << 1) ^ (((rem >>> 11) & 1) * 0x1F25);
        }
        int bits = (version << 12) | rem;
        for (int i = 0; i < 18; i++) {
            boolean dark = ((bits >>> i) & 1) != 0;
            int a = size - 11 + (i % 3);
            int b = i / 3;
            modules[a][b] = dark;
            modules[b][a] = dark;
        }
    }

    private static int computePenaltyScore(boolean[][] modules, int size) {
        int penalty = 0;

        // N1: adjacent same-color runs >= 5 in rows and columns
        for (int r = 0; r < size; r++) {
            int runLen = 1;
            for (int c = 1; c < size; c++) {
                if (modules[r][c] == modules[r][c - 1]) {
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
                if (modules[r][c] == modules[r - 1][c]) {
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

        // N2: 2x2 blocks of same color
        for (int r = 0; r < size - 1; r++) {
            for (int c = 0; c < size - 1; c++) {
                boolean color = modules[r][c];
                if (modules[r][c + 1] == color
                        && modules[r + 1][c] == color
                        && modules[r + 1][c + 1] == color) {
                    penalty += 3;
                }
            }
        }

        // N3: finder-like 1:1:3:1:1 patterns
        for (int r = 0; r < size; r++) {
            for (int c = 0; c <= size - 11; c++) {
                if (matchesFinderLikeRow(modules, r, c)) {
                    penalty += 40;
                }
            }
        }
        for (int c = 0; c < size; c++) {
            for (int r = 0; r <= size - 11; r++) {
                if (matchesFinderLikeCol(modules, r, c)) {
                    penalty += 40;
                }
            }
        }

        // N4: dark module balance around 50%
        int darkCount = 0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (modules[r][c]) {
                    darkCount++;
                }
            }
        }
        int total = size * size;
        int k = Math.abs(darkCount * 20 - total * 10) / total;
        penalty += k * 10;

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
