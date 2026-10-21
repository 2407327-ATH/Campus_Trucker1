package com.northrise.campustrack.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "courses",
        foreignKeys = @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "lecturerId"),
        indices = {@Index("lecturerId"), @Index(value = "code", unique = true)}
)
public class Course {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String code;
    public String title;
    public long lecturerId;
    public String room;
    public boolean pendingSync;
}
