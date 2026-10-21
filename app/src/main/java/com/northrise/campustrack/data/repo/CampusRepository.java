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
import com.northrise.campustrack.model.Notification;
import com.northrise.campustrack.model.Role;
import com.northrise.campustrack.model.RosterStudent;
import com.northrise.campustrack.model.User;
import com.northrise.campustrack.util.AttendanceMath;
import com.northrise.campustrack.util.Dates;
import com.northrise.campustrack.util.PasswordHasher;
import com.northrise.campustrack.util.QrPayload;
import com.northrise.campustrack.util.Validators;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import retrofit2.Response;

public class CampusRepository {
    private static final long BASE = 1_791_439_200_000L;
    private static final long DAY = 86_400_000L;

    private final AppDatabase db;
    private final SessionManager session;
    private final ApiService api;
    private final SyncEngine syncEngine;

    public CampusRepository(AppDatabase db, SessionManager session) {
        this.db = db;
        this.session = session;
        this.api = ApiClient.create(session);
        this.syncEngine = new SyncEngine(db, session, api);
    }

    public AppDatabase database() {
        return db;
    }

    public SessionManager session() {
        return session;
    }

    public void seedIfEmpty() {
        if (db.userDao().count() > 0) {
            return;
        }
        db.runInTransaction(() -> {
            if (db.userDao().count() > 0) {
                return;
            }
            long admin = addUser("Admin User", "admin@nu.ac.zm", "Admin@123", Role.ADMIN, null, false);
            long lecturer = addUser("Dr. Banda", "lecturer@nu.ac.zm", "Lecturer@123", Role.LECTURER, null, false);
            long chipo = addUser("Chipo Phiri", "student@nu.ac.zm", "Student@123", Role.STUDENT, "SN2201001", false);
            long james = addUser("James Bwalya", "student2@nu.ac.zm", "Student@123", Role.STUDENT, "SN2201002", false);
            long mad = addCourse("MAD301", "Mobile Application Development", lecturer, "Lab 2", false);
            long dbs = addCourse("DBS220", "Database Systems", lecturer, "LH1", false);
            addEnrollment(chipo, mad, false);
            addEnrollment(james, mad, false);
            addEnrollment(chipo, dbs, false);
            addEnrollment(james, dbs, false);
            addClosedSession(mad, "mad-20260917", BASE - 21 * DAY, chipo, james, "ABSENT", "PRESENT");
            addClosedSession(mad, "mad-20260924", BASE - 14 * DAY, chipo, james, "PRESENT", "LATE");
            addClosedSession(mad, "mad-20261001", BASE - 7 * DAY, chipo, james, "PRESENT", "PRESENT");
            addClosedSession(mad, "mad-20261008", BASE, chipo, james, "PRESENT", "PRESENT");
            addClosedSession(dbs, "dbs-20260922", BASE - 16 * DAY, chipo, james, "ABSENT", "PRESENT");
            addClosedSession(dbs, "dbs-20260929", BASE - 9 * DAY, chipo, james, "ABSENT", "LATE");
            addClosedSession(dbs, "dbs-20261006", BASE - 2 * DAY, chipo, james, "PRESENT", "PRESENT");
            refreshCourseAlerts(mad, false);
            refreshCourseAlerts(dbs, false);
        });
    }

