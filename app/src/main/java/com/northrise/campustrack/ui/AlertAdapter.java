package com.northrise.campustrack.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.northrise.campustrack.databinding.ItemAlertBinding;
import com.northrise.campustrack.model.Notification;
import com.northrise.campustrack.util.Dates;

import java.util.ArrayList;
import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.Holder> {
    public interface Listener {
        void onClick(Notification notification);
    }

    private final Listener listener;
    private List<Notification> items = new ArrayList<>();

    public AlertAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Notification> items) {
        this.items = items == null ? new ArrayList<>() : items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemAlertBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Notification notification = items.get(position);
        holder.binding.messageText.setText(notification.message);
        holder.binding.timeText.setText(Dates.format(notification.createdAt));
        holder.binding.unreadDot.setVisibility(notification.readAt == null ? View.VISIBLE : View.INVISIBLE);
        holder.itemView.setOnClickListener(v -> listener.onClick(notification));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemAlertBinding binding;

        Holder(ItemAlertBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
