package com.northrise.campustrack.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.northrise.campustrack.data.local.CourseDao;
import com.northrise.campustrack.data.local.EnrollmentDao;
import com.northrise.campustrack.data.local.NotificationDao;
import com.northrise.campustrack.data.local.RecordDao;
import com.northrise.campustrack.data.local.ReportDao;
import com.northrise.campustrack.data.local.SessionDao;
import com.northrise.campustrack.data.local.UserDao;
import com.northrise.campustrack.model.AttendanceRecord;
import com.northrise.campustrack.model.AttendanceSession;
import com.northrise.campustrack.model.Course;
import com.northrise.campustrack.model.Enrollment;
import com.northrise.campustrack.model.Notification;
import com.northrise.campustrack.model.User;

@Database(
        entities = {
                User.class,
                Course.class,
                Enrollment.class,
                AttendanceSession.class,
                AttendanceRecord.class,
                Notification.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();

    public abstract CourseDao courseDao();

    public abstract EnrollmentDao enrollmentDao();

    public abstract SessionDao sessionDao();

    public abstract RecordDao recordDao();

    public abstract NotificationDao notificationDao();

    public abstract ReportDao reportDao();

    public static final Callback FOREIGN_KEYS = new Callback() {
        @Override
        public void onOpen(SupportSQLiteDatabase db) {
            super.onOpen(db);
            db.execSQL("PRAGMA foreign_keys = ON");
        }
    };
}
