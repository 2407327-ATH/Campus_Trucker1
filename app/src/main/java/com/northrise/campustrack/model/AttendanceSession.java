package com.northrise.campustrack.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "attendance_sessions",
        foreignKeys = @ForeignKey(entity = Course.class, parentColumns = "id", childColumns = "courseId"),
        indices = {@Index("courseId"), @Index(value = "qrToken", unique = true)}
)
public class AttendanceSession {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long courseId;
    public String qrToken;
    public long startedAt;
    public long expiresAt;
    public String status;
    public boolean pendingSync;
}
