package com.example.spotipeng.api;

import com.example.spotipeng.model.LoginPayload;
import com.example.spotipeng.model.LoginResponse;
import com.example.spotipeng.model.RegisterPayload;
import com.example.spotipeng.model.Song;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface JsonPlaceHolderApi {
    @GET("song")
    Call<List<Song>> getSongs();

    @POST("login")
    Call<LoginResponse> login(@Body LoginPayload loginPayload);

    @POST("register")
    Call<String> register(@Body RegisterPayload registerPayload);
}