    public AuthResult login(String email, String password) {
        String emailError = Validators.email(email);
        if (emailError != null) {
            return AuthResult.error(emailError);
        }
        String passwordError = Validators.password(password);
        if (passwordError != null) {
            return AuthResult.error(passwordError);
        }
        String normalized = email.trim().toLowerCase(Locale.US);
        try {
            Response<Dtos.LoginResponse> response = api.login(new Dtos.LoginRequest(normalized, password)).execute();
            if (response.isSuccessful() && response.body() != null && response.body().user != null) {
                User user = cacheLoggedInUser(response.body().user, password);
                session.save(user, response.body().token);
                try {
                    syncEngine.pullOnly();
                    refreshAllAlerts(false);
                } catch (IOException ignored) {
                }
                return AuthResult.ok();
            }
            if (response.code() == 401 || response.code() == 400) {
                User pending = db.userDao().findByEmail(normalized);
                if (pending != null && pending.pendingSync && pending.pendingPassword != null
                        && PasswordHasher.verify(password, pending.passwordHash)) {
                    try {
                        Response<Dtos.LoginResponse> created = api.register(new Dtos.RegisterRequest(
                                pending.name, pending.email, password, pending.role, pending.studentNumber
                        )).execute();
                        if (created.isSuccessful() && created.body() != null && created.body().user != null) {
                            User user = cacheLoggedInUser(created.body().user, password);
                            session.save(user, created.body().token);
                            return AuthResult.ok();
                        }
                    } catch (IOException ignored) {
                    }
                    session.save(pending, "");
                    return AuthResult.ok();
                }
                return AuthResult.error("Email or password is incorrect.");
            }
        } catch (IOException ignored) {
        }
        User local = db.userDao().findByEmail(normalized);
        if (local == null || !PasswordHasher.verify(password, local.passwordHash)) {
            return AuthResult.error("Email or password is incorrect.");
        }
        String token = normalized.equalsIgnoreCase(session.getEmail()) ? session.getToken() : "";
        session.save(local, token);
        return AuthResult.ok();
    }

    public AuthResult register(String name, String email, String password, String confirm, String role, String studentNumber) {
        String error = first(
                Validators.name(name),
                Validators.email(email),
                Validators.password(password)
        );
        if (error == null && (confirm == null || !confirm.equals(password))) {
            error = "Passwords do not match.";
        }
        String normalizedRole = Role.normalize(role);
        if (error == null) {
            error = Validators.role(normalizedRole);
        }
        boolean student = Role.STUDENT.equals(normalizedRole);
        if (error == null) {
            error = Validators.studentNumber(studentNumber, student);
        }
        if (error != null) {
            return AuthResult.error(error);
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.US);
        if (db.userDao().findByEmail(normalizedEmail) != null) {
            return AuthResult.error("An account with that email already exists.");
        }
        String number = student && studentNumber != null ? studentNumber.trim().toUpperCase(Locale.US) : null;
        try {
            Response<Dtos.LoginResponse> response = api.register(
                    new Dtos.RegisterRequest(name.trim(), normalizedEmail, password, normalizedRole, number)
            ).execute();
            if (response.isSuccessful() && response.body() != null && response.body().user != null) {
                User user = cacheLoggedInUser(response.body().user, password);
                session.save(user, response.body().token);
                return AuthResult.ok();
            }
            if (response.code() >= 400 && response.code() < 500) {
                return AuthResult.error(ApiClient.errorMessage(response, "Registration was rejected."));
            }
        } catch (IOException ignored) {
        }
        long id = addUser(name.trim(), normalizedEmail, password, normalizedRole, number, true);
        User created = db.userDao().findById(id);
        session.save(created, "");
        return AuthResult.ok();
    }

    public String createCourse(String code, String title, String room, String lecturerEmail) {
        String error = first(Validators.courseCode(code), Validators.courseTitle(title));
        if (error != null) {
            return error;
        }
        String normalizedCode = code.trim().toUpperCase(Locale.US);
        if (db.courseDao().findByCode(normalizedCode) != null) {
            return "That course code already exists.";
        }
        User actor = currentUser();
        if (actor == null) {
            return "Log in again.";
        }
        long lecturerId;
        if (Role.ADMIN.equals(actor.role)) {
            if (Validators.email(lecturerEmail) != null) {
                return "Enter the lecturer's email.";
            }
            User lecturer = db.userDao().findByEmail(lecturerEmail.trim().toLowerCase(Locale.US));
            if (lecturer == null || !Role.LECTURER.equals(lecturer.role)) {
                return "No lecturer account uses that email.";
            }
            lecturerId = lecturer.id;
        } else if (Role.LECTURER.equals(actor.role)) {
            lecturerId = actor.id;
        } else {
            return "Only a lecturer or administrator can create a course.";
        }
        addCourse(normalizedCode, title.trim(), lecturerId, room == null ? "" : room.trim(), true);
        return null;
    }

