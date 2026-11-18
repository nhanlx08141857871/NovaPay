package com.novabank.novapay.presentation.customer.transfer;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.novabank.novapay.R;
import com.novabank.novapay.databinding.ItemBeneficiaryCardBinding;
import com.novabank.novapay.domain.model.bank.Beneficiary;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter hiển thị danh sách người thụ hưởng đã lưu (users/{uid}/beneficiaries).
 */
public class BeneficiaryAdapter extends RecyclerView.Adapter<BeneficiaryAdapter.ViewHolder> {

    public interface Listener {
        void onSelectBeneficiary(@NonNull Beneficiary beneficiary);

        void onDeleteBeneficiary(@NonNull Beneficiary beneficiary);
    }

    private final List<Beneficiary> items = new ArrayList<>();
    private final Listener listener;

    public BeneficiaryAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<Beneficiary> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBeneficiaryCardBinding binding = ItemBeneficiaryCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemBeneficiaryCardBinding binding;

        ViewHolder(@NonNull ItemBeneficiaryCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Beneficiary item) {
            String displayTitle = !item.getAlias().isEmpty()
                    ? item.getAlias() + " (" + item.getName() + ")"
                    : item.getName();
            binding.tvBeneficiaryName.setText(displayTitle);
            binding.tvBeneficiaryAccount.setText(item.getAccountNumber());
            binding.tvBeneficiaryBank.setText(
                    item.isInternal()
                            ? binding.getRoot().getContext().getString(R.string.money_transfer_beneficiary_internal_tag)
                            : item.getBankCode()
            );

            binding.getRoot().setOnClickListener(v -> listener.onSelectBeneficiary(item));
            binding.btnDeleteBeneficiary.setOnClickListener(v -> listener.onDeleteBeneficiary(item));
        }
    }
}
