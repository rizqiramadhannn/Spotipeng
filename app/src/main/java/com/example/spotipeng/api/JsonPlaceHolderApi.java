package com.example.spotipeng.api;

import com.example.spotipeng.model.Song;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface JsonPlaceHolderApi {
    @GET("music")
    Call<List<Song>> getSongs();
}