    public String updateCourse(long courseId, String code, String title, String room) {
        Course course = db.courseDao().findById(courseId);
        String permission = assertCanManage(course);
        if (permission != null) {
            return permission;
        }
        String error = first(Validators.courseCode(code), Validators.courseTitle(title));
        if (error != null) {
            return error;
        }
        String normalizedCode = code.trim().toUpperCase(Locale.US);
        Course existing = db.courseDao().findByCode(normalizedCode);
        if (existing != null && existing.id != courseId) {
            return "That course code already exists.";
        }
        course.code = normalizedCode;
        course.title = title.trim();
        course.room = room == null ? "" : room.trim();
        course.pendingSync = true;
        db.courseDao().update(course);
        return null;
    }

    public String enroll(long courseId, String studentEmail) {
        if (Validators.email(studentEmail) != null) {
            return "Enter a valid student email.";
        }
        Course course = db.courseDao().findById(courseId);
        String permission = assertCanManage(course);
        if (permission != null) {
            return permission;
        }
        User student = db.userDao().findByEmail(studentEmail.trim().toLowerCase(Locale.US));
        if (student == null || !Role.STUDENT.equals(student.role)) {
            return "No student account uses that email.";
        }
        if (db.enrollmentDao().find(student.id, course.id) != null) {
            return "That student is already enrolled.";
        }
        addEnrollment(student.id, course.id, true);
        refreshCourseAlerts(course.id, true);
        return null;
    }

    public ActionResult startSession(long courseId) {
        Course course = db.courseDao().findById(courseId);
        String permission = assertLecturerOwner(course);
        if (permission != null) {
            return ActionResult.fail(permission);
        }
        long now = System.currentTimeMillis();
        db.sessionDao().closeExpired(now);
        AttendanceSession open = db.sessionDao().findOpen(courseId, now);
        if (open != null) {
            return ActionResult.ok(open.id, "This course already has an open session.");
        }
        if (hasToken()) {
            try {
                Dtos.SessionCreate body = new Dtos.SessionCreate();
                body.courseCode = course.code;
                Response<Dtos.SessionDto> response = api.startSession(body).execute();
                if (response.isSuccessful() && response.body() != null) {
                    AttendanceSession created = syncEngine.upsertSession(course, response.body(), false);
                    return ActionResult.ok(created.id, "Session started.");
                }
                if (response.code() == 401) {
                    return ActionResult.fail("Server session expired. Log in again online, or work offline after logging out.");
                }
                if (response.code() == 403) {
                    return ActionResult.fail(ApiClient.errorMessage(response, "You cannot start this session."));
                }
            } catch (IOException ignored) {
            }
        }
        AttendanceSession local = new AttendanceSession();
        local.courseId = course.id;
        local.qrToken = UUID.randomUUID().toString().replace("-", "");
        local.startedAt = now;
        local.expiresAt = now + AttendanceMath.SESSION_LENGTH_MS;
        local.status = "OPEN";
        local.pendingSync = true;
        local.id = db.sessionDao().insert(local);
        return ActionResult.ok(local.id, "Session saved on this phone. Sync before other phones can scan it.");
    }

    public String closeSession(long sessionId) {
        AttendanceSession found = db.sessionDao().findById(sessionId);
        if (found == null) {
            return "Session not found.";
        }
        Course course = db.courseDao().findById(found.courseId);
        String permission = assertLecturerOwner(course);
        if (permission != null) {
            return permission;
        }
        found.status = "CLOSED";
        found.expiresAt = Math.min(found.expiresAt, System.currentTimeMillis());
        found.pendingSync = true;
        db.sessionDao().update(found);
        return null;
    }

