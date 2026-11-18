package com.novabank.novapay.core.ui;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.databinding.ViewEmptyStateBinding;
import com.novabank.novapay.databinding.ViewErrorStateBinding;
import com.novabank.novapay.databinding.ViewLoadingStateBinding;

/**
 * Hỗ trợ đồng bộ hiển thị 4 trạng thái bắt buộc (LOADING, SUCCESS, EMPTY, ERROR)
 * trên mọi màn hình MVVM của NovaPay.
 */
public final class StateLayoutHelper {

    private final View contentView;
    private final ViewLoadingStateBinding loadingBinding;
    private final ViewEmptyStateBinding emptyBinding;
    private final ViewErrorStateBinding errorBinding;

    public StateLayoutHelper(
            @NonNull View contentView,
            @NonNull ViewLoadingStateBinding loadingBinding,
            @NonNull ViewEmptyStateBinding emptyBinding,
            @NonNull ViewErrorStateBinding errorBinding,
            @Nullable Runnable onRetryAction
    ) {
        this.contentView = contentView;
        this.loadingBinding = loadingBinding;
        this.emptyBinding = emptyBinding;
        this.errorBinding = errorBinding;

        if (onRetryAction != null) {
            this.errorBinding.btnRetry.setVisibility(View.VISIBLE);
            this.errorBinding.btnRetry.setOnClickListener(v -> onRetryAction.run());
        } else {
            this.errorBinding.btnRetry.setVisibility(View.GONE);
        }
    }

    public <T> void render(@NonNull UiState<T> state) {
        UiState.Status status = state.getStatus();
        contentView.setVisibility(status == UiState.Status.SUCCESS ? View.VISIBLE : View.GONE);
        loadingBinding.getRoot().setVisibility(status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        emptyBinding.getRoot().setVisibility(status == UiState.Status.EMPTY ? View.VISIBLE : View.GONE);
        errorBinding.getRoot().setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);

        if (status == UiState.Status.ERROR) {
            String msg = state.getErrorMessage();
            if (msg != null && !msg.trim().isEmpty()) {
                errorBinding.tvErrorMessage.setText(msg);
            } else {
                errorBinding.tvErrorMessage.setText(R.string.state_error_default_message);
            }
        }
    }
}
