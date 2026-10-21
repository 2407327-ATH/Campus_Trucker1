package com.northrise.campustrack.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "attendance_records",
        foreignKeys = {
                @ForeignKey(entity = AttendanceSession.class, parentColumns = "id", childColumns = "sessionId"),
                @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "studentId")
        },
        indices = {
                @Index("sessionId"),
                @Index("studentId"),
                @Index(value = {"sessionId", "studentId"}, unique = true)
        }
)
public class AttendanceRecord {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long sessionId;
    public long studentId;
    public String status;
    public long markedAt;
    public boolean pendingSync;
}
