package com.example.spotipeng;

import android.Manifest;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jp.wasabeef.recyclerview.adapters.ScaleInAnimationAdapter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private JsonPlaceHolderApi jsonPlaceHolderApi;
    RecyclerView recyclerView;
    SongAdapter songAdapter;
    List<Song> allSongs = new ArrayList<>();
    ActivityResultLauncher<String> storagePermissionLauncher;
    private boolean isMusicPlaying = false;
    private MiniPlayerFragment miniPlayerFragment;
    private String currentSongTitle;
    private String currentSongArtist;

    private Uri currentSongArtwork;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getResources().getString(R.string.app_name));

        recyclerView = findViewById(R.id.recyclerview);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://6152fa45c465200017d1a8e3.mockapi.io/api/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        jsonPlaceHolderApi = retrofit.create(JsonPlaceHolderApi.class);

        // Set up the MiniPlayerFragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        miniPlayerFragment = (MiniPlayerFragment) fragmentManager.findFragmentById(R.id.miniPlayerContainer);
        fetchSongs();
    }


    private void fetchSongs() {
        List<Song> songs = new ArrayList<>();
        Call<List<Song>> call = jsonPlaceHolderApi.getSongs();

        List<Song> finalSongs = songs;
        call.enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (!response.isSuccessful()){
                    Log.i("Get Song", "Code: " + response.code());
                    return;
                }

                List<Song> jsonSong = response.body();

                for (Song song : jsonSong){
                    new Song(
                            song.getSinger(),
                            song.getTitle(),
                            song.getUrl(),
                            song.getAlbum());

                    finalSongs.add(song);
                }
                showSongs(finalSongs);
                Log.i("TAG", "onResponse: " + jsonSong.get(0).getTitle());
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                Log.i("Get Song", "Call failed " + t);
            }
        });

        Log.i("Songs", "fetchSongs: " + finalSongs);
    }

    private void showSongs(List<Song> songs) {
        Log.i("TAG", "fetchSongs: " + songs);
        if (songs.size() == 0){
            Toast.makeText(this, "No Songs", Toast.LENGTH_SHORT).show();
            return;
        }

        allSongs.clear();
        allSongs.addAll(songs);
        String title = getResources().getString(R.string.app_name);
        Objects.requireNonNull(getSupportActionBar()).setTitle(title);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        songAdapter = new SongAdapter(this, allSongs);
        recyclerView.setAdapter(songAdapter);

        ScaleInAnimationAdapter scaleInAnimationAdapter = new ScaleInAnimationAdapter(songAdapter);
        scaleInAnimationAdapter.setDuration(1000);
        scaleInAnimationAdapter.setInterpolator(new OvershootInterpolator());
        scaleInAnimationAdapter.setFirstOnly(false);
        recyclerView.setAdapter(scaleInAnimationAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.search_btn, menu);

        MenuItem menuItem = menu.findItem(R.id.searchBtn);
        SearchView searchView = (SearchView) menuItem.getActionView();

        SearchSong(searchView);
        return super.onCreateOptionsMenu(menu);
    }

    private void SearchSong(SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSongs(newText.toLowerCase());
                return true;
            }
        });
    }

    private void filterSongs(String query) {
        List<Song> filteredList = new ArrayList<>();

        if (allSongs.size() > 0){
            for (Song song : allSongs){
                if (song.getTitle().toLowerCase().contains(query)){
                    filteredList.add(song);
                }
            }

            if (songAdapter != null){
                songAdapter.filterSongs(filteredList);
            }
        }
    }

    public void onMusicPlaybackStarted(Song song) {
        // Call startMusicPlayback() when music starts playing
        startMusicPlayback(song);
    }

    public void onMusicPlaybackStopped() {
        // Call stopMusicPlayback() when music stops playing
        stopMusicPlayback();
    }

    private void startMusicPlayback(Song song) {
        // Call this method when music starts playing
        isMusicPlaying = true;
        showMiniPlayerFragment();

        if (miniPlayerFragment != null) {
            miniPlayerFragment.updateSongTitle(song.getTitle());
            miniPlayerFragment.updatePlayPauseButton(true);
        }

        // Set the current song title
        currentSongTitle = song.getTitle();
        currentSongArtwork = Uri.parse(song.getAlbum());
        currentSongArtist = song.getSinger();

        // Start playing the music in the SongAdapter
        songAdapter.startMusic();
    }

    private void stopMusicPlayback() {
        // Call this method when music stops playing
        isMusicPlaying = false;
        hideMiniPlayerFragment();

        // Stop the music in the SongAdapter
        songAdapter.stopMusic();
    }

    private void showMiniPlayerFragment() {
        // Show the MiniPlayerFragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        if (miniPlayerFragment == null) {
            miniPlayerFragment = new MiniPlayerFragment();
            transaction.add(R.id.miniPlayerContainer, miniPlayerFragment);
        } else {
            transaction.show(miniPlayerFragment);
        }

        transaction.commit();
    }

    private void hideMiniPlayerFragment() {
        // Hide the MiniPlayerFragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        if (miniPlayerFragment != null) {
            transaction.hide(miniPlayerFragment);
        }

        transaction.commit();
    }

    public SongAdapter getSongAdapter() {
        return songAdapter;
    }

    public String getCurrentSongTitle() {
        return currentSongTitle;
    }

    public String getCurrentArtist() {
        return currentSongArtist;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the playback when the activity is destroyed
        songAdapter.stopPlayback();
    }
}
