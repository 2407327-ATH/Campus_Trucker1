package com.northrise.campustrack.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.northrise.campustrack.databinding.ItemHistoryBinding;
import com.northrise.campustrack.model.HistoryRow;
import com.northrise.campustrack.util.Dates;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {
    private List<HistoryRow> items = new ArrayList<>();

    public void submit(List<HistoryRow> items) {
        this.items = items == null ? new ArrayList<>() : items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        HistoryRow row = items.get(position);
        holder.binding.codeText.setText(row.courseCode);
        holder.binding.titleText.setText(row.courseTitle);
        holder.binding.statusText.setText(row.status.substring(0, 1) + row.status.substring(1).toLowerCase(Locale.US));
        holder.binding.statusText.setTextColor(Ui.statusColor(holder.itemView.getContext(), row.status));
        holder.binding.dateText.setText(Dates.format(row.markedAt));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemHistoryBinding binding;

        Holder(ItemHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
