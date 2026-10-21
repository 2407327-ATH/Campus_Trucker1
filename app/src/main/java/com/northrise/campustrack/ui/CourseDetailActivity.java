package com.northrise.campustrack.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.northrise.campustrack.CampusTrackApp;
import com.northrise.campustrack.data.SessionManager;
import com.northrise.campustrack.data.repo.CampusRepository;
import com.northrise.campustrack.databinding.ActivityCourseDetailBinding;
import com.northrise.campustrack.databinding.DialogCourseBinding;
import com.northrise.campustrack.databinding.DialogEnrollBinding;
import com.northrise.campustrack.databinding.DialogQrBinding;
import com.northrise.campustrack.model.AttendanceSession;
import com.northrise.campustrack.model.Course;
import com.northrise.campustrack.model.CourseStat;
import com.northrise.campustrack.model.Role;
import com.northrise.campustrack.model.RosterStudent;
import com.northrise.campustrack.util.AppExecutors;
import com.northrise.campustrack.util.AttendanceMath;
import com.northrise.campustrack.util.Dates;
import com.northrise.campustrack.util.QrPayload;

import java.util.List;

public class CourseDetailActivity extends AppCompatActivity {
    public static final String EXTRA_COURSE_ID = "courseId";

    private ActivityCourseDetailBinding binding;
    private long courseId;
    private long openSessionId;
    private final RosterAdapter rosterAdapter = new RosterAdapter((student, status) -> mark(student, status));
    private final HistoryAdapter historyAdapter = new HistoryAdapter();
    private LiveData<List<RosterStudent>> rosterLive;
    private final Observer<List<RosterStudent>> rosterObserver = this::showRoster;

    public static Intent intent(Context context, long courseId) {
        Intent intent = new Intent(context, CourseDetailActivity.class);
        intent.putExtra(EXTRA_COURSE_ID, courseId);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        courseId = getIntent().getLongExtra(EXTRA_COURSE_ID, 0);
        binding.rosterRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.historyRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.rosterRecycler.setAdapter(rosterAdapter);
        binding.historyRecycler.setAdapter(historyAdapter);
        SessionManager session = CampusTrackApp.get().session();
        boolean student = Role.STUDENT.equals(session.getRole());
        boolean lecturer = Role.LECTURER.equals(session.getRole());
        boolean admin = Role.ADMIN.equals(session.getRole());
        binding.historyRecycler.setVisibility(student ? View.VISIBLE : View.GONE);
        binding.rosterRecycler.setVisibility(student ? View.GONE : View.VISIBLE);
        binding.startButton.setVisibility(lecturer ? View.VISIBLE : View.GONE);
        binding.qrButton.setVisibility(lecturer ? View.VISIBLE : View.GONE);
        binding.closeButton.setVisibility(lecturer ? View.VISIBLE : View.GONE);
        binding.enrollButton.setVisibility(lecturer || admin ? View.VISIBLE : View.GONE);
        binding.absentButton.setVisibility(lecturer || admin ? View.VISIBLE : View.GONE);
        binding.editButton.setVisibility(lecturer || admin ? View.VISIBLE : View.GONE);
        binding.startButton.setOnClickListener(v -> runAction(() -> CampusTrackApp.get().repository().startSession(courseId).message));
        binding.closeButton.setOnClickListener(v -> {
            if (openSessionId == 0) {
                toast("There is no open session.");
                return;
            }
            runAction(() -> CampusTrackApp.get().repository().closeSession(openSessionId));
        });
        binding.qrButton.setOnClickListener(v -> showQr());
        binding.enrollButton.setOnClickListener(v -> showEnroll());
        binding.absentButton.setOnClickListener(v -> {
            if (openSessionId == 0) {
                toast("Start a session first.");
                return;
            }
            runAction(() -> CampusTrackApp.get().repository().markUnmarkedAbsent(openSessionId));
        });
        binding.editButton.setOnClickListener(v -> showEdit());
        var reports = CampusTrackApp.get().repository().database().reportDao();
        if (student) {
            reports.observeStudentCourse(session.getUserId(), courseId).observe(this, this::showStat);
            reports.observeCourseHistory(session.getUserId(), courseId).observe(this, rows -> {
                historyAdapter.submit(rows);
                binding.emptyText.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                binding.emptyText.setText("No attendance has been recorded for you in this course.");
            });
        } else {
            reports.observeClass(courseId).observe(this, this::showStat);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Role.STUDENT.equals(CampusTrackApp.get().session().getRole())) {
            refreshSession();
        }
    }

