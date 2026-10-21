package com.northrise.campustrack.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.northrise.campustrack.model.AttendanceSession;

import java.util.List;

@Dao
public interface SessionDao {
    @Insert
    long insert(AttendanceSession session);

    @Update
    void update(AttendanceSession session);

    @Query("SELECT * FROM attendance_sessions WHERE id = :id LIMIT 1")
    AttendanceSession findById(long id);

    @Query("SELECT * FROM attendance_sessions WHERE qrToken = :token LIMIT 1")
    AttendanceSession findByToken(String token);

    @Query("SELECT * FROM attendance_sessions WHERE courseId = :courseId AND status = 'OPEN' AND expiresAt > :now ORDER BY startedAt DESC LIMIT 1")
    AttendanceSession findOpen(long courseId, long now);

    @Query("UPDATE attendance_sessions SET status = 'CLOSED' WHERE status = 'OPEN' AND expiresAt <= :now")
    void closeExpired(long now);

    @Query("SELECT * FROM attendance_sessions WHERE pendingSync = 1")
    List<AttendanceSession> pending();
}
