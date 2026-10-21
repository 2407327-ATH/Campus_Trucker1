package com.northrise.campustrack.data.repo;

import com.northrise.campustrack.data.AppDatabase;
import com.northrise.campustrack.data.SessionManager;
import com.northrise.campustrack.data.remote.ApiClient;
import com.northrise.campustrack.data.remote.ApiService;
import com.northrise.campustrack.data.remote.Dtos;
import com.northrise.campustrack.model.AttendanceRecord;
import com.northrise.campustrack.model.AttendanceSession;
import com.northrise.campustrack.model.Course;
import com.northrise.campustrack.model.Enrollment;
import com.northrise.campustrack.model.Role;
import com.northrise.campustrack.model.User;
import com.northrise.campustrack.util.Dates;
import com.northrise.campustrack.util.PasswordHasher;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import retrofit2.Response;

class SyncEngine {
    private final AppDatabase db;
    private final SessionManager session;
    private final ApiService api;

    SyncEngine(AppDatabase db, SessionManager session, ApiService api) {
        this.db = db;
        this.session = session;
        this.api = api;
    }

    String sync() {
        if (!session.isLoggedIn()) {
            return "Log in before syncing.";
        }
        if (session.getToken() == null || session.getToken().isEmpty()) {
            return "This login is stored only on this phone. Log in while the server is running to sync.";
        }
        try {
            Response<Dtos.SyncResult> response = api.sync(buildPayload()).execute();
            if (response.code() == 401) {
                return "Server session expired. Log in again while online.";
            }
            if (!response.isSuccessful() || response.body() == null) {
                return ApiClient.errorMessage(response, "Sync failed.");
            }
            clearSaved(response.body().saved);
            applySnapshot(response.body().snapshot);
            if (response.body().errors != null && !response.body().errors.isEmpty()) {
                return "Sync complete. " + response.body().errors.get(0);
            }
            return "Sync complete.";
        } catch (IOException ex) {
            return "Server unreachable. Changes stay on this phone until it is back.";
        }
    }

    void pullOnly() throws IOException {
        Response<Dtos.SyncResult> response = api.sync(new Dtos.SyncPayload()).execute();
        if (response.isSuccessful() && response.body() != null) {
            applySnapshot(response.body().snapshot);
        }
    }

    AttendanceSession upsertSession(Course course, Dtos.SessionDto dto, boolean pending) {
        AttendanceSession existing = db.sessionDao().findByToken(dto.qrToken);
        if (existing == null) {
            existing = new AttendanceSession();
            existing.courseId = course.id;
            existing.qrToken = dto.qrToken;
            existing.startedAt = Dates.parseIso(dto.startedAt);
            existing.expiresAt = Dates.parseIso(dto.expiresAt);
            existing.status = dto.status == null ? "OPEN" : dto.status;
            existing.pendingSync = pending;
            existing.id = db.sessionDao().insert(existing);
            return existing;
        }
        if (!existing.pendingSync) {
            existing.startedAt = Dates.parseIso(dto.startedAt);
            existing.expiresAt = Dates.parseIso(dto.expiresAt);
            existing.status = dto.status == null ? existing.status : dto.status;
            existing.pendingSync = pending;
            db.sessionDao().update(existing);
        }
        return existing;
    }

    Course upsertCourse(Dtos.CourseDto dto, boolean pending) {
        if (dto == null || dto.code == null || dto.lecturerEmail == null) {
            return null;
        }
        User lecturer = db.userDao().findByEmail(dto.lecturerEmail.trim().toLowerCase(Locale.US));
        if (lecturer == null) {
            return null;
        }
        Course existing = db.courseDao().findByCode(dto.code.trim().toUpperCase(Locale.US));
        if (existing == null) {
            existing = new Course();
            existing.code = dto.code.trim().toUpperCase(Locale.US);
            existing.title = dto.title;
            existing.room = dto.room == null ? "" : dto.room;
            existing.lecturerId = lecturer.id;
            existing.pendingSync = pending;
            existing.id = db.courseDao().insert(existing);
            return existing;
        }
        if (!existing.pendingSync) {
            existing.title = dto.title;
            existing.room = dto.room == null ? "" : dto.room;
            existing.lecturerId = lecturer.id;
            existing.pendingSync = pending;
            db.courseDao().update(existing);
        }
        return existing;
    }