    private void showStat(CourseStat stat) {
        if (stat == null) {
            return;
        }
        binding.codeText.setText(stat.code);
        binding.titleText.setText(stat.title);
        String room = stat.room == null || stat.room.isEmpty() ? "Room TBA" : stat.room;
        binding.metaText.setText(room + " · " + stat.lecturerName);
        binding.percentText.setText(AttendanceMath.format(AttendanceMath.percentage(stat.present, stat.late, stat.absent)));
        binding.percentText.setTextColor(Ui.percentColor(this, stat.present, stat.late, stat.absent));
        binding.breakdownText.setText(AttendanceMath.breakdown(stat.present, stat.late, stat.absent));
        binding.progressBar.setProgress(Ui.progress(stat.present, stat.late, stat.absent));
    }

    private void refreshSession() {
        AppExecutors.io(() -> {
            AttendanceSession open = CampusTrackApp.get().repository().openSession(courseId);
            AppExecutors.main(() -> {
                if (isFinishing()) {
                    return;
                }
                openSessionId = open == null ? 0 : open.id;
                if (open == null) {
                    binding.sessionText.setText("No open session. Start one to mark attendance or show a QR code.");
                } else {
                    binding.sessionText.setText("Session open until " + Dates.format(open.expiresAt));
                }
                watchRoster();
            });
        });
    }

    private void watchRoster() {
        if (rosterLive != null) {
            rosterLive.removeObserver(rosterObserver);
        }
        rosterLive = CampusTrackApp.get().repository().database().reportDao().observeRoster(courseId, openSessionId);
        rosterLive.observe(this, rosterObserver);
    }

    private void showRoster(List<RosterStudent> students) {
        rosterAdapter.submit(students, openSessionId != 0);
        binding.emptyText.setVisibility(students.isEmpty() ? View.VISIBLE : View.GONE);
        binding.emptyText.setText("No students are enrolled yet.");
    }

    private void mark(RosterStudent student, String status) {
        if (openSessionId == 0) {
            toast("Start a session before marking attendance.");
            return;
        }
        runAction(() -> CampusTrackApp.get().repository().mark(openSessionId, student.studentId, status));
    }

    private void showQr() {
        AppExecutors.io(() -> {
            AttendanceSession open = CampusTrackApp.get().repository().openSession(courseId);
            AppExecutors.main(() -> {
                if (open == null) {
                    toast("Start a session before showing the QR code.");
                    return;
                }
                DialogQrBinding dialog = DialogQrBinding.inflate(getLayoutInflater());
                String payload = QrPayload.encode(open.id, open.qrToken);
                dialog.qrImage.setImageBitmap(QrImages.create(payload));
                dialog.tokenText.setText(payload);
                dialog.expiryText.setText("Expires " + Dates.format(open.expiresAt));
                new AlertDialog.Builder(this)
                        .setTitle("Attendance QR")
                        .setView(dialog.getRoot())
                        .setPositiveButton("Done", null)
                        .show();
            });
        });
    }

    private void showEnroll() {
        DialogEnrollBinding form = DialogEnrollBinding.inflate(getLayoutInflater());
        new AlertDialog.Builder(this)
                .setTitle("Enrol student")
                .setView(form.getRoot())
                .setPositiveButton("Enrol", (d, w) -> runAction(() ->
                        CampusTrackApp.get().repository().enroll(courseId, LoginActivity.text(form.emailInput.getText()))))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEdit() {
        AppExecutors.io(() -> {
            Course course = CampusTrackApp.get().repository().database().courseDao().findById(courseId);
            AppExecutors.main(() -> {
                if (course == null) {
                    return;
                }
                DialogCourseBinding form = DialogCourseBinding.inflate(getLayoutInflater());
                form.codeInput.setText(course.code);
                form.titleInput.setText(course.title);
                form.roomInput.setText(course.room);
                new AlertDialog.Builder(this)
                        .setTitle("Edit course")
                        .setView(form.getRoot())
                        .setPositiveButton("Save", (d, w) -> runAction(() -> CampusTrackApp.get().repository().updateCourse(
                                courseId,
                                LoginActivity.text(form.codeInput.getText()),
                                LoginActivity.text(form.titleInput.getText()),
                                LoginActivity.text(form.roomInput.getText())
                        )))
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        });
    }

    private void runAction(java.util.concurrent.Callable<String> work) {
        AppExecutors.io(() -> {
            String message;
            try {
                message = work.call();
            } catch (Exception ex) {
                message = "That action could not be saved.";
            }
            String toast = message;
            AppExecutors.main(() -> {
                if (toast != null) {
                    toast(toast);
                }
                if (!Role.STUDENT.equals(CampusTrackApp.get().session().getRole())) {
                    refreshSession();
                }
            });
        });
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
