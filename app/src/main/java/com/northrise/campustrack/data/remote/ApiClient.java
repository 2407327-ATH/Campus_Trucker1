package com.northrise.campustrack.data.remote;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.northrise.campustrack.data.SessionManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    public static final String BASE_URL = "http://10.0.2.2:3000/api/";

    private ApiClient() {
    }

    public static ApiService create(SessionManager session) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    String token = session.getToken();
                    if (token != null && !token.isEmpty()) {
                        request = request.newBuilder()
                                .header("Authorization", "Bearer " + token)
                                .build();
                    }
                    return chain.proceed(request);
                })
                .addInterceptor(logging)
                .build();
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
    }

    public static String errorMessage(Response<?> response, String fallback) {
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                JsonObject object = JsonParser.parseString(raw).getAsJsonObject();
                if (object.has("error") && !object.get("error").isJsonNull()) {
                    return object.get("error").getAsString();
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }
}
