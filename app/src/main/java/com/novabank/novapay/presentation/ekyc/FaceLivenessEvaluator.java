package com.novabank.novapay.presentation.ekyc;

import com.google.mlkit.vision.face.Face;
import com.novabank.novapay.R;

import java.util.List;

/**
 * Đánh giá tiêu chí Liveness chuẩn SPEC_CHOT.md Q2:
 * - Đúng 1 khuôn mặt trong khung hình.
 * - Mở cả hai mắt (leftEyeOpenProbability >= 0.7f && rightEyeOpenProbability >= 0.7f).
 * - Nhìn thẳng chính diện (|EulerY| <= 15f && |EulerZ| <= 15f).
 */
public final class FaceLivenessEvaluator {

    public static final float MIN_EYE_OPEN_PROBABILITY = 0.7f;
    public static final float MAX_HEAD_ANGLE_DEGREES = 15.0f;

    public enum LivenessVerdict {
        READY,
        NO_FACE,
        MULTIPLE_FACES,
        LOOK_STRAIGHT,
        OPEN_EYES
    }

    private FaceLivenessEvaluator() {
    }

    public static LivenessVerdict evaluateFaces(List<Face> faces) {
        if (faces == null || faces.isEmpty()) {
            return LivenessVerdict.NO_FACE;
        }
        if (faces.size() > 1) {
            return LivenessVerdict.MULTIPLE_FACES;
        }
        Face face = faces.get(0);
        return evaluateMetrics(
                1,
                face.getLeftEyeOpenProbability(),
                face.getRightEyeOpenProbability(),
                face.getHeadEulerAngleY(),
                face.getHeadEulerAngleZ()
        );
    }

    public static LivenessVerdict evaluateMetrics(
            int faceCount,
            Float leftEyeOpenProbability,
            Float rightEyeOpenProbability,
            float headEulerAngleY,
            float headEulerAngleZ
    ) {
        if (faceCount <= 0) {
            return LivenessVerdict.NO_FACE;
        }
        if (faceCount > 1) {
            return LivenessVerdict.MULTIPLE_FACES;
        }
        if (Math.abs(headEulerAngleY) > MAX_HEAD_ANGLE_DEGREES
                || Math.abs(headEulerAngleZ) > MAX_HEAD_ANGLE_DEGREES) {
            return LivenessVerdict.LOOK_STRAIGHT;
        }
        if (leftEyeOpenProbability == null
                || rightEyeOpenProbability == null
                || leftEyeOpenProbability < MIN_EYE_OPEN_PROBABILITY
                || rightEyeOpenProbability < MIN_EYE_OPEN_PROBABILITY) {
            return LivenessVerdict.OPEN_EYES;
        }
        return LivenessVerdict.READY;
    }

    public static int toGuideMessageRes(LivenessVerdict verdict) {
        if (verdict == LivenessVerdict.READY) {
            return R.string.core_liveness_ready;
        }
        if (verdict == LivenessVerdict.MULTIPLE_FACES) {
            return R.string.core_liveness_multiple_faces;
        }
        if (verdict == LivenessVerdict.LOOK_STRAIGHT) {
            return R.string.core_liveness_look_straight;
        }
        if (verdict == LivenessVerdict.OPEN_EYES) {
            return R.string.core_liveness_open_eyes;
        }
        return R.string.core_liveness_no_face;
    }
}
