package com.novabank.novapay.presentation.customer.utility;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.novabank.novapay.R;
import com.novabank.novapay.core.util.CurrencyFormatter;
import com.novabank.novapay.databinding.ItemServiceCatalogBinding;
import com.novabank.novapay.domain.model.utility.ServiceItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter hiển thị danh mục dịch vụ Vé máy bay, Xem phim, Khách sạn & Mua sắm (service_catalog).
 */
public class ServiceCatalogAdapter extends RecyclerView.Adapter<ServiceCatalogAdapter.ViewHolder> {

    public interface OnServiceItemSelectListener {
        void onSelectItem(@NonNull ServiceItem item);
    }

    private final List<ServiceItem> items = new ArrayList<>();
    private final OnServiceItemSelectListener listener;

    public ServiceCatalogAdapter(@NonNull OnServiceItemSelectListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<ServiceItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemServiceCatalogBinding binding = ItemServiceCatalogBinding.inflate(
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
        private final ItemServiceCatalogBinding binding;

        ViewHolder(@NonNull ItemServiceCatalogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull ServiceItem item) {
            Context ctx = binding.getRoot().getContext();
            binding.tvServiceTitle.setText(item.getTitle());
            if (item.getDetails().isEmpty()) {
                binding.tvServiceDetails.setVisibility(View.GONE);
            } else {
                binding.tvServiceDetails.setVisibility(View.VISIBLE);
                binding.tvServiceDetails.setText(item.getDetails());
            }
            binding.tvServicePrice.setText(ctx.getString(
                    R.string.money_services_unit_price,
                    CurrencyFormatter.formatVnd(item.getPrice())
            ));
            binding.btnSelectService.setOnClickListener(v -> listener.onSelectItem(item));
            binding.getRoot().setOnClickListener(v -> listener.onSelectItem(item));
        }
    }
}
