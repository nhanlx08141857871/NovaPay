package com.novabank.novapay.presentation.customer.transfer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.core.util.CurrencyFormatter;
import com.novabank.novapay.core.util.DateUtils;
import com.novabank.novapay.databinding.FragmentReceiptBinding;
import com.novabank.novapay.domain.model.transaction.ExternalRecipient;
import com.novabank.novapay.domain.model.transaction.Transaction;
import com.novabank.novapay.domain.model.transaction.TransactionMeta;
import com.novabank.novapay.domain.model.transaction.TransactionStatus;
import com.novabank.novapay.domain.model.transaction.TransactionType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Màn hình Biên lai giao dịch điện tử (E-Receipt):
 * Hiển thị thông tin giao dịch thành công và xuất ảnh PNG chia sẻ qua FileProvider Intent.
 */
@AndroidEntryPoint
public class ReceiptFragment extends BaseFragment<FragmentReceiptBinding> {

    public static final String ARG_TX_ID = "txId";

    private ReceiptViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private String txId;

    @NonNull
    @Override
    protected FragmentReceiptBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentReceiptBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(ReceiptViewModel.class);
        FragmentReceiptBinding b = getBinding();

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.loadReceipt(txId)
        );

        Bundle args = getArguments();
        if (args != null) {
            txId = args.getString(ARG_TX_ID);
            if (txId == null) {
                txId = args.getString(Constants.BUNDLE_TX_ID);
            }
        }

        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        b.btnBackHome.setOnClickListener(v -> {
            try {
                navigateDeepLink(Constants.DEEPLINK_HOME);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                NavHostFragment.findNavController(this).popBackStack();
            }
        });
        b.btnSharePng.setOnClickListener(v -> exportAndShareReceiptPng());

        viewModel.loadReceipt(txId);
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            stateLayoutHelper.render(state);
            ReceiptUiState data = state.getData();
            if (data != null) {
                renderTransaction(data.getTransaction());
            }
        });
    }

    private void renderTransaction(@NonNull Transaction tx) {
        FragmentReceiptBinding b = getBinding();
        boolean success = tx.getStatus() == TransactionStatus.SUCCESS;
        b.tvReceiptStatus.setText(success
                ? R.string.money_receipt_status_success
                : R.string.money_receipt_status_failed);
        b.tvReceiptStatus.setTextColor(ContextCompat.getColor(
                requireContext(),
                success ? R.color.novapay_income_positive : R.color.novapay_expense_negative
        ));

        b.tvReceiptAmount.setText(CurrencyFormatter.formatVnd(tx.getAmount()));
        b.tvReceiptTxId.setText(tx.getTxId());
        b.tvReceiptTime.setText(tx.getCreatedAt() != null
                ? DateUtils.formatDateTime(tx.getCreatedAt())
                : "-");
        b.tvReceiptType.setText(resolveTypeLabel(tx.getType()));
        b.tvReceiptFromAccount.setText(tx.getFromAccountId() != null ? tx.getFromAccountId() : "-");
        b.tvReceiptTarget.setText(resolveTargetSummary(tx));
        b.tvReceiptFee.setText(tx.getFee() <= 0L
                ? getString(R.string.money_receipt_free_fee)
                : CurrencyFormatter.formatVnd(tx.getFee()));
        b.tvReceiptNote.setText(!tx.getNote().isEmpty() ? tx.getNote() : "-");
        b.tvReceiptSecurity.setText(tx.isFaceChecked()
                ? R.string.money_receipt_security_3layer
                : R.string.money_receipt_security_otp);
    }

    @NonNull
    private String resolveTypeLabel(@NonNull TransactionType type) {
        return switch (type) {
            case TRANSFER_INTERNAL -> getString(R.string.money_tx_type_internal);
            case TRANSFER_INTERBANK -> getString(R.string.money_tx_type_interbank);
            case GATEWAY -> getString(R.string.money_tx_type_gateway);
            case DEPOSIT -> getString(R.string.money_tx_type_deposit);
            case WITHDRAW -> getString(R.string.money_tx_type_withdraw);
            case BILL -> getString(R.string.money_tx_type_bill);
            case TOPUP -> getString(R.string.money_tx_type_topup);
            case SERVICE_PAYMENT -> getString(R.string.money_tx_type_service);
            case MORTGAGE_PAYMENT -> getString(R.string.money_tx_type_mortgage);
        };
    }

    @NonNull
    private String resolveTargetSummary(@NonNull Transaction tx) {
        ExternalRecipient ext = tx.getToExternal();
        if (ext != null && !ext.getAccountNumber().isEmpty()) {
            return ext.getName() + " • " + ext.getBankCode() + " (" + ext.getAccountNumber() + ")";
        }
        if (tx.getToAccountId() != null && !tx.getToAccountId().isEmpty()) {
            return tx.getToAccountId();
        }
        TransactionMeta meta = tx.getMeta();
        if (meta != null) {
            if (meta.getBillCode() != null) {
                return meta.getBillCode();
            }
            if (meta.getBillId() != null) {
                return meta.getBillId();
            }
            if (meta.getPhone() != null) {
                return meta.getPhone();
            }
            if (meta.getMortgageAccountId() != null) {
                return meta.getMortgageAccountId();
            }
            if (meta.getItemId() != null) {
                return meta.getOrderId() != null
                        ? meta.getItemId() + " (" + meta.getOrderId() + ")"
                        : meta.getItemId();
            }
            if (meta.getGatewayRef() != null) {
                return "Stripe • " + meta.getGatewayRef();
            }
        }
        return tx.getFromAccountId() != null ? tx.getFromAccountId() : "-";
    }

    private void exportAndShareReceiptPng() {
        View captureView = getBinding().cardReceiptCapture;
        int width = captureView.getWidth();
        int height = captureView.getHeight();
        if (width <= 0 || height <= 0) {
            showSnackbar(getString(R.string.money_receipt_export_failed));
            return;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        captureView.draw(canvas);

        File receiptsDir = new File(requireContext().getCacheDir(), "receipts");
        if (!receiptsDir.exists() && !receiptsDir.mkdirs()) {
            bitmap.recycle();
            showSnackbar(getString(R.string.money_receipt_export_failed));
            return;
        }

        String safeId = (txId != null && !txId.isEmpty())
                ? txId.replaceAll("[^A-Za-z0-9_-]", "_")
                : String.valueOf(System.currentTimeMillis());
        File pngFile = new File(receiptsDir, "novapay_receipt_" + safeId + ".png");

        try (FileOutputStream fos = new FileOutputStream(pngFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
        } catch (IOException ex) {
            bitmap.recycle();
            showSnackbar(getString(R.string.money_receipt_export_failed));
            return;
        } finally {
            bitmap.recycle();
        }

        Uri contentUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                pngFile
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/png");
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, getString(R.string.money_receipt_share_chooser)));
    }
}
