package com.northrise.campustrack.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.northrise.campustrack.databinding.ItemRiskBinding;
import com.northrise.campustrack.model.RiskRow;
import com.northrise.campustrack.util.AttendanceMath;

import java.util.ArrayList;
import java.util.List;

public class RiskAdapter extends RecyclerView.Adapter<RiskAdapter.Holder> {
    private List<RiskRow> items = new ArrayList<>();

    public void submit(List<RiskRow> items) {
        this.items = items == null ? new ArrayList<>() : items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemRiskBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        RiskRow row = items.get(position);
        holder.binding.nameText.setText(row.studentName);
        holder.binding.courseText.setText(row.courseCode + " · " + row.courseTitle);
        holder.binding.percentText.setText(AttendanceMath.format(AttendanceMath.percentage(row.present, row.late, row.absent)));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemRiskBinding binding;

        Holder(ItemRiskBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
