package com.northrise.campustrack.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.northrise.campustrack.model.Course;

import java.util.List;

@Dao
public interface CourseDao {
    @Insert
    long insert(Course course);

    @Update
    void update(Course course);

    @Query("SELECT * FROM courses WHERE id = :id LIMIT 1")
    Course findById(long id);

    @Query("SELECT * FROM courses WHERE code = :code LIMIT 1")
    Course findByCode(String code);

    @Query("SELECT * FROM courses WHERE pendingSync = 1")
    List<Course> pending();

    @Query("SELECT * FROM courses")
    List<Course> all();
}
