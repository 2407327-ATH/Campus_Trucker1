package com.northrise.campustrack.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.northrise.campustrack.model.AdminStats;
import com.northrise.campustrack.model.CourseStat;
import com.northrise.campustrack.model.HistoryRow;
import com.northrise.campustrack.model.RiskRow;
import com.northrise.campustrack.model.RosterStudent;

import java.util.List;

@Dao
public interface ReportDao {
    @Query("SELECT c.id AS courseId, c.code AS code, c.title AS title, c.room AS room, u.name AS lecturerName, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = c.id AND ar.studentId = :studentId AND ar.status = 'PRESENT') AS present, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = c.id AND ar.studentId = :studentId AND ar.status = 'LATE') AS late, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = c.id AND ar.studentId = :studentId AND ar.status = 'ABSENT') AS absent " +
            "FROM courses c INNER JOIN enrollments e ON e.courseId = c.id AND e.studentId = :studentId " +
            "INNER JOIN users u ON u.id = c.lecturerId ORDER BY c.code")
    LiveData<List<CourseStat>> observeStudentCourses(long studentId);

    @Query("SELECT c.id AS courseId, c.code AS code, c.title AS title, c.room AS room, u.name AS lecturerName, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = c.id AND ar.studentId = :studentId AND ar.status = 'PRESENT') AS present, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = c.id AND ar.studentId = :studentId AND ar.status = 'LATE') AS late, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = c.id AND ar.studentId = :studentId AND ar.status = 'ABSENT') AS absent " +
            "FROM courses c INNER JOIN enrollments e ON e.courseId = c.id AND e.studentId = :studentId " +
            "INNER JOIN users u ON u.id = c.lecturerId WHERE c.id = :courseId LIMIT 1")
    LiveData<CourseStat> observeStudentCourse(long studentId, long courseId);

    @Query("SELECT c.id AS courseId, c.code AS code, c.title AS title, c.room AS room, u.name AS lecturerName, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = c.id AND ar.status = 'PRESENT') AS present, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = c.id AND ar.status = 'LATE') AS late, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = c.id AND ar.status = 'ABSENT') AS absent " +
            "FROM courses c INNER JOIN users u ON u.id = c.lecturerId " +
            "WHERE (:isAdmin = 1 OR c.lecturerId = :lecturerId) ORDER BY c.code")
    LiveData<List<CourseStat>> observeManagedCourses(long lecturerId, int isAdmin);

    @Query("SELECT c.id AS courseId, c.code AS code, c.title AS title, c.room AS room, u.name AS lecturerName, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = c.id AND ar.status = 'PRESENT') AS present, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = c.id AND ar.status = 'LATE') AS late, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = c.id AND ar.status = 'ABSENT') AS absent " +
            "FROM courses c INNER JOIN users u ON u.id = c.lecturerId WHERE c.id = :courseId LIMIT 1")
    LiveData<CourseStat> observeClass(long courseId);

    @Query("SELECT u.id AS studentId, u.name AS name, u.email AS email, u.studentNumber AS studentNumber, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = :courseId AND ar.studentId = u.id AND ar.status = 'PRESENT') AS present, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = :courseId AND ar.studentId = u.id AND ar.status = 'LATE') AS late, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = :courseId AND ar.studentId = u.id AND ar.status = 'ABSENT') AS absent, " +
            "(SELECT ar.status FROM attendance_records ar WHERE ar.sessionId = :sessionId AND ar.studentId = u.id LIMIT 1) AS sessionStatus " +
            "FROM users u INNER JOIN enrollments e ON e.studentId = u.id AND e.courseId = :courseId ORDER BY u.name")
    LiveData<List<RosterStudent>> observeRoster(long courseId, long sessionId);

    @Query("SELECT u.id AS studentId, u.name AS name, u.email AS email, u.studentNumber AS studentNumber, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = :courseId AND ar.studentId = u.id AND ar.status = 'PRESENT') AS present, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = :courseId AND ar.studentId = u.id AND ar.status = 'LATE') AS late, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = :courseId AND ar.studentId = u.id AND ar.status = 'ABSENT') AS absent, " +
            "(SELECT ar.status FROM attendance_records ar WHERE ar.sessionId = :sessionId AND ar.studentId = u.id LIMIT 1) AS sessionStatus " +
            "FROM users u INNER JOIN enrollments e ON e.studentId = u.id AND e.courseId = :courseId ORDER BY u.name")
    List<RosterStudent> rosterNow(long courseId, long sessionId);

    @Query("SELECT ar.id AS recordId, c.code AS courseCode, c.title AS courseTitle, ar.status AS status, " +
            "ar.markedAt AS markedAt, s.startedAt AS startedAt " +
            "FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "INNER JOIN courses c ON c.id = s.courseId WHERE ar.studentId = :studentId ORDER BY ar.markedAt DESC")
    LiveData<List<HistoryRow>> observeHistory(long studentId);

    @Query("SELECT ar.id AS recordId, c.code AS courseCode, c.title AS courseTitle, ar.status AS status, " +
            "ar.markedAt AS markedAt, s.startedAt AS startedAt " +
            "FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "INNER JOIN courses c ON c.id = s.courseId WHERE ar.studentId = :studentId AND c.id = :courseId " +
            "ORDER BY ar.markedAt DESC")
    LiveData<List<HistoryRow>> observeCourseHistory(long studentId, long courseId);

    @Query("SELECT u.id AS studentId, u.name AS studentName, c.id AS courseId, c.code AS courseCode, c.title AS courseTitle, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = c.id AND ar.studentId = u.id AND ar.status = 'PRESENT') AS present, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = c.id AND ar.studentId = u.id AND ar.status = 'LATE') AS late, " +
            "(SELECT COUNT(*) FROM attendance_records ar INNER JOIN attendance_sessions s ON s.id = ar.sessionId " +
            "WHERE s.courseId = c.id AND ar.studentId = u.id AND ar.status = 'ABSENT') AS absent " +
            "FROM enrollments e INNER JOIN users u ON u.id = e.studentId INNER JOIN courses c ON c.id = e.courseId " +
            "WHERE (:isAdmin = 1 OR c.lecturerId = :lecturerId) ORDER BY c.code, u.name")
    LiveData<List<RiskRow>> observeRisks(long lecturerId, int isAdmin);

    @Query("SELECT " +
            "(SELECT COUNT(*) FROM users) AS userCount, " +
            "(SELECT COUNT(*) FROM courses) AS courseCount, " +
            "(SELECT COUNT(*) FROM attendance_sessions) AS sessionCount, " +
            "(SELECT COUNT(*) FROM attendance_records) AS recordCount, " +
            "(SELECT COUNT(*) FROM attendance_records WHERE status IN ('PRESENT','LATE')) AS attendedCount")
    LiveData<AdminStats> observeAdminStats();
}