    private Dtos.SyncPayload buildPayload() {
        Dtos.SyncPayload payload = new Dtos.SyncPayload();
        for (User user : db.userDao().pending()) {
            if (user.pendingPassword == null) {
                continue;
            }
            Dtos.UserDto dto = new Dtos.UserDto();
            dto.name = user.name;
            dto.email = user.email;
            dto.role = user.role;
            dto.studentNumber = user.studentNumber;
            dto.password = user.pendingPassword;
            payload.users.add(dto);
        }
        for (Course course : db.courseDao().pending()) {
            User lecturer = db.userDao().findById(course.lecturerId);
            if (lecturer == null) {
                continue;
            }
            Dtos.CourseDto dto = new Dtos.CourseDto();
            dto.code = course.code;
            dto.title = course.title;
            dto.room = course.room;
            dto.lecturerEmail = lecturer.email;
            payload.courses.add(dto);
        }
        for (Enrollment enrollment : db.enrollmentDao().pending()) {
            User student = db.userDao().findById(enrollment.studentId);
            Course course = db.courseDao().findById(enrollment.courseId);
            if (student == null || course == null) {
                continue;
            }
            Dtos.EnrollmentDto dto = new Dtos.EnrollmentDto();
            dto.studentEmail = student.email;
            dto.courseCode = course.code;
            payload.enrollments.add(dto);
        }
        for (AttendanceSession stored : db.sessionDao().pending()) {
            Course course = db.courseDao().findById(stored.courseId);
            if (course == null) {
                continue;
            }
            payload.sessions.add(sessionDto(stored, course.code));
        }
        for (AttendanceRecord record : db.recordDao().pending()) {
            AttendanceSession stored = db.sessionDao().findById(record.sessionId);
            User student = db.userDao().findById(record.studentId);
            if (stored == null || student == null) {
                continue;
            }
            Dtos.RecordDto dto = new Dtos.RecordDto();
            dto.qrToken = stored.qrToken;
            dto.studentEmail = student.email;
            dto.status = record.status;
            dto.markedAt = Dates.iso(record.markedAt);
            payload.records.add(dto);
        }
        return payload;
    }

    private void clearSaved(Dtos.SyncSaved saved) {
        if (saved == null) {
            return;
        }
        if (saved.users != null) {
            for (String email : saved.users) {
                User user = db.userDao().findByEmail(email);
                if (user != null) {
                    user.pendingSync = false;
                    user.pendingPassword = null;
                    db.userDao().update(user);
                }
            }
        }
        if (saved.courses != null) {
            for (String code : saved.courses) {
                Course course = db.courseDao().findByCode(code);
                if (course != null) {
                    course.pendingSync = false;
                    db.courseDao().update(course);
                }
            }
        }
        if (saved.enrollments != null) {
            for (String key : saved.enrollments) {
                String[] parts = key.split("\\|");
                if (parts.length != 2) {
                    continue;
                }
                User student = db.userDao().findByEmail(parts[0]);
                Course course = db.courseDao().findByCode(parts[1]);
                if (student == null || course == null) {
                    continue;
                }
                Enrollment enrollment = db.enrollmentDao().find(student.id, course.id);
                if (enrollment != null) {
                    enrollment.pendingSync = false;
                    db.enrollmentDao().update(enrollment);
                }
            }
        }
        if (saved.sessions != null) {
            for (String token : saved.sessions) {
                AttendanceSession stored = db.sessionDao().findByToken(token);
                if (stored != null) {
                    stored.pendingSync = false;
                    db.sessionDao().update(stored);
                }
            }
        }
        if (saved.records != null) {
            for (String key : saved.records) {
                String[] parts = key.split("\\|");
                if (parts.length != 2) {
                    continue;
                }
                AttendanceSession stored = db.sessionDao().findByToken(parts[0]);
                User student = db.userDao().findByEmail(parts[1]);
                if (stored == null || student == null) {
                    continue;
                }
                AttendanceRecord record = db.recordDao().find(stored.id, student.id);
                if (record != null) {
                    record.pendingSync = false;
                    db.recordDao().update(record);
                }
            }
        }
    }

