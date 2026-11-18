package com.novabank.novapay.presentation.officer;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.databinding.ItemEkycReviewBinding;
import com.novabank.novapay.domain.model.audit.FaceCheckAudit;
import com.novabank.novapay.domain.model.officer.EkycReviewCandidate;
import com.novabank.novapay.domain.model.user.CustomerProfile;

import java.util.Objects;

public final class EkycReviewAdapter
        extends ListAdapter<EkycReviewCandidate, EkycReviewAdapter.ViewHolder> {

    public interface OnEkycActionListener {
        void onApprove(@NonNull EkycReviewCandidate item);
        void onReject(@NonNull EkycReviewCandidate item);
        void onCounterCapture(@NonNull EkycReviewCandidate item);
        void onImagePreview(@NonNull String base64Image);
    }

    private static final DiffUtil.ItemCallback<EkycReviewCandidate> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull EkycReviewCandidate oldItem,
                        @NonNull EkycReviewCandidate newItem
                ) {
                    return Objects.equals(oldItem.getUid(), newItem.getUid());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull EkycReviewCandidate oldItem,
                        @NonNull EkycReviewCandidate newItem
                ) {
                    return oldItem.equals(newItem);
                }
            };

    private final OnEkycActionListener listener;

    public EkycReviewAdapter(@NonNull OnEkycActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEkycReviewBinding binding = ItemEkycReviewBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemEkycReviewBinding binding;

        ViewHolder(ItemEkycReviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(EkycReviewCandidate item, OnEkycActionListener listener) {
            Context ctx = binding.getRoot().getContext();
            CustomerProfile customer = item.getCustomer();
            binding.tvEkycCustomerName.setText(customer.getFullName());
            binding.tvEkycCustomerInfo.setText(
                    customer.getEmail() + " • " + customer.getPhone() + " • " + customer.getNationalId()
            );

            String ekycStatus = customer.getEkycStatus();
            if (Constants.EKYC_STATUS_VERIFIED.equalsIgnoreCase(ekycStatus)) {
                binding.tvEkycStatusBadge.setText(R.string.officer_customer_ekyc_verified);
                binding.tvEkycStatusBadge.setTextColor(
                        ContextCompat.getColor(ctx, R.color.novapay_verified_badge)
                );
            } else if (Constants.EKYC_STATUS_PENDING.equalsIgnoreCase(ekycStatus)) {
                binding.tvEkycStatusBadge.setText(R.string.officer_customer_ekyc_pending);
                binding.tvEkycStatusBadge.setTextColor(
                        ContextCompat.getColor(ctx, R.color.novapay_warning_pending)
                );
            } else if (Constants.EKYC_STATUS_REJECTED.equalsIgnoreCase(ekycStatus)) {
                binding.tvEkycStatusBadge.setText(R.string.officer_customer_ekyc_rejected);
                binding.tvEkycStatusBadge.setTextColor(
                        ContextCompat.getColor(ctx, R.color.novapay_expense_negative)
                );
            } else {
                binding.tvEkycStatusBadge.setText(R.string.officer_customer_ekyc_none);
                binding.tvEkycStatusBadge.setTextColor(
                        ContextCompat.getColor(ctx, R.color.novapay_warning_pending)
                );
            }

            Bitmap ekycBmp = Base64ImageDecoder.decode(item.getEkycImageBase64(), 360);
            if (ekycBmp != null) {
                binding.ivEkycPhoto.setImageBitmap(ekycBmp);
                binding.tvEkycPhotoCaption.setText(R.string.officer_ekyc_photo_label);
                binding.ivEkycPhoto.setOnClickListener(v ->
                        listener.onImagePreview(item.getEkycImageBase64())
                );
            } else {
                binding.ivEkycPhoto.setImageResource(R.drawable.ic_modc_person_placeholder);
                binding.tvEkycPhotoCaption.setText(R.string.officer_ekyc_no_image);
                binding.ivEkycPhoto.setOnClickListener(null);
            }

            FaceCheckAudit manualAudit = item.getManualAuditItem();
            if (manualAudit != null) {
                binding.tvManualAuditNotice.setVisibility(View.VISIBLE);
                binding.layoutFaceCheckCompare.setVisibility(View.VISIBLE);
                Bitmap faceCheckBmp = Base64ImageDecoder.decode(manualAudit.getImageBase64(), 360);
                if (faceCheckBmp != null) {
                    binding.ivFaceCheckPhoto.setImageBitmap(faceCheckBmp);
                    binding.ivFaceCheckPhoto.setOnClickListener(v ->
                            listener.onImagePreview(manualAudit.getImageBase64())
                    );
                } else {
                    binding.ivFaceCheckPhoto.setImageResource(R.drawable.ic_modc_person_placeholder);
                    binding.ivFaceCheckPhoto.setOnClickListener(null);
                }
            } else {
                binding.tvManualAuditNotice.setVisibility(View.GONE);
                binding.layoutFaceCheckCompare.setVisibility(View.GONE);
            }

            boolean isPending = customer.isEkycPending();
            binding.layoutApproveRejectRow.setVisibility(isPending ? View.VISIBLE : View.GONE);
            binding.btnApproveEkyc.setEnabled(isPending);
            binding.btnRejectEkyc.setEnabled(isPending);
            binding.btnApproveEkyc.setOnClickListener(v -> listener.onApprove(item));
            binding.btnRejectEkyc.setOnClickListener(v -> listener.onReject(item));

            boolean showCounterCapture = !customer.isEkycVerified();
            binding.btnCounterCapture.setVisibility(showCounterCapture ? View.VISIBLE : View.GONE);
            binding.btnCounterCapture.setOnClickListener(v -> listener.onCounterCapture(item));
        }
    }
}
