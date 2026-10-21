package com.northrise.campustrack.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.northrise.campustrack.CampusTrackApp;
import com.northrise.campustrack.data.SessionManager;
import com.northrise.campustrack.databinding.FragmentReportsBinding;
import com.northrise.campustrack.model.AdminStats;
import com.northrise.campustrack.model.CourseStat;
import com.northrise.campustrack.model.RiskRow;
import com.northrise.campustrack.model.Role;
import com.northrise.campustrack.util.AttendanceMath;

import java.util.ArrayList;
import java.util.List;

public class ReportsFragment extends Fragment {
    private FragmentReportsBinding binding;
    private final CourseAdapter courses = new CourseAdapter(course ->
            startActivity(CourseDetailActivity.intent(requireContext(), course.courseId)));
    private final RiskAdapter risks = new RiskAdapter();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.riskRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(courses);
        binding.riskRecycler.setAdapter(risks);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SessionManager session = CampusTrackApp.get().session();
        var reports = CampusTrackApp.get().repository().database().reportDao();
        if (Role.STUDENT.equals(session.getRole())) {
            binding.riskTitle.setVisibility(View.GONE);
            binding.riskRecycler.setVisibility(View.GONE);
            binding.emptyText.setVisibility(View.GONE);
            reports.observeStudentCourses(session.getUserId()).observe(getViewLifecycleOwner(), this::showCourses);
        } else {
            int admin = Role.ADMIN.equals(session.getRole()) ? 1 : 0;
            long lecturerId = admin == 1 ? 0 : session.getUserId();
            reports.observeManagedCourses(lecturerId, admin).observe(getViewLifecycleOwner(), this::showCourses);
            reports.observeRisks(lecturerId, admin).observe(getViewLifecycleOwner(), this::showRisks);
            if (admin == 1) {
                reports.observeAdminStats().observe(getViewLifecycleOwner(), this::showAdmin);
            }
        }
    }

    private void showCourses(List<CourseStat> items) {
        courses.submit(items);
        if (!Role.ADMIN.equals(CampusTrackApp.get().session().getRole())) {
            int present = 0;
            int late = 0;
            int absent = 0;
            for (CourseStat course : items) {
                present += course.present;
                late += course.late;
                absent += course.absent;
            }
            binding.summaryText.setText("Overall " + AttendanceMath.format(AttendanceMath.percentage(present, late, absent))
                    + "\n" + AttendanceMath.breakdown(present, late, absent));
        }
    }

    private void showAdmin(AdminStats stats) {
        if (stats == null) {
            return;
        }
        Double percentage = stats.recordCount == 0 ? null : stats.attendedCount * 100.0 / stats.recordCount;
        binding.summaryText.setText("University attendance " + AttendanceMath.format(percentage)
                + "\n" + stats.userCount + " users · " + stats.courseCount + " courses · "
                + stats.recordCount + " attendance records");
    }

    private void showRisks(List<RiskRow> rows) {
        List<RiskRow> low = new ArrayList<>();
        for (RiskRow row : rows) {
            if (AttendanceMath.isLow(row.present, row.late, row.absent)) {
                low.add(row);
            }
        }
        risks.submit(low);
        binding.emptyText.setVisibility(low.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