    private void applySnapshot(Dtos.Snapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        if (snapshot.users != null) {
            for (Dtos.UserDto dto : snapshot.users) {
                upsertUser(dto);
            }
        }
        if (snapshot.courses != null) {
            for (Dtos.CourseDto dto : snapshot.courses) {
                upsertCourse(dto, false);
            }
        }
        if (snapshot.enrollments != null) {
            for (Dtos.EnrollmentDto dto : snapshot.enrollments) {
                upsertEnrollment(dto);
            }
        }
        if (snapshot.sessions != null) {
            for (Dtos.SessionDto dto : snapshot.sessions) {
                Course course = dto.courseCode == null ? null : db.courseDao().findByCode(dto.courseCode);
                if (course != null && dto.qrToken != null && dto.startedAt != null && dto.expiresAt != null) {
                    upsertSession(course, dto, false);
                }
            }
        }
        if (snapshot.records != null) {
            for (Dtos.RecordDto dto : snapshot.records) {
                upsertRecord(dto);
            }
        }
    }

    private void upsertUser(Dtos.UserDto dto) {
        if (dto == null || dto.email == null) {
            return;
        }
        String email = dto.email.trim().toLowerCase(Locale.US);
        User existing = db.userDao().findByEmail(email);
        if (existing == null) {
            User user = new User();
            user.email = email;
            user.name = dto.name == null ? email : dto.name;
            user.role = Role.normalize(dto.role);
            user.studentNumber = dto.studentNumber;
            user.passwordHash = PasswordHasher.hash(UUID.randomUUID().toString());
            user.pendingSync = false;
            db.userDao().insert(user);
            return;
        }
        if (existing.pendingSync) {
            return;
        }
        existing.name = dto.name == null ? existing.name : dto.name;
        existing.role = Role.normalize(dto.role);
        existing.studentNumber = dto.studentNumber;
        db.userDao().update(existing);
    }

    private void upsertEnrollment(Dtos.EnrollmentDto dto) {
        if (dto.studentEmail == null || dto.courseCode == null) {
            return;
        }
        User student = db.userDao().findByEmail(dto.studentEmail.trim().toLowerCase(Locale.US));
        Course course = db.courseDao().findByCode(dto.courseCode.trim().toUpperCase(Locale.US));
        if (student == null || course == null || db.enrollmentDao().find(student.id, course.id) != null) {
            return;
        }
        Enrollment enrollment = new Enrollment();
        enrollment.studentId = student.id;
        enrollment.courseId = course.id;
        enrollment.pendingSync = false;
        db.enrollmentDao().insert(enrollment);
    }

    private void upsertRecord(Dtos.RecordDto dto) {
        if (dto.qrToken == null || dto.studentEmail == null || dto.status == null || dto.markedAt == null) {
            return;
        }
        AttendanceSession stored = db.sessionDao().findByToken(dto.qrToken);
        User student = db.userDao().findByEmail(dto.studentEmail.trim().toLowerCase(Locale.US));
        if (stored == null || student == null) {
            return;
        }
        AttendanceRecord existing = db.recordDao().find(stored.id, student.id);
        long markedAt = Dates.parseIso(dto.markedAt);
        if (existing == null) {
            AttendanceRecord record = new AttendanceRecord();
            record.sessionId = stored.id;
            record.studentId = student.id;
            record.status = dto.status;
            record.markedAt = markedAt;
            record.pendingSync = false;
            db.recordDao().insert(record);
            return;
        }
        if (!existing.pendingSync) {
            existing.status = dto.status;
            existing.markedAt = markedAt;
            db.recordDao().update(existing);
        }
    }

    private Dtos.SessionDto sessionDto(AttendanceSession stored, String courseCode) {
        Dtos.SessionDto dto = new Dtos.SessionDto();
        dto.courseCode = courseCode;
        dto.qrToken = stored.qrToken;
        dto.startedAt = Dates.iso(stored.startedAt);
        dto.expiresAt = Dates.iso(stored.expiresAt);
        dto.status = stored.status;
        return dto;
    }
}
