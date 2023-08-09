package com.example.spotipeng.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spotipeng.R;
import com.example.spotipeng.SongAdapter;
import com.example.spotipeng.api.JsonPlaceHolderApi;
import com.example.spotipeng.events.GetCurrentSongEvent;
import com.example.spotipeng.events.GetSongListEvent;
import com.example.spotipeng.events.MusicPlaybackStartedEvent;
import com.example.spotipeng.events.StartFragmentEvent;
import com.example.spotipeng.fragment.MiniPlayerFragment;
import com.example.spotipeng.model.Song;
import com.example.spotipeng.service.MusicService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

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
    private Song currentPlayingSong;
    List<Song> allSongs = new ArrayList<>();
    ActivityResultLauncher<String> storagePermissionLauncher;
    private boolean isMusicPlaying = false;
    private MiniPlayerFragment miniPlayerFragment;
    private String currentSongTitle;
    private String currentSongArtist;
    List<Song> songs = new ArrayList<>();

    private Uri currentSongArtwork;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://64c1e509fa35860baea0ed02.mockapi.io/androidcourse/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        jsonPlaceHolderApi = retrofit.create(JsonPlaceHolderApi.class);
        fetchSongs();
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getResources().getString(R.string.app_name));

        recyclerView = findViewById(R.id.recyclerview);
        FragmentManager fragmentManager = getSupportFragmentManager();
        miniPlayerFragment = (MiniPlayerFragment) fragmentManager.findFragmentById(R.id.miniPlayerContainer);
        Intent serviceIntent = new Intent(this, MusicService.class);
        this.startService(serviceIntent);
    }

    private void fetchSongs() {

        Call<List<Song>> call = jsonPlaceHolderApi.getSongs();
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

                    songs.add(song);
                }
                showSongs();
                EventBus.getDefault().post(new GetSongListEvent(songs));
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                Log.i("Get Song", "Call failed " + t);
            }
        });
    }
    public void showSongs() {
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
    @Subscribe
    public void onStartFragment(StartFragmentEvent event) {
        // Call startMusicPlayback() when music starts playing
        showMiniPlayerFragment();
    }

    @Subscribe
    public void onCurrentSongEvent(GetCurrentSongEvent event) {
        Song currentSong = event.getSong();
        Intent intent = new Intent(this, SongDetailActivity.class);
        intent.putExtra("song", currentSong); // Pass the currentSong to the SongDetailActivity
        startActivity(intent);
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
