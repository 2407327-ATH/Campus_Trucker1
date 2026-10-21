package com.northrise.campustrack.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "notifications", indices = {@Index("userId")})
public class Notification {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long userId;
    public long courseId;
    public String subjectEmail;
    public String type;
    public String message;
    public Long readAt;
    public long createdAt;
    public boolean pendingSync;
}
