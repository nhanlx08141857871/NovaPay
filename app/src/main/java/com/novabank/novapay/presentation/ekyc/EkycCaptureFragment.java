package com.novabank.novapay.presentation.ekyc;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.core.util.ImageCompressor;
import com.novabank.novapay.databinding.FragmentEkycCaptureBinding;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Màn hình chụp ảnh định danh khuôn mặt eKYC (novapay://ekyc & ?targetUid={targetUid}).
 * Sử dụng CameraX + ML Kit Face Detection (PERFORMANCE_MODE_ACCURATE + CLASSIFICATION_MODE_ALL),
 * nén ảnh JPEG < 200KB và gửi POST /ekyc/submit.
 */
@AndroidEntryPoint
public class EkycCaptureFragment extends BaseFragment<FragmentEkycCaptureBinding> {

    private EkycCaptureViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private String targetUid;

    private ExecutorService cameraExecutor;
    private FaceDetector faceDetector;
    private ImageCapture imageCapture;
    private int lensFacing = CameraSelector.LENS_FACING_FRONT;
    private volatile boolean isCapturing = false;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    showPermissionUi(false);
                    startCamera();
                } else {
                    showPermissionUi(true);
                }
            });

    @NonNull
    @Override
    protected FragmentEkycCaptureBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentEkycCaptureBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(EkycCaptureViewModel.class);
        FragmentEkycCaptureBinding b = getBinding();

        b.stateLoading.tvLoadingMessage.setText(R.string.core_ekyc_uploading);
        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> {
                    isCapturing = false;
                    viewModel.resetCameraReady();
                }
        );

        Bundle args = getArguments();
        if (args != null) {
            targetUid = args.getString("targetUid");
            if (targetUid == null) {
                targetUid = args.getString(Constants.BUNDLE_TARGET_UID);
            }
        }
        if (targetUid != null && !targetUid.trim().isEmpty()) {
            b.tvSubtitle.setText(R.string.core_ekyc_counter_subtitle);
            lensFacing = CameraSelector.LENS_FACING_BACK;
        } else {
            b.tvSubtitle.setText(R.string.core_ekyc_self_subtitle);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
        faceDetector = FaceDetection.getClient(
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build()
        );

        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        b.btnRequestCameraPermission.setOnClickListener(v ->
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA));
        b.btnSwitchCamera.setOnClickListener(v -> {
            lensFacing = (lensFacing == CameraSelector.LENS_FACING_FRONT)
                    ? CameraSelector.LENS_FACING_BACK
                    : CameraSelector.LENS_FACING_FRONT;
            if (hasCameraPermission()) {
                startCamera();
            }
        });
        b.btnCaptureEkyc.setOnClickListener(v -> captureAndUpload());

        if (hasCameraPermission()) {
            showPermissionUi(false);
            startCamera();
        } else {
            showPermissionUi(true);
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            stateLayoutHelper.render(state);
            EkycCaptureUiState data = state.getData();
            if (data != null) {
                FragmentEkycCaptureBinding b = getBinding();
                b.tvLivenessGuide.setText(data.getGuideMessageRes());
                b.tvLivenessGuide.setTextColor(ContextCompat.getColor(
                        requireContext(),
                        data.isLivenessPassed()
                                ? R.color.novapay_income_positive
                                : R.color.novapay_warning_pending
                ));
                b.btnCaptureEkyc.setEnabled(data.isLivenessPassed() && !isCapturing);
            }
        });

        viewModel.getSubmitSuccessEvent().observe(getViewLifecycleOwner(), status -> {
            isCapturing = false;
            showSnackbar(getString(R.string.core_ekyc_submit_success));
            Bundle result = new Bundle();
            if (targetUid != null && !targetUid.trim().isEmpty()) {
                result.putString(Constants.BUNDLE_TARGET_UID, targetUid.trim());
            }
            getParentFragmentManager().setFragmentResult(Constants.REQ_EKYC_CAPTURE, result);
            NavHostFragment.findNavController(this).popBackStack();
        });
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void showPermissionUi(boolean showRationale) {
        FragmentEkycCaptureBinding b = getBinding();
        b.layoutPermissionRationale.setVisibility(showRationale ? View.VISIBLE : View.GONE);
        b.previewView.setVisibility(showRationale ? View.GONE : View.VISIBLE);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            if (!isAdded()) {
                return;
            }
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException ex) {
                showSnackbar(getString(R.string.core_liveness_capture_failed));
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @androidx.camera.core.ExperimentalGetImage
    private void bindUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        FragmentEkycCaptureBinding b = getBinding();
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(b.previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImageProxy);

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(
                getViewLifecycleOwner(),
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
        );
    }

    @ExperimentalGetImage
    private void analyzeImageProxy(@NonNull ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }
        InputImage inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.getImageInfo().getRotationDegrees()
        );
        faceDetector.process(inputImage)
                .addOnSuccessListener(faces -> {
                    if (isAdded()) {
                        FaceLivenessEvaluator.LivenessVerdict verdict =
                                FaceLivenessEvaluator.evaluateFaces(faces);
                        viewModel.onLivenessUpdated(verdict);
                    }
                })
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void captureAndUpload() {
        if (imageCapture == null || isCapturing) {
            return;
        }
        isCapturing = true;
        getBinding().btnCaptureEkyc.setEnabled(false);

        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                String base64;
                try {
                    base64 = ImageCompressor.compressImageProxyToBase64(image);
                } catch (Exception ex) {
                    requireActivity().runOnUiThread(() -> {
                        isCapturing = false;
                        showSnackbar(getString(R.string.core_liveness_capture_failed));
                        viewModel.resetCameraReady();
                    });
                    return;
                } finally {
                    image.close();
                }
                requireActivity().runOnUiThread(() ->
                        viewModel.submitCapturedBase64(base64, targetUid));
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        isCapturing = false;
                        showSnackbar(getString(R.string.core_liveness_capture_failed));
                        viewModel.resetCameraReady();
                    });
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        if (faceDetector != null) {
            faceDetector.close();
            faceDetector = null;
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdownNow();
            cameraExecutor = null;
        }
        imageCapture = null;
        super.onDestroyView();
    }
}
