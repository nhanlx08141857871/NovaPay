package com.novabank.novapay.presentation.ekyc;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.android.material.snackbar.Snackbar;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.core.util.ImageCompressor;
import com.novabank.novapay.databinding.DialogFaceCheckBinding;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Dialog xác thực khuôn mặt cho giao dịch chi/rút > 5.000.000 VNĐ (SPEC_CHOT.md Q1, Q2, Q4).
 * Trả kết quả về phân hệ gọi (B) qua FragmentResultListener với khóa Constants.REQ_FACE_CHECK.
 */
@AndroidEntryPoint
public class FaceCheckDialogFragment extends AppCompatDialogFragment {

    private DialogFaceCheckBinding binding;
    private FaceCheckViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private String draftId;

    private ExecutorService cameraExecutor;
    private FaceDetector faceDetector;
    private ImageCapture imageCapture;
    private volatile boolean isCapturing = false;
    private boolean resultDelivered = false;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    showPermissionUi(false);
                    startCamera();
                } else {
                    showPermissionUi(true);
                }
            });

    public static FaceCheckDialogFragment newInstance(@NonNull String draftId) {
        FaceCheckDialogFragment fragment = new FaceCheckDialogFragment();
        Bundle args = new Bundle();
        args.putString(Constants.BUNDLE_DRAFT_ID, draftId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = DialogFaceCheckBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            Window window = getDialog().getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FaceCheckViewModel.class);

        Bundle args = getArguments();
        if (args != null) {
            draftId = args.getString(Constants.BUNDLE_DRAFT_ID);
            if (draftId == null) {
                draftId = args.getString("draftId");
            }
        }

        binding.stateLoading.tvLoadingMessage.setText(R.string.core_face_check_verifying);
        stateLayoutHelper = new StateLayoutHelper(
                binding.contentContainer,
                binding.stateLoading,
                binding.stateEmpty,
                binding.stateError,
                () -> {
                    isCapturing = false;
                    viewModel.resetReady();
                }
        );

        cameraExecutor = Executors.newSingleThreadExecutor();
        faceDetector = FaceDetection.getClient(
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build()
        );

        binding.btnCancel.setOnClickListener(v -> {
            deliverResult(false, 0L);
            dismiss();
        });
        binding.btnRequestCameraPermission.setOnClickListener(v ->
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA));
        binding.btnVerifyFace.setOnClickListener(v -> captureAndVerify());

        observeViewModel();

        if (hasCameraPermission()) {
            showPermissionUi(false);
            startCamera();
        } else {
            showPermissionUi(true);
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || binding == null) {
                return;
            }
            stateLayoutHelper.render(state);
            FaceCheckUiState data = state.getData();
            if (data != null) {
                binding.tvLivenessGuide.setText(data.getGuideMessageRes());
                binding.tvLivenessGuide.setTextColor(ContextCompat.getColor(
                        requireContext(),
                        data.isLivenessPassed()
                                ? R.color.novapay_income_positive
                                : R.color.novapay_warning_pending
                ));
                binding.btnVerifyFace.setEnabled(data.isLivenessPassed() && !isCapturing);
            }
        });

        viewModel.getFaceCheckPassedEvent().observe(getViewLifecycleOwner(), otpExpiresAtEpochMs -> {
            isCapturing = false;
            long expiresAt = otpExpiresAtEpochMs != null ? otpExpiresAtEpochMs : 0L;
            deliverResult(true, expiresAt);
            dismiss();
        });
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void showPermissionUi(boolean showRationale) {
        if (binding == null) {
            return;
        }
        binding.layoutPermissionRationale.setVisibility(showRationale ? View.VISIBLE : View.GONE);
        binding.previewView.setVisibility(showRationale ? View.GONE : View.VISIBLE);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            if (!isAdded() || binding == null) {
                return;
            }
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException ex) {
                showMessage(getString(R.string.core_liveness_capture_failed));
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @androidx.camera.core.ExperimentalGetImage
    private void bindUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        if (binding == null) {
            return;
        }
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImageProxy);

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
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

    private void captureAndVerify() {
        if (imageCapture == null || isCapturing) {
            return;
        }
        isCapturing = true;
        if (binding != null) {
            binding.btnVerifyFace.setEnabled(false);
        }

        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                String base64;
                try {
                    base64 = ImageCompressor.compressImageProxyToBase64(image);
                } catch (Exception ex) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            isCapturing = false;
                            showMessage(getString(R.string.core_liveness_capture_failed));
                            viewModel.resetReady();
                        });
                    }
                    return;
                } finally {
                    image.close();
                }
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> viewModel.verifyFace(draftId, base64));
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        isCapturing = false;
                        showMessage(getString(R.string.core_liveness_capture_failed));
                        viewModel.resetReady();
                    });
                }
            }
        });
    }

    private void deliverResult(boolean passed, long otpExpiresAtEpochMs) {
        if (resultDelivered || !isAdded()) {
            return;
        }
        resultDelivered = true;
        Bundle bundle = new Bundle();
        bundle.putString(Constants.BUNDLE_DRAFT_ID, draftId != null ? draftId : "");
        bundle.putBoolean(Constants.BUNDLE_FACE_CHECK_PASSED, passed);
        bundle.putLong(Constants.BUNDLE_OTP_EXPIRES_AT_EPOCH_MS, otpExpiresAtEpochMs);
        getParentFragmentManager().setFragmentResult(Constants.REQ_FACE_CHECK, bundle);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        deliverResult(false, 0L);
        super.onDismiss(dialog);
    }

    private void showMessage(@NonNull String message) {
        if (binding != null && !message.isEmpty()) {
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
        }
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
        binding = null;
        super.onDestroyView();
    }
}
