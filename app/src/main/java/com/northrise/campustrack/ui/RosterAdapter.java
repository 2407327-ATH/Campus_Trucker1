package com.northrise.campustrack.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.northrise.campustrack.R;
import com.northrise.campustrack.databinding.ItemRosterBinding;
import com.northrise.campustrack.model.RosterStudent;
import com.northrise.campustrack.util.AttendanceMath;

import java.util.ArrayList;
import java.util.List;

public class RosterAdapter extends RecyclerView.Adapter<RosterAdapter.Holder> {
    public interface Listener {
        void onStatus(RosterStudent student, String status);
    }

    private final Listener listener;
    private List<RosterStudent> items = new ArrayList<>();
    private boolean sessionOpen;

    public RosterAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<RosterStudent> items, boolean sessionOpen) {
        this.items = items == null ? new ArrayList<>() : items;
        this.sessionOpen = sessionOpen;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemRosterBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        RosterStudent student = items.get(position);
        holder.binding.nameText.setText(student.name);
        String number = student.studentNumber == null || student.studentNumber.isEmpty() ? "" : " · " + student.studentNumber;
        holder.binding.metaText.setText(student.email + number);
        holder.binding.percentText.setText(AttendanceMath.format(AttendanceMath.percentage(student.present, student.late, student.absent)));
        holder.binding.percentText.setTextColor(Ui.percentColor(holder.itemView.getContext(), student.present, student.late, student.absent));
        style(holder.binding.presentButton, "PRESENT".equals(student.sessionStatus), holder.itemView.getContext().getColor(R.color.present));
        style(holder.binding.lateButton, "LATE".equals(student.sessionStatus), holder.itemView.getContext().getColor(R.color.late));
        style(holder.binding.absentButton, "ABSENT".equals(student.sessionStatus), holder.itemView.getContext().getColor(R.color.absent));
        holder.binding.presentButton.setEnabled(sessionOpen);
        holder.binding.lateButton.setEnabled(sessionOpen);
        holder.binding.absentButton.setEnabled(sessionOpen);
        holder.binding.presentButton.setOnClickListener(v -> listener.onStatus(student, "PRESENT"));
        holder.binding.lateButton.setOnClickListener(v -> listener.onStatus(student, "LATE"));
        holder.binding.absentButton.setOnClickListener(v -> listener.onStatus(student, "ABSENT"));
    }

    private void style(MaterialButton button, boolean selected, int color) {
        button.setTextColor(selected ? Color.WHITE : color);
        button.setBackgroundColor(selected ? color : Color.TRANSPARENT);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemRosterBinding binding;

        Holder(ItemRosterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
