package com.novabank.novapay.presentation.officer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import androidx.annotation.Nullable;

final class Base64ImageDecoder {

    private Base64ImageDecoder() {
    }

    @Nullable
    static Bitmap decode(@Nullable String rawBase64, int maxDimensionPx) {
        if (rawBase64 == null || rawBase64.trim().isEmpty()) {
            return null;
        }
        String clean = rawBase64.trim();
        int commaIdx = clean.indexOf(',');
        if (commaIdx >= 0 && commaIdx + 1 < clean.length()) {
            clean = clean.substring(commaIdx + 1);
        }
        try {
            byte[] bytes = Base64.decode(clean, Base64.DEFAULT);
            if (bytes.length == 0) {
                return null;
            }
            BitmapFactory.Options boundsOpts = new BitmapFactory.Options();
            boundsOpts.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, boundsOpts);

            int sampleSize = 1;
            int width = boundsOpts.outWidth;
            int height = boundsOpts.outHeight;
            if (maxDimensionPx > 0 && (width > maxDimensionPx || height > maxDimensionPx)) {
                while ((width / sampleSize) > maxDimensionPx || (height / sampleSize) > maxDimensionPx) {
                    sampleSize *= 2;
                }
            }

            BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
            decodeOpts.inSampleSize = sampleSize;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, decodeOpts);
        } catch (IllegalArgumentException | OutOfMemoryError ex) {
            return null;
        }
    }
}
