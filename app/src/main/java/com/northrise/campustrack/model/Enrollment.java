package com.northrise.campustrack.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "enrollments",
        foreignKeys = {
                @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "studentId"),
                @ForeignKey(entity = Course.class, parentColumns = "id", childColumns = "courseId")
        },
        indices = {
                @Index("studentId"),
                @Index("courseId"),
                @Index(value = {"studentId", "courseId"}, unique = true)
        }
)
public class Enrollment {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long studentId;
    public long courseId;
    public boolean pendingSync;
}
