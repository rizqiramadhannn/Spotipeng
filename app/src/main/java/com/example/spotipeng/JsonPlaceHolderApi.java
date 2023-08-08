package com.example.spotipeng;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface JsonPlaceHolderApi {
    @GET("spotipi")
    Call<List<Song>> getSongs();
}
