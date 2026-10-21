package com.northrise.campustrack.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.northrise.campustrack.CampusTrackApp;
import com.northrise.campustrack.R;
import com.northrise.campustrack.data.SessionManager;
import com.northrise.campustrack.databinding.FragmentDashboardBinding;
import com.northrise.campustrack.model.AdminStats;
import com.northrise.campustrack.model.CourseStat;
import com.northrise.campustrack.model.Role;
import com.northrise.campustrack.util.AppExecutors;
import com.northrise.campustrack.util.AttendanceMath;

import java.util.List;

public class DashboardFragment extends Fragment {
    private FragmentDashboardBinding binding;
    private final CourseAdapter adapter = new CourseAdapter(course -> {
        if (getContext() == null) {
            return;
        }
        startActivity(CourseDetailActivity.intent(requireContext(), course.courseId));
    });
    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> scanner =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() == null) {
                    return;
                }
                checkIn(result.getContents());
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        binding.recycler.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        SessionManager session = CampusTrackApp.get().session();
        binding.greetingText.setText("Hello, " + session.getName());
        binding.offlineHint.setText(session.getLastSync());
        wireActions(session.getRole());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        observe(CampusTrackApp.get().session());
    }

    private void wireActions(String role) {
        if (Role.STUDENT.equals(role)) {
            binding.primaryButton.setText("Scan QR");
            binding.secondaryButton.setText("Alerts");
            binding.sectionTitle.setText("My courses");
            binding.primaryButton.setOnClickListener(v -> {
                ScanOptions options = new ScanOptions();
                options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
                options.setPrompt("Scan the class attendance QR");
                options.setBeepEnabled(true);
                options.setOrientationLocked(false);
                scanner.launch(options);
            });
            binding.secondaryButton.setOnClickListener(v -> ((MainActivity) requireActivity()).openTab(R.id.nav_alerts));
            binding.codeButton.setVisibility(View.VISIBLE);
            binding.codeButton.setOnClickListener(v -> showCodeEntry());
        } else if (Role.LECTURER.equals(role)) {
            binding.primaryButton.setText("Mark attendance");
            binding.secondaryButton.setText("Alerts");
            binding.sectionTitle.setText("My courses");
            binding.primaryButton.setOnClickListener(v -> ((MainActivity) requireActivity()).openTab(R.id.nav_mark));
            binding.secondaryButton.setOnClickListener(v -> ((MainActivity) requireActivity()).showAlerts());
        } else {
            binding.primaryButton.setText("Users");
            binding.secondaryButton.setText("Reports");
            binding.sectionTitle.setText("Courses");
            binding.primaryButton.setOnClickListener(v -> ((MainActivity) requireActivity()).openTab(R.id.nav_users));
            binding.secondaryButton.setOnClickListener(v -> ((MainActivity) requireActivity()).openTab(R.id.nav_reports));
        }
    }

    private void observe(SessionManager session) {
        var reports = CampusTrackApp.get().repository().database().reportDao();
        if (Role.STUDENT.equals(session.getRole())) {
            reports.observeStudentCourses(session.getUserId()).observe(getViewLifecycleOwner(), this::showStudent);
        } else if (Role.LECTURER.equals(session.getRole())) {
            reports.observeManagedCourses(session.getUserId(), 0).observe(getViewLifecycleOwner(), this::showClass);
        } else {
            reports.observeAdminStats().observe(getViewLifecycleOwner(), this::showAdmin);
            reports.observeManagedCourses(0, 1).observe(getViewLifecycleOwner(), this::showClass);
        }
    }

    private void showCodeEntry() {
        EditText input = new EditText(requireContext());
        input.setHint("CAMPUSTRACK|session|token");
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);
        new AlertDialog.Builder(requireContext())
                .setTitle("Enter attendance code")
                .setMessage("Use this when the camera cannot see the lecturer's QR code. The code is shown under the QR image.")
                .setView(input)
                .setPositiveButton("Check in", (dialog, which) -> checkIn(input.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkIn(String raw) {
        AppExecutors.io(() -> {
            String message = CampusTrackApp.get().repository().checkInQr(raw);
            AppExecutors.main(() -> {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void showStudent(List<CourseStat> courses) {
        int present = 0;
        int late = 0;
        int absent = 0;
        int low = 0;
        for (CourseStat course : courses) {
            present += course.present;
            late += course.late;
            absent += course.absent;
            if (AttendanceMath.isLow(course.present, course.late, course.absent)) {
                low++;
            }
        }
        Double percentage = AttendanceMath.percentage(present, late, absent);
        binding.summaryValue.setText(AttendanceMath.format(percentage));
        binding.summaryLabel.setText("Overall attendance");
        binding.summaryDetail.setText(AttendanceMath.breakdown(present, late, absent));
        binding.summaryProgress.setProgress(Ui.progress(present, late, absent));
        binding.summaryValue.setTextColor(Ui.percentColor(requireContext(), present, late, absent));
        if (low > 0) {
            binding.warningText.setVisibility(View.VISIBLE);
            binding.warningText.setText(low + " course(s) are below the 75% minimum.");
        } else {
            binding.warningText.setVisibility(View.GONE);
        }
        adapter.submit(courses);
        binding.emptyText.setVisibility(courses.isEmpty() ? View.VISIBLE : View.GONE);
        binding.emptyText.setText("You are not enrolled in a course yet.");
    }

    private void showClass(List<CourseStat> courses) {
        if (!Role.ADMIN.equals(CampusTrackApp.get().session().getRole())) {
            int present = 0;
            int late = 0;
            int absent = 0;
            for (CourseStat course : courses) {
                present += course.present;
                late += course.late;
                absent += course.absent;
            }
            binding.summaryValue.setText(AttendanceMath.format(AttendanceMath.percentage(present, late, absent)));
            binding.summaryLabel.setText(courses.size() + " course(s)");
            binding.summaryDetail.setText(AttendanceMath.breakdown(present, late, absent));
            binding.summaryProgress.setProgress(Ui.progress(present, late, absent));
            binding.summaryValue.setTextColor(Ui.percentColor(requireContext(), present, late, absent));
        }
        adapter.submit(courses);
        binding.emptyText.setVisibility(courses.isEmpty() ? View.VISIBLE : View.GONE);
        binding.emptyText.setText("No courses yet.");
    }

    private void showAdmin(AdminStats stats) {
        if (stats == null) {
            return;
        }
        Double percentage = stats.recordCount == 0 ? null : stats.attendedCount * 100.0 / stats.recordCount;
        binding.summaryValue.setText(AttendanceMath.format(percentage));
        binding.summaryLabel.setText("University attendance");
        binding.summaryDetail.setText(stats.userCount + " users · " + stats.courseCount + " courses · " + stats.sessionCount + " sessions");
        binding.summaryProgress.setProgress(percentage == null ? 0 : (int) Math.round(percentage));
    }
}
