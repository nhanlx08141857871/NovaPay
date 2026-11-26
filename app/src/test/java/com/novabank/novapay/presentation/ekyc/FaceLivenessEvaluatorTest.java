package com.novabank.novapay.presentation.ekyc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FaceLivenessEvaluatorTest {

    @Test
    public void evaluateMetrics_handlesAllLivenessBoundaries() {
        assertEquals(
                FaceLivenessEvaluator.LivenessVerdict.NO_FACE,
                FaceLivenessEvaluator.evaluateMetrics(0, 0.9f, 0.9f, 0f, 0f)
        );
        assertEquals(
                FaceLivenessEvaluator.LivenessVerdict.MULTIPLE_FACES,
                FaceLivenessEvaluator.evaluateMetrics(2, 0.9f, 0.9f, 0f, 0f)
        );
        assertEquals(
                FaceLivenessEvaluator.LivenessVerdict.LOOK_STRAIGHT,
                FaceLivenessEvaluator.evaluateMetrics(1, 0.9f, 0.9f, 15.1f, 0f)
        );
        assertEquals(
                FaceLivenessEvaluator.LivenessVerdict.LOOK_STRAIGHT,
                FaceLivenessEvaluator.evaluateMetrics(1, 0.9f, 0.9f, 0f, -15.1f)
        );
        assertEquals(
                FaceLivenessEvaluator.LivenessVerdict.OPEN_EYES,
                FaceLivenessEvaluator.evaluateMetrics(1, 0.69f, 0.9f, 5f, -5f)
        );
        assertEquals(
                FaceLivenessEvaluator.LivenessVerdict.OPEN_EYES,
                FaceLivenessEvaluator.evaluateMetrics(1, null, 0.9f, 0f, 0f)
        );
        assertEquals(
                FaceLivenessEvaluator.LivenessVerdict.READY,
                FaceLivenessEvaluator.evaluateMetrics(1, 0.7f, 0.7f, 15.0f, -15.0f)
        );
    }

    @Test
    public void faceCheckViewModel_isTerminalErrorCode_allowsRetryOnFaceMismatch() {
        assertFalse(FaceCheckViewModel.isTerminalErrorCode("FACE_MISMATCH"));
        assertFalse(FaceCheckViewModel.isTerminalErrorCode("NETWORK_ERROR"));
        assertFalse(FaceCheckViewModel.isTerminalErrorCode("EMAIL_SEND_FAILED"));
        assertFalse(FaceCheckViewModel.isTerminalErrorCode(null));

        assertTrue(FaceCheckViewModel.isTerminalErrorCode("TOO_MANY_ATTEMPTS"));
        assertTrue(FaceCheckViewModel.isTerminalErrorCode("DRAFT_EXPIRED"));
        assertTrue(FaceCheckViewModel.isTerminalErrorCode("EKYC_REQUIRED"));
        assertTrue(FaceCheckViewModel.isTerminalErrorCode("PERMISSION_DENIED"));
    }
}
