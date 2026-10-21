package com.northrise.campustrack.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.northrise.campustrack.databinding.ItemCourseBinding;
import com.northrise.campustrack.model.CourseStat;
import com.northrise.campustrack.util.AttendanceMath;

import java.util.ArrayList;
import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.Holder> {
    public interface Listener {
        void onClick(CourseStat course);
    }

    private final Listener listener;
    private List<CourseStat> items = new ArrayList<>();

    public CourseAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<CourseStat> items) {
        this.items = items == null ? new ArrayList<>() : items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemCourseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        CourseStat course = items.get(position);
        holder.binding.codeText.setText(course.code);
        holder.binding.titleText.setText(course.title);
        String room = course.room == null || course.room.isEmpty() ? "Room TBA" : course.room;
        holder.binding.metaText.setText(room + " · " + course.lecturerName);
        holder.binding.percentText.setText(AttendanceMath.format(AttendanceMath.percentage(course.present, course.late, course.absent)));
        holder.binding.percentText.setTextColor(Ui.percentColor(holder.itemView.getContext(), course.present, course.late, course.absent));
        holder.binding.breakdownText.setText(AttendanceMath.breakdown(course.present, course.late, course.absent));
        holder.binding.progressBar.setProgress(Ui.progress(course.present, course.late, course.absent));
        holder.itemView.setOnClickListener(v -> listener.onClick(course));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemCourseBinding binding;

        Holder(ItemCourseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
