package com.novabank.novapay.presentation.customer.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.novabank.novapay.R;
import com.novabank.novapay.core.util.CurrencyFormatter;
import com.novabank.novapay.core.util.DateUtils;
import com.novabank.novapay.databinding.ItemLedgerEntryBinding;
import com.novabank.novapay.domain.model.transaction.LedgerEntry;

import java.util.ArrayList;
import java.util.List;

public class LedgerAdapter extends RecyclerView.Adapter<LedgerAdapter.LedgerViewHolder> {

    private final List<LedgerEntry> items = new ArrayList<>();

    public void submitList(@NonNull List<LedgerEntry> entries) {
        items.clear();
        items.addAll(entries);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LedgerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLedgerEntryBinding binding = ItemLedgerEntryBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new LedgerViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LedgerViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class LedgerViewHolder extends RecyclerView.ViewHolder {

        private final ItemLedgerEntryBinding binding;

        LedgerViewHolder(@NonNull ItemLedgerEntryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull LedgerEntry entry) {
            Context context = binding.getRoot().getContext();
            boolean isIn = entry.isInflow();

            binding.tvDescription.setText(entry.getDescription());
            binding.tvSignedAmount.setText(CurrencyFormatter.formatSignedVnd(entry.getAmount(), isIn));
            binding.tvSignedAmount.setTextColor(ContextCompat.getColor(
                    context,
                    isIn ? R.color.novapay_income_positive : R.color.novapay_expense_negative
            ));
            binding.tvCreatedAt.setText(DateUtils.formatDateTime(entry.getCreatedAt()));
            binding.tvBalanceAfter.setText(context.getString(
                    R.string.core_history_balance_after,
                    CurrencyFormatter.formatVnd(entry.getBalanceAfter())
            ));
        }
    }
}
