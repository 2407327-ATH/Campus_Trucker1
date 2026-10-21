package com.northrise.campustrack.data.remote;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {
    @POST("auth/register")
    Call<Dtos.LoginResponse> register(@Body Dtos.RegisterRequest body);

    @POST("auth/login")
    Call<Dtos.LoginResponse> login(@Body Dtos.LoginRequest body);

    @GET("auth/me")
    Call<Dtos.UserDto> me();

    @GET("courses")
    Call<java.util.List<Dtos.CourseDto>> courses();

    @POST("courses")
    Call<Dtos.CourseDto> createCourse(@Body Dtos.CourseDto body);

    @POST("sessions")
    Call<Dtos.SessionDto> startSession(@Body Dtos.SessionCreate body);

    @POST("attendance/checkin")
    Call<Dtos.CheckinResponse> checkin(@Body Dtos.CheckinRequest body);

    @POST("sync/batch")
    Call<Dtos.SyncResult> sync(@Body Dtos.SyncPayload body);

    @GET("notifications")
    Call<java.util.List<Dtos.NotificationDto>> notifications();
}
