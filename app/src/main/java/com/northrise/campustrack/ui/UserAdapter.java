package com.northrise.campustrack.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.northrise.campustrack.databinding.ItemUserBinding;
import com.northrise.campustrack.model.Role;
import com.northrise.campustrack.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.Holder> {
    private List<User> items = new ArrayList<>();

    public void submit(List<User> items) {
        this.items = items == null ? new ArrayList<>() : items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemUserBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        User user = items.get(position);
        holder.binding.nameText.setText(user.name);
        holder.binding.emailText.setText(user.email);
        String extra = user.studentNumber == null || user.studentNumber.isEmpty() ? "" : " · " + user.studentNumber;
        holder.binding.roleText.setText(Role.label(user.role) + extra);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemUserBinding binding;

        Holder(ItemUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
