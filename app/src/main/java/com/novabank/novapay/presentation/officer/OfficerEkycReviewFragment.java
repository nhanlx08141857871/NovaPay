package com.novabank.novapay.presentation.officer;

import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.databinding.FragmentOfficerEkycReviewBinding;
import com.novabank.novapay.domain.model.officer.EkycReviewCandidate;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class OfficerEkycReviewFragment extends BaseFragment<FragmentOfficerEkycReviewBinding> {

    private OfficerEkycReviewViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private EkycReviewAdapter adapter;
    @Nullable
    private String focusUid;

    @NonNull
    @Override
    protected FragmentOfficerEkycReviewBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentOfficerEkycReviewBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(OfficerEkycReviewViewModel.class);
        FragmentOfficerEkycReviewBinding b = getBinding();

        if (getArguments() != null) {
            focusUid = getArguments().getString(OfficerNavConstants.ARG_FOCUS_UID);
        }

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.loadPendingEkyc()
        );
        b.stateEmpty.tvEmptyTitle.setText(R.string.officer_ekyc_empty_title);
        b.stateEmpty.tvEmptySubtitle.setText(R.string.officer_ekyc_empty_subtitle);

        adapter = new EkycReviewAdapter(new EkycReviewAdapter.OnEkycActionListener() {
            @Override
            public void onApprove(@NonNull EkycReviewCandidate item) {
                confirmReviewDecision(item, Constants.EKYC_STATUS_VERIFIED);
            }

            @Override
            public void onReject(@NonNull EkycReviewCandidate item) {
                confirmReviewDecision(item, Constants.EKYC_STATUS_REJECTED);
            }

            @Override
            public void onCounterCapture(@NonNull EkycReviewCandidate item) {
                launchCounterCapture(item.getUid());
            }

            @Override
            public void onImagePreview(@NonNull String base64Image) {
                showZoomedImageDialog(base64Image);
            }
        });
        b.contentContainer.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.contentContainer.setAdapter(adapter);

        b.toolbarEkycReview.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack()
        );
        b.toolbarEkycReview.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_refresh) {
                viewModel.loadPendingEkyc();
                return true;
            }
            return false;
        });

        b.chipGroupEkycFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipPendingOnly)) {
                viewModel.setFilterMode(OfficerEkycReviewUiState.FilterMode.PENDING_ONLY);
            } else if (checkedIds.contains(R.id.chipAllUnverified)) {
                viewModel.setFilterMode(OfficerEkycReviewUiState.FilterMode.ALL_UNVERIFIED);
            } else if (checkedIds.contains(R.id.chipManualFaceAudit)) {
                viewModel.setFilterMode(OfficerEkycReviewUiState.FilterMode.MANUAL_FACE_AUDIT);
            }
        });

        if (focusUid != null && !focusUid.trim().isEmpty()) {
            b.chipManualFaceAudit.setChecked(true);
            viewModel.setFilterMode(OfficerEkycReviewUiState.FilterMode.MANUAL_FACE_AUDIT);
        }

        getParentFragmentManager().setFragmentResultListener(
                Constants.REQ_EKYC_CAPTURE,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    showSnackbar(getString(R.string.officer_ekyc_capture_returned));
                    viewModel.loadPendingEkyc();
                }
        );
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || !isAdded() || getView() == null) {
                return;
            }
            stateLayoutHelper.render(state);
            if (state.getStatus() == UiState.Status.SUCCESS && state.getData() != null) {
                List<EkycReviewCandidate> candidates = state.getData().getCandidates();
                adapter.submitList(candidates, () -> scrollToFocusedUidIfNeeded(candidates));
            }
        });

        viewModel.getReviewInProgress().observe(getViewLifecycleOwner(), inProgress -> {
            if (isAdded() && getView() != null) {
                getBinding().progressEkycAction.setVisibility(
                        Boolean.TRUE.equals(inProgress) ? View.VISIBLE : View.GONE
                );
            }
        });

        viewModel.getFeedbackMessageEvent().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                showSnackbar(msg);
            }
        });

        viewModel.getFeedbackResEvent().observe(getViewLifecycleOwner(), resId -> {
            if (resId != null) {
                showSnackbar(getString(resId));
            }
        });
    }

    private void scrollToFocusedUidIfNeeded(@NonNull List<EkycReviewCandidate> candidates) {
        if (focusUid == null || !isAdded() || getView() == null) {
            return;
        }
        for (int i = 0; i < candidates.size(); i++) {
            if (focusUid.equals(candidates.get(i).getUid())) {
                getBinding().contentContainer.scrollToPosition(i);
                focusUid = null;
                break;
            }
        }
    }

    private void launchCounterCapture(@NonNull String targetUid) {
        try {
            Uri uri = Uri.parse(Constants.DEEPLINK_EKYC + "?targetUid=" + Uri.encode(targetUid));
            NavHostFragment.findNavController(this).navigate(uri);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showSnackbar(getString(R.string.officer_ekyc_capture_unavailable));
        }
    }

    private void confirmReviewDecision(@NonNull EkycReviewCandidate item, @NonNull String decision) {
        boolean approve = Constants.EKYC_STATUS_VERIFIED.equals(decision);
        int titleRes = approve
                ? R.string.officer_ekyc_approve_confirm_title
                : R.string.officer_ekyc_reject_confirm_title;
        int msgRes = approve
                ? R.string.officer_ekyc_approve_confirm_message
                : R.string.officer_ekyc_reject_confirm_message;
        int positiveRes = approve
                ? R.string.officer_ekyc_approve_action
                : R.string.officer_ekyc_reject_action;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleRes)
                .setMessage(getString(msgRes, item.getCustomer().getFullName()))
                .setNegativeButton(R.string.officer_action_cancel, null)
                .setPositiveButton(positiveRes, (dialog, which) ->
                        viewModel.reviewEkyc(
                                item.getUid(),
                                decision,
                                R.string.officer_ekyc_review_approved,
                                R.string.officer_ekyc_review_rejected
                        )
                )
                .show();
    }

    private void showZoomedImageDialog(@NonNull String base64Image) {
        Bitmap bmp = Base64ImageDecoder.decode(base64Image, 1024);
        if (bmp == null) {
            return;
        }
        ImageView imageView = new ImageView(requireContext());
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        imageView.setPadding(pad, pad, pad, pad);
        imageView.setImageBitmap(bmp);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.officer_ekyc_zoom_title)
                .setView(imageView)
                .setPositiveButton(R.string.officer_action_close, null)
                .show();
    }
}