    public String mark(long sessionId, long studentId, String status) {
        String invalid = Validators.status(status);
        if (invalid != null) {
            return invalid;
        }
        AttendanceSession found = db.sessionDao().findById(sessionId);
        if (found == null) {
            return "Start a session before marking attendance.";
        }
        Course course = db.courseDao().findById(found.courseId);
        String permission = assertCanManage(course);
        if (permission != null) {
            return permission;
        }
        if (db.enrollmentDao().find(studentId, course.id) == null) {
            return "That student is not enrolled in this course.";
        }
        boolean pending = !pushManualMark(found, studentId, status);
        saveRecord(found.id, studentId, status, System.currentTimeMillis(), pending);
        refreshCourseAlerts(course.id, true);
        return null;
    }

    public String markUnmarkedAbsent(long sessionId) {
        AttendanceSession found = db.sessionDao().findById(sessionId);
        if (found == null) {
            return "Start a session first.";
        }
        Course course = db.courseDao().findById(found.courseId);
        String permission = assertCanManage(course);
        if (permission != null) {
            return permission;
        }
        List<RosterStudent> roster = db.reportDao().rosterNow(course.id, sessionId);
        int count = 0;
        long now = System.currentTimeMillis();
        for (RosterStudent student : roster) {
            if (student.sessionStatus == null) {
                boolean pending = !pushManualMark(found, student.studentId, "ABSENT");
                saveRecord(sessionId, student.studentId, "ABSENT", now, pending);
                count++;
            }
        }
        refreshCourseAlerts(course.id, true);
        return count + " unmarked student(s) marked absent.";
    }

    public String checkInQr(String raw) {
        if (!Role.STUDENT.equals(session.getRole())) {
            return "Only a student can scan a class QR code.";
        }
        String token = QrPayload.tokenFrom(raw);
        if (token == null) {
            return "This is not a CampusTrack attendance code.";
        }
        User student = currentUser();
        if (student == null) {
            return "Log in again.";
        }
        if (hasToken()) {
            try {
                Dtos.CheckinRequest body = new Dtos.CheckinRequest();
                body.qrToken = token;
                Response<Dtos.CheckinResponse> response = api.checkin(body).execute();
                if (response.isSuccessful() && response.body() != null) {
                    applyRemoteCheckin(response.body(), student);
                    return "Checked in as " + label(response.body().status) + ".";
                }
                if (response.code() >= 400 && response.code() < 500) {
                    return ApiClient.errorMessage(response, "Check-in was rejected.");
                }
            } catch (IOException ignored) {
            }
        }
        AttendanceSession found = db.sessionDao().findByToken(token);
        if (found == null) {
            return "This session is not on this phone yet. Connect to the server and scan again.";
        }
        long now = System.currentTimeMillis();
        if (!"OPEN".equals(found.status) || found.expiresAt <= now) {
            return "This session has expired. Ask the lecturer to start a new one.";
        }
        if (db.enrollmentDao().find(student.id, found.courseId) == null) {
            return "You are not enrolled in this course.";
        }
        String status = AttendanceMath.statusForCheckIn(found.startedAt, now);
        saveRecord(found.id, student.id, status, now, true);
        refreshCourseAlerts(found.courseId, true);
        return "Checked in as " + label(status) + ". It will sync when the server is reachable.";
    }

    public String sync() {
        String message = syncEngine.sync();
        if (message.startsWith("Sync complete")) {
            refreshAllAlerts(false);
        }
        session.setLastSync(message);
        return message;
    }

    public void markNotificationRead(long id) {
        db.notificationDao().markRead(id, System.currentTimeMillis());
    }

    public AttendanceSession openSession(long courseId) {
        long now = System.currentTimeMillis();
        db.sessionDao().closeExpired(now);
        return db.sessionDao().findOpen(courseId, now);
    }

    private void applyRemoteCheckin(Dtos.CheckinResponse body, User student) {
        if (body.course != null && body.course.lecturerEmail != null) {
            ensureUserStub(body.course.lecturerEmail, body.course.lecturerName, Role.LECTURER);
            Course course = syncEngine.upsertCourse(body.course, false);
            if (course != null && db.enrollmentDao().find(student.id, course.id) == null) {
                addEnrollment(student.id, course.id, false);
            }
            if (course != null && body.session != null) {
                AttendanceSession stored = syncEngine.upsertSession(course, body.session, false);
                long markedAt = body.markedAt == null ? System.currentTimeMillis() : Dates.parseIso(body.markedAt);
                saveRecord(stored.id, student.id, body.status, markedAt, false);
                refreshCourseAlerts(course.id, false);
            }
        }
    }

