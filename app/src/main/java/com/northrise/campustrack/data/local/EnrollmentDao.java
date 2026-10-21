package com.northrise.campustrack.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.northrise.campustrack.model.Enrollment;

import java.util.List;

@Dao
public interface EnrollmentDao {
    @Insert
    long insert(Enrollment enrollment);

    @Update
    void update(Enrollment enrollment);

    @Query("SELECT * FROM enrollments WHERE studentId = :studentId AND courseId = :courseId LIMIT 1")
    Enrollment find(long studentId, long courseId);

    @Query("SELECT * FROM enrollments WHERE pendingSync = 1")
    List<Enrollment> pending();

    @Query("SELECT * FROM enrollments")
    List<Enrollment> all();
}
