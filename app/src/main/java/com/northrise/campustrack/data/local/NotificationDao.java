package com.northrise.campustrack.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.northrise.campustrack.model.Notification;

import java.util.List;

@Dao
public interface NotificationDao {
    @Insert
    long insert(Notification notification);

    @Update
    void update(Notification notification);

    @Query("DELETE FROM notifications WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM notifications WHERE userId = :userId AND courseId = :courseId AND type = :type AND subjectEmail = :subjectEmail AND readAt IS NULL LIMIT 1")
    Notification findUnread(long userId, long courseId, String type, String subjectEmail);

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    LiveData<List<Notification>> observeForUser(long userId);

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND readAt IS NULL")
    LiveData<Integer> observeUnread(long userId);

    @Query("UPDATE notifications SET readAt = :readAt WHERE id = :id AND readAt IS NULL")
    void markRead(long id, long readAt);
}
