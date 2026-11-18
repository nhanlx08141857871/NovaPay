package com.novabank.novapay.presentation.officer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.novabank.novapay.R;
import com.novabank.novapay.core.util.DateUtils;
import com.novabank.novapay.databinding.ItemInterestRateBinding;
import com.novabank.novapay.domain.model.rate.InterestRate;

import java.util.Objects;

public final class InterestRateAdapter
        extends ListAdapter<InterestRate, InterestRateAdapter.ViewHolder> {

    public interface OnRateClickListener {
        void onRateClick(@NonNull InterestRate rate);
    }

    private static final DiffUtil.ItemCallback<InterestRate> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull InterestRate oldItem,
                        @NonNull InterestRate newItem
                ) {
                    return Objects.equals(oldItem.getRateId(), newItem.getRateId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull InterestRate oldItem,
                        @NonNull InterestRate newItem
                ) {
                    return oldItem.equals(newItem);
                }
            };

    private final OnRateClickListener clickListener;

    public InterestRateAdapter(@NonNull OnRateClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInterestRateBinding binding = ItemInterestRateBinding.inflate(
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
        private final ItemInterestRateBinding binding;

        ViewHolder(ItemInterestRateBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(InterestRate item, OnRateClickListener listener) {
            Context ctx = binding.getRoot().getContext();
            String termLabel = item.getTermMonths() == 0
                    ? ctx.getString(R.string.officer_rate_non_term)
                    : ctx.getString(R.string.officer_rate_term_months_format, item.getTermMonths());
            binding.tvRateTermLabel.setText(termLabel);
            binding.tvRateAnnualPercent.setText(
                    ctx.getString(R.string.officer_rate_percent_format, item.getAnnualRate())
            );
            binding.tvRateEffectiveFrom.setText(ctx.getString(
                    R.string.officer_rate_effective_format,
                    DateUtils.formatDate(item.getEffectiveFrom())
            ));
            binding.tvRateUpdatedBy.setText(ctx.getString(
                    R.string.officer_rate_updated_by_format,
                    item.getUpdatedBy()
            ));
            binding.getRoot().setOnClickListener(v -> listener.onRateClick(item));
        }
    }
}
