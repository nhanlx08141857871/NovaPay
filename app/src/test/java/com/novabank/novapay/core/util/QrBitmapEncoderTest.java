package com.novabank.novapay.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class QrBitmapEncoderTest {

    @Test
    public void encodeMatrix_accountNumber_producesValidVersion1WithQuietZone() {
        boolean[][] matrix = QrBitmapEncoder.encodeMatrix("1001000001");
        // Version 1 = 21x21 + 4 quiet zone on each side = 29x29
        assertEquals(29, matrix.length);
        assertEquals(29, matrix[0].length);

        // Quiet zone border must be white (false)
        for (int i = 0; i < matrix.length; i++) {
            assertFalse(matrix[0][i]);
            assertFalse(matrix[matrix.length - 1][i]);
            assertFalse(matrix[i][0]);
            assertFalse(matrix[i][matrix.length - 1]);
        }

        // Top-left finder pattern center (4 + 3 = 7, 7) must be dark (true)
        assertTrue(matrix[7][7]);
        // Top-right finder pattern center (4 + 3 = 7, 4 + 21 - 4 = 21)
        assertTrue(matrix[7][21]);
        // Bottom-left finder pattern center (21, 7)
        assertTrue(matrix[21][7]);
        // Top-left finder inner ring (5, 5) must be white (false)
        assertFalse(matrix[5][5]);
    }

    @Test
    public void encodeMatrix_longerPayload_scalesUpVersionAndIsDeterministic() {
        String payload = "novapay://transfer?accountNumber=1001000001&name=NguyenHoangAnh";
        boolean[][] first = QrBitmapEncoder.encodeMatrix(payload);
        boolean[][] second = QrBitmapEncoder.encodeMatrix(payload);

        assertTrue(first.length > 29);
        assertEquals(first.length, second.length);
        for (int r = 0; r < first.length; r++) {
            for (int c = 0; c < first.length; c++) {
                assertEquals(first[r][c], second[r][c]);
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void encodeMatrix_emptyContent_throwsIllegalArgumentException() {
        QrBitmapEncoder.encodeMatrix("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void encodeMatrix_oversizedContent_throwsIllegalArgumentException() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 250; i++) {
            sb.append('A');
        }
        QrBitmapEncoder.encodeMatrix(sb.toString());
    }
}
