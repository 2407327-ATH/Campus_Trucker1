package com.northrise.campustrack.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.northrise.campustrack.model.AttendanceRecord;

import java.util.List;

@Dao
public interface RecordDao {
    @Insert
    long insert(AttendanceRecord record);

    @Update
    void update(AttendanceRecord record);

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId AND studentId = :studentId LIMIT 1")
    AttendanceRecord find(long sessionId, long studentId);

    @Query("SELECT * FROM attendance_records WHERE pendingSync = 1")
    List<AttendanceRecord> pending();
}
