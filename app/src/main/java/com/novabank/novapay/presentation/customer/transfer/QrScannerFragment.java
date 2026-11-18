package com.novabank.novapay.presentation.customer.transfer;

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
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.novabank.novapay.R;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.core.util.QrHelper;
import com.novabank.novapay.databinding.FragmentQrScannerBinding;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Màn hình Quét mã QR chuyển khoản bằng CameraX + ML Kit Barcode Scanning.
 * Trả QrTransferPayload về TransferFragment qua FragmentResultListener.
 */
@AndroidEntryPoint
public class QrScannerFragment extends BaseFragment<FragmentQrScannerBinding> {

    public static final String REQ_QR_SCAN_RESULT = "req_novapay_qr_scan_result";
    public static final String KEY_QR_ACCOUNT_NUMBER = "qr_account_number";
    public static final String KEY_QR_BANK_CODE = "qr_bank_code";
    public static final String KEY_QR_RECIPIENT_NAME = "qr_recipient_name";
    public static final String KEY_QR_AMOUNT = "qr_amount";
    public static final String KEY_QR_NOTE = "qr_note";

    private QrScannerViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    togglePermissionRationale(false);
                    startCamera();
                } else {
                    togglePermissionRationale(true);
                }
            });

    @NonNull
    @Override
    protected FragmentQrScannerBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentQrScannerBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(QrScannerViewModel.class);
        FragmentQrScannerBinding b = getBinding();

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.resetReady()
        );

        cameraExecutor = Executors.newSingleThreadExecutor();
        barcodeScanner = BarcodeScanning.getClient(
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
        );

        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        b.btnGrantCameraPermission.setOnClickListener(v ->
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA));
        b.btnApplyManualQr.setOnClickListener(v -> {
            String raw = b.etManualQrPayload.getText() != null
                    ? b.etManualQrPayload.getText().toString()
                    : "";
            viewModel.onManualSubmit(raw);
        });

        if (hasCameraPermission()) {
            togglePermissionRationale(false);
            startCamera();
        } else {
            togglePermissionRationale(true);
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state != null) {
                stateLayoutHelper.render(state);
            }
        });

        viewModel.getInvalidQrEvent().observe(getViewLifecycleOwner(), invalid -> {
            if (Boolean.TRUE.equals(invalid)) {
                showSnackbar(getString(R.string.money_qr_invalid_payload));
            }
        });

        viewModel.getDecodedEvent().observe(getViewLifecycleOwner(), this::deliverQrPayloadAndFinish);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void togglePermissionRationale(boolean showRationale) {
        FragmentQrScannerBinding b = getBinding();
        b.layoutPermissionRationale.setVisibility(showRationale ? View.VISIBLE : View.GONE);
        b.previewViewQr.setVisibility(showRationale ? View.GONE : View.VISIBLE);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(requireContext());
        providerFuture.addListener(() -> {
            if (!isAdded()) {
                return;
            }
            try {
                ProcessCameraProvider cameraProvider = providerFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException ignored) {
                togglePermissionRationale(true);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @ExperimentalGetImage
    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        FragmentQrScannerBinding b = getBinding();
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(b.previewViewQr.getSurfaceProvider());

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        analysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

        CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;
        cameraProvider.unbindAll();
        try {
            cameraProvider.bindToLifecycle(getViewLifecycleOwner(), selector, preview, analysis);
        } catch (IllegalArgumentException ex) {
            // Fallback sang camera trước nếu máy ảo chỉ bật front camera
            try {
                cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(),
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        analysis
                );
            } catch (Exception ignored) {
                togglePermissionRationale(true);
            }
        }
    }

    @ExperimentalGetImage
    private void analyzeFrame(@NonNull ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null || barcodeScanner == null) {
            imageProxy.close();
            return;
        }
        InputImage inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.getImageInfo().getRotationDegrees()
        );
        barcodeScanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    if (!isAdded() || barcodes == null) {
                        return;
                    }
                    for (Barcode barcode : barcodes) {
                        String rawValue = barcode.getRawValue();
                        if (rawValue != null && !rawValue.isEmpty()) {
                            requireActivity().runOnUiThread(() -> viewModel.onCameraScanRaw(rawValue));
                            break;
                        }
                    }
                })
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void deliverQrPayloadAndFinish(@Nullable QrHelper.QrTransferPayload payload) {
        if (payload == null || !isAdded()) {
            return;
        }
        Bundle result = new Bundle();
        result.putString(KEY_QR_ACCOUNT_NUMBER, payload.getAccountNumber());
        if (payload.getBankCode() != null) {
            result.putString(KEY_QR_BANK_CODE, payload.getBankCode());
        }
        if (payload.getRecipientName() != null) {
            result.putString(KEY_QR_RECIPIENT_NAME, payload.getRecipientName());
        }
        result.putLong(KEY_QR_AMOUNT, payload.getAmount());
        if (payload.getNote() != null) {
            result.putString(KEY_QR_NOTE, payload.getNote());
        }
        getParentFragmentManager().setFragmentResult(REQ_QR_SCAN_RESULT, result);
        NavHostFragment.findNavController(this).popBackStack();
    }

    @Override
    public void onDestroyView() {
        if (barcodeScanner != null) {
            barcodeScanner.close();
            barcodeScanner = null;
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdownNow();
            cameraExecutor = null;
        }
        super.onDestroyView();
    }
}
