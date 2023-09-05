package com.example.spotipeng.api;

import com.example.spotipeng.model.LoginPayload;
import com.example.spotipeng.model.LoginResponse;
import com.example.spotipeng.model.RegisterPayload;
import com.example.spotipeng.model.Song;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface JsonPlaceHolderApi {
    @GET("spotipeng/api/v1/song")
    Call<JsonObject> getSongs();

    @GET("spotipeng/api/v1/users")
    Call<JsonObject> getUser();

    @POST("spotipeng/api/v1/users/login")
    Call<LoginResponse> login(@Body LoginPayload loginPayload);

    @POST("spotipeng/api/v1/register")
    Call<String> register(@Body RegisterPayload registerPayload);
}
