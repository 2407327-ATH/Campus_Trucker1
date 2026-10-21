package com.northrise.campustrack.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.northrise.campustrack.CampusTrackApp;
import com.northrise.campustrack.data.SessionManager;
import com.northrise.campustrack.databinding.DialogCourseBinding;
import com.northrise.campustrack.databinding.FragmentListBinding;
import com.northrise.campustrack.model.CourseStat;
import com.northrise.campustrack.model.Role;
import com.northrise.campustrack.util.AppExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CoursesFragment extends Fragment {
    private static final String MARK_MODE = "markMode";
    private FragmentListBinding binding;
    private final List<CourseStat> all = new ArrayList<>();
    private final CourseAdapter adapter = new CourseAdapter(course ->
            startActivity(CourseDetailActivity.intent(requireContext(), course.courseId)));

    public static CoursesFragment newInstance(boolean markMode) {
        CoursesFragment fragment = new CoursesFragment();
        Bundle args = new Bundle();
        args.putBoolean(MARK_MODE, markMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentListBinding.inflate(inflater, container, false);
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        boolean markMode = getArguments() != null && getArguments().getBoolean(MARK_MODE);
        SessionManager session = CampusTrackApp.get().session();
        boolean canAdd = !markMode && (Role.LECTURER.equals(session.getRole()) || Role.ADMIN.equals(session.getRole()));
        binding.addButton.setVisibility(canAdd ? View.VISIBLE : View.GONE);
        binding.addButton.setOnClickListener(v -> showCreateDialog(Role.ADMIN.equals(session.getRole())));
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) { }
        });
        binding.emptyText.setText(markMode ? "No courses to mark." : "No courses match your search.");
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SessionManager session = CampusTrackApp.get().session();
        var reports = CampusTrackApp.get().repository().database().reportDao();
        if (Role.STUDENT.equals(session.getRole())) {
            reports.observeStudentCourses(session.getUserId()).observe(getViewLifecycleOwner(), this::show);
        } else if (Role.LECTURER.equals(session.getRole())) {
            reports.observeManagedCourses(session.getUserId(), 0).observe(getViewLifecycleOwner(), this::show);
        } else {
            reports.observeManagedCourses(0, 1).observe(getViewLifecycleOwner(), this::show);
        }
    }

    private void show(List<CourseStat> courses) {
        all.clear();
        all.addAll(courses);
        filter(LoginActivity.text(binding.searchInput.getText()));
    }

    private void filter(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.US);
        List<CourseStat> shown = new ArrayList<>();
        for (CourseStat course : all) {
            String haystack = (course.code + " " + course.title + " " + course.lecturerName).toLowerCase(Locale.US);
            if (needle.isEmpty() || haystack.contains(needle)) {
                shown.add(course);
            }
        }
        adapter.submit(shown);
        binding.emptyText.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showCreateDialog(boolean admin) {
        DialogCourseBinding form = DialogCourseBinding.inflate(getLayoutInflater());
        form.lecturerLayout.setVisibility(admin ? View.VISIBLE : View.GONE);
        new AlertDialog.Builder(requireContext())
                .setTitle("Add course")
                .setView(form.getRoot())
                .setPositiveButton("Save", (dialog, which) -> AppExecutors.io(() -> {
                    String message = CampusTrackApp.get().repository().createCourse(
                            LoginActivity.text(form.codeInput.getText()),
                            LoginActivity.text(form.titleInput.getText()),
                            LoginActivity.text(form.roomInput.getText()),
                            LoginActivity.text(form.lecturerInput.getText())
                    );
                    if (message != null && isAdded()) {
                        AppExecutors.main(() -> Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show());
                    }
                }))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
