package com.novabank.novapay.core.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;

import com.novabank.novapay.core.common.Constants;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Tiện ích chuyển đổi ImageProxy (CameraX) / Bitmap sang JPEG dung lượng < 200KB
 * và mã hóa Base64 cho luồng eKYC và FaceCheck (> 5.000.000 VNĐ).
 */
public final class ImageCompressor {

    private static final int INITIAL_MAX_EDGE_PX = 1024;
    private static final int[] QUALITY_STEPS = {85, 75, 65, 55, 45, 35, 25};

    private ImageCompressor() {
        // Cấm khởi tạo
    }

    @NonNull
    public static String compressImageProxyToBase64(@NonNull ImageProxy imageProxy) {
        Bitmap bitmap = imageProxyToBitmap(imageProxy);
        if (bitmap == null) {
            throw new IllegalStateException("Cannot decode CameraX ImageProxy to Bitmap");
        }
        try {
            byte[] jpegBytes = compressToJpegBytes(bitmap, Constants.MAX_EKYC_IMAGE_BYTES);
            return toBase64(jpegBytes);
        } finally {
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    @Nullable
    public static Bitmap imageProxyToBitmap(@NonNull ImageProxy imageProxy) {
        int format = imageProxy.getFormat();
        Bitmap rawBitmap = null;

        if (format == ImageFormat.JPEG && imageProxy.getPlanes().length > 0) {
            ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else if (imageProxy.getPlanes().length >= 3) {
            byte[] nv21 = yuv420888ToNv21(imageProxy);
            YuvImage yuvImage = new YuvImage(
                    nv21,
                    ImageFormat.NV21,
                    imageProxy.getWidth(),
                    imageProxy.getHeight(),
                    null
            );
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(
                    new Rect(0, 0, imageProxy.getWidth(), imageProxy.getHeight()),
                    90,
                    out
            );
            byte[] jpeg = out.toByteArray();
            rawBitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
        }

        if (rawBitmap == null) {
            return null;
        }
        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
        if (rotationDegrees == 0) {
            return rawBitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        Bitmap rotated = Bitmap.createBitmap(
                rawBitmap,
                0,
                0,
                rawBitmap.getWidth(),
                rawBitmap.getHeight(),
                matrix,
                true
        );
        if (rotated != rawBitmap) {
            rawBitmap.recycle();
        }
        return rotated;
    }

    @NonNull
    public static byte[] compressToJpegBytes(@NonNull Bitmap source, int maxBytes) {
        Bitmap current = scaleDownIfNeeded(source, INITIAL_MAX_EDGE_PX);
        boolean ownsCurrent = current != source;

        try {
            for (int round = 0; round < 5; round++) {
                for (int quality : QUALITY_STEPS) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    current.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                    byte[] bytes = baos.toByteArray();
                    if (bytes.length <= maxBytes) {
                        return bytes;
                    }
                }
                int nextW = Math.max(160, (int) (current.getWidth() * 0.75f));
                int nextH = Math.max(160, (int) (current.getHeight() * 0.75f));
                Bitmap scaled = Bitmap.createScaledBitmap(current, nextW, nextH, true);
                if (ownsCurrent && scaled != current) {
                    current.recycle();
                }
                current = scaled;
                ownsCurrent = true;
            }
            ByteArrayOutputStream finalBaos = new ByteArrayOutputStream();
            current.compress(Bitmap.CompressFormat.JPEG, 20, finalBaos);
            return finalBaos.toByteArray();
        } finally {
            if (ownsCurrent && !current.isRecycled()) {
                current.recycle();
            }
        }
    }

    @NonNull
    public static String toBase64(@NonNull byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    @NonNull
    private static Bitmap scaleDownIfNeeded(@NonNull Bitmap source, int maxEdge) {
        int width = source.getWidth();
        int height = source.getHeight();
        int largest = Math.max(width, height);
        if (largest <= maxEdge || largest <= 0) {
            return source;
        }
        float scale = (float) maxEdge / (float) largest;
        int targetW = Math.max(1, Math.round(width * scale));
        int targetH = Math.max(1, Math.round(height * scale));
        return Bitmap.createScaledBitmap(source, targetW, targetH, true);
    }

    @NonNull
    private static byte[] yuv420888ToNv21(@NonNull ImageProxy image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int ySize = width * height;
        int uvSize = width * height / 4;
        byte[] nv21 = new byte[ySize + uvSize * 2];

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int yRowStride = image.getPlanes()[0].getRowStride();
        int yPixelStride = image.getPlanes()[0].getPixelStride();
        int pos = 0;

        if (yPixelStride == 1 && yRowStride == width) {
            yBuffer.get(nv21, 0, ySize);
            pos = ySize;
        } else {
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    nv21[pos++] = yBuffer.get(row * yRowStride + col * yPixelStride);
                }
            }
        }

        int uvRowStride = image.getPlanes()[2].getRowStride();
        int uvPixelStride = image.getPlanes()[2].getPixelStride();
        int uRowStride = image.getPlanes()[1].getRowStride();
        int uPixelStride = image.getPlanes()[1].getPixelStride();

        int chromaHeight = height / 2;
        int chromaWidth = width / 2;
        for (int row = 0; row < chromaHeight; row++) {
            for (int col = 0; col < chromaWidth; col++) {
                nv21[pos++] = vBuffer.get(row * uvRowStride + col * uvPixelStride);
                nv21[pos++] = uBuffer.get(row * uRowStride + col * uPixelStride);
            }
        }
        return nv21;
    }
}
