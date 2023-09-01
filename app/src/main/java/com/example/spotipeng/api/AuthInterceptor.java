package com.example.spotipeng.api;

import android.content.SharedPreferences;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class AuthInterceptor implements Interceptor {
    private final SharedPreferences sharedPreferences;

    public AuthInterceptor(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // Retrieve the token from SharedPreferences
        String token = sharedPreferences.getString("token", "");

        // Add the token as a bearer token to the request header
        Request newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();

        return chain.proceed(newRequest);
    }
}
