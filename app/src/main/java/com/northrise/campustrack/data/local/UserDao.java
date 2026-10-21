package com.northrise.campustrack.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.northrise.campustrack.model.User;

import java.util.List;

@Dao
public interface UserDao {
    @Insert
    long insert(User user);

    @Update
    void update(User user);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User findById(long id);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User findByEmail(String email);

    @Query("SELECT COUNT(*) FROM users")
    int count();

    @Query("SELECT * FROM users WHERE role = :role ORDER BY name")
    List<User> byRole(String role);

    @Query("SELECT * FROM users ORDER BY name")
    LiveData<List<User>> observeAll();

    @Query("SELECT * FROM users WHERE pendingSync = 1")
    List<User> pending();
}