    private boolean pushManualMark(AttendanceSession found, long studentId, String status) {
        if (!hasToken() || found.pendingSync) {
            return false;
        }
        User student = db.userDao().findById(studentId);
        if (student == null) {
            return false;
        }
        try {
            Dtos.CheckinRequest body = new Dtos.CheckinRequest();
            body.qrToken = found.qrToken;
            body.studentEmail = student.email;
            body.status = status;
            Response<Dtos.CheckinResponse> response = api.checkin(body).execute();
            return response.isSuccessful();
        } catch (IOException ex) {
            return false;
        }
    }

    private void saveRecord(long sessionId, long studentId, String status, long markedAt, boolean pending) {
        AttendanceRecord existing = db.recordDao().find(sessionId, studentId);
        if (existing == null) {
            AttendanceRecord record = new AttendanceRecord();
            record.sessionId = sessionId;
            record.studentId = studentId;
            record.status = status;
            record.markedAt = markedAt;
            record.pendingSync = pending;
            db.recordDao().insert(record);
        } else {
            existing.status = status;
            existing.markedAt = markedAt;
            existing.pendingSync = pending || existing.pendingSync && pending;
            if (!pending) {
                existing.pendingSync = false;
            }
            db.recordDao().update(existing);
        }
    }

    private void refreshAllAlerts(boolean pending) {
        for (Course course : db.courseDao().all()) {
            refreshCourseAlerts(course.id, pending);
        }
    }

    private void refreshCourseAlerts(long courseId, boolean pending) {
        Course course = db.courseDao().findById(courseId);
        if (course == null) {
            return;
        }
        User lecturer = db.userDao().findById(course.lecturerId);
        List<User> admins = db.userDao().byRole(Role.ADMIN);
        for (RosterStudent student : db.reportDao().rosterNow(courseId, 0)) {
            boolean low = AttendanceMath.isLow(student.present, student.late, student.absent);
            String studentMessage = AttendanceMath.studentMessage(course.code, student.present, student.late, student.absent);
            String staffMessage = AttendanceMath.lecturerMessage(student.name, course.code, student.present, student.late, student.absent);
            upsertAlert(student.studentId, course.id, student.email, low, studentMessage, pending);
            if (lecturer != null) {
                upsertAlert(lecturer.id, course.id, student.email, low, staffMessage, pending);
            }
            for (User admin : admins) {
                upsertAlert(admin.id, course.id, student.email, low, staffMessage, pending);
            }
        }
    }

    private void upsertAlert(long userId, long courseId, String subjectEmail, boolean low, String message, boolean pending) {
        Notification existing = db.notificationDao().findUnread(userId, courseId, "LOW_ATTENDANCE", subjectEmail);
        if (!low) {
            if (existing != null) {
                db.notificationDao().deleteById(existing.id);
            }
            return;
        }
        if (existing != null && message.equals(existing.message)) {
            return;
        }
        if (existing != null) {
            db.notificationDao().deleteById(existing.id);
        }
        Notification notification = new Notification();
        notification.userId = userId;
        notification.courseId = courseId;
        notification.subjectEmail = subjectEmail;
        notification.type = "LOW_ATTENDANCE";
        notification.message = message;
        notification.createdAt = System.currentTimeMillis();
        notification.pendingSync = pending;
        db.notificationDao().insert(notification);
    }

