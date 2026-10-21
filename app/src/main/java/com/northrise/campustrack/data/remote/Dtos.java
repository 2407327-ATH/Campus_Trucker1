package com.northrise.campustrack.data.remote;

import java.util.ArrayList;
import java.util.List;

public final class Dtos {
    private Dtos() {
    }

    public static class LoginRequest {
        public String email;
        public String password;

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    public static class RegisterRequest {
        public String name;
        public String email;
        public String password;
        public String role;
        public String studentNumber;

        public RegisterRequest(String name, String email, String password, String role, String studentNumber) {
            this.name = name;
            this.email = email;
            this.password = password;
            this.role = role;
            this.studentNumber = studentNumber;
        }
    }

    public static class LoginResponse {
        public String token;
        public UserDto user;
    }

    public static class UserDto {
        public long id;
        public String name;
        public String email;
        public String role;
        public String studentNumber;
        public String password;
    }

    public static class CourseDto {
        public long id;
        public String code;
        public String title;
        public String room;
        public String lecturerEmail;
        public String lecturerName;
    }

    public static class EnrollmentDto {
        public String studentEmail;
        public String courseCode;
    }

    public static class SessionCreate {
        public String courseCode;
    }

    public static class SessionDto {
        public long id;
        public String courseCode;
        public String qrToken;
        public String startedAt;
        public String expiresAt;
        public String status;
    }

    public static class CheckinRequest {
        public String qrToken;
        public String studentEmail;
        public String status;
    }

    public static class CheckinResponse {
        public String status;
        public String markedAt;
        public SessionDto session;
        public CourseDto course;
    }

    public static class SyncPayload {
        public List<UserDto> users = new ArrayList<>();
        public List<CourseDto> courses = new ArrayList<>();
        public List<EnrollmentDto> enrollments = new ArrayList<>();
        public List<SessionDto> sessions = new ArrayList<>();
        public List<RecordDto> records = new ArrayList<>();
    }

    public static class RecordDto {
        public String qrToken;
        public String studentEmail;
        public String status;
        public String markedAt;
    }

    public static class NotificationDto {
        public String type;
        public String message;
        public String courseCode;
        public String subjectEmail;
        public String createdAt;
        public String readAt;
    }

    public static class SyncSaved {
        public List<String> users = new ArrayList<>();
        public List<String> courses = new ArrayList<>();
        public List<String> enrollments = new ArrayList<>();
        public List<String> sessions = new ArrayList<>();
        public List<String> records = new ArrayList<>();
    }

    public static class Snapshot {
        public List<UserDto> users = new ArrayList<>();
        public List<CourseDto> courses = new ArrayList<>();
        public List<EnrollmentDto> enrollments = new ArrayList<>();
        public List<SessionDto> sessions = new ArrayList<>();
        public List<RecordDto> records = new ArrayList<>();
        public List<NotificationDto> notifications = new ArrayList<>();
    }

    public static class SyncResult {
        public List<String> errors = new ArrayList<>();
        public SyncSaved saved = new SyncSaved();
        public Snapshot snapshot = new Snapshot();
    }
}
