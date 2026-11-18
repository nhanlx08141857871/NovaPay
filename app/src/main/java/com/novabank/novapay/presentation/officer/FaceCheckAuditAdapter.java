package com.novabank.novapay.presentation.officer;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.novabank.novapay.R;
import com.novabank.novapay.core.util.DateUtils;
import com.novabank.novapay.databinding.ItemFaceCheckAuditBinding;
import com.novabank.novapay.domain.model.audit.FaceCheckAudit;

import java.util.Objects;

public final class FaceCheckAuditAdapter
        extends ListAdapter<FaceCheckAudit, FaceCheckAuditAdapter.ViewHolder> {

    public interface OnFaceAuditClickListener {
        void onFaceAuditClick(@NonNull FaceCheckAudit item);
    }

    private static final DiffUtil.ItemCallback<FaceCheckAudit> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull FaceCheckAudit oldItem, @NonNull FaceCheckAudit newItem) {
                    return Objects.equals(oldItem.getDraftId(), newItem.getDraftId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull FaceCheckAudit oldItem, @NonNull FaceCheckAudit newItem) {
                    return oldItem.equals(newItem);
                }
            };

    private final OnFaceAuditClickListener clickListener;

    public FaceCheckAuditAdapter(@NonNull OnFaceAuditClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFaceCheckAuditBinding binding = ItemFaceCheckAuditBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), clickListener);
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemFaceCheckAuditBinding binding;

        ViewHolder(ItemFaceCheckAuditBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FaceCheckAudit item, OnFaceAuditClickListener listener) {
            binding.tvFaceAuditUid.setText(binding.getRoot().getContext().getString(
                    R.string.officer_post_audit_item_uid,
                    item.getUid()
            ));
            String txText = item.getTxId() != null && !item.getTxId().isEmpty()
                    ? binding.getRoot().getContext().getString(R.string.officer_post_audit_item_tx, item.getTxId())
                    : binding.getRoot().getContext().getString(R.string.officer_post_audit_item_tx_pending);
            binding.tvFaceAuditTxId.setText(txText);
            binding.tvFaceAuditTime.setText(DateUtils.formatDateTime(item.getCreatedAt()));

            Bitmap bmp = Base64ImageDecoder.decode(item.getImageBase64(), 256);
            if (bmp != null) {
                binding.ivFaceAuditThumb.setImageBitmap(bmp);
            } else {
                binding.ivFaceAuditThumb.setImageResource(R.drawable.ic_modc_person_placeholder);
            }

            binding.getRoot().setOnClickListener(v -> listener.onFaceAuditClick(item));
        }
    }
}