    private User cacheLoggedInUser(Dtos.UserDto dto, String password) {
        String email = dto.email.trim().toLowerCase(Locale.US);
        User user = db.userDao().findByEmail(email);
        if (user == null) {
            user = new User();
            user.email = email;
            user.name = dto.name;
            user.role = Role.normalize(dto.role);
            user.studentNumber = dto.studentNumber;
            user.passwordHash = PasswordHasher.hash(password);
            user.pendingSync = false;
            user.id = db.userDao().insert(user);
        } else {
            user.name = dto.name;
            user.role = Role.normalize(dto.role);
            user.studentNumber = dto.studentNumber;
            user.passwordHash = PasswordHasher.hash(password);
            user.pendingPassword = null;
            user.pendingSync = false;
            db.userDao().update(user);
        }
        return user;
    }

    private void ensureUserStub(String email, String name, String role) {
        String normalized = email.trim().toLowerCase(Locale.US);
        if (db.userDao().findByEmail(normalized) != null) {
            return;
        }
        User user = new User();
        user.email = normalized;
        user.name = name == null || name.isEmpty() ? normalized : name;
        user.role = role;
        user.passwordHash = PasswordHasher.hash(UUID.randomUUID().toString());
        user.pendingSync = false;
        db.userDao().insert(user);
    }

    private long addUser(String name, String email, String password, String role, String studentNumber, boolean pending) {
        User user = new User();
        user.name = name;
        user.email = email;
        user.passwordHash = PasswordHasher.hash(password);
        user.pendingPassword = pending ? password : null;
        user.role = role;
        user.studentNumber = studentNumber;
        user.pendingSync = pending;
        return db.userDao().insert(user);
    }

    private long addCourse(String code, String title, long lecturerId, String room, boolean pending) {
        Course course = new Course();
        course.code = code;
        course.title = title;
        course.lecturerId = lecturerId;
        course.room = room;
        course.pendingSync = pending;
        return db.courseDao().insert(course);
    }

    private void addEnrollment(long studentId, long courseId, boolean pending) {
        Enrollment enrollment = new Enrollment();
        enrollment.studentId = studentId;
        enrollment.courseId = courseId;
        enrollment.pendingSync = pending;
        db.enrollmentDao().insert(enrollment);
    }

    private void addClosedSession(long courseId, String token, long startedAt, long firstStudent, long secondStudent, String firstStatus, String secondStatus) {
        AttendanceSession stored = new AttendanceSession();
        stored.courseId = courseId;
        stored.qrToken = token;
        stored.startedAt = startedAt;
        stored.expiresAt = startedAt + AttendanceMath.SESSION_LENGTH_MS;
        stored.status = "CLOSED";
        stored.pendingSync = false;
        stored.id = db.sessionDao().insert(stored);
        saveRecord(stored.id, firstStudent, firstStatus, startedAt + offset(firstStatus), false);
        saveRecord(stored.id, secondStudent, secondStatus, startedAt + offset(secondStatus), false);
    }

    private long offset(String status) {
        if ("ABSENT".equals(status)) {
            return 20L * 60L * 1000L;
        }
        if ("LATE".equals(status)) {
            return 12L * 60L * 1000L;
        }
        return 5L * 60L * 1000L;
    }

    private String assertCanManage(Course course) {
        User actor = currentUser();
        if (actor == null) {
            return "Log in again.";
        }
        if (course == null) {
            return "Course not found.";
        }
        if (Role.ADMIN.equals(actor.role)) {
            return null;
        }
        if (Role.LECTURER.equals(actor.role) && course.lecturerId == actor.id) {
            return null;
        }
        return "You do not have permission to change this course.";
    }

    private String assertLecturerOwner(Course course) {
        User actor = currentUser();
        if (actor == null) {
            return "Log in again.";
        }
        if (course == null) {
            return "Course not found.";
        }
        if (Role.LECTURER.equals(actor.role) && course.lecturerId == actor.id) {
            return null;
        }
        return "Only the lecturer who owns this course can start a session.";
    }

    private User currentUser() {
        return db.userDao().findById(session.getUserId());
    }

    private boolean hasToken() {
        return session.getToken() != null && !session.getToken().isEmpty();
    }

    private static String first(String... errors) {
        for (String error : errors) {
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    private static String label(String status) {
        if (status == null) {
            return "recorded";
        }
        return status.toLowerCase(Locale.US);
    }
}
