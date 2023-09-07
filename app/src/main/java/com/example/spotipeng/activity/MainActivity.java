package com.example.spotipeng.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spotipeng.R;
import com.example.spotipeng.SongAdapter;
import com.example.spotipeng.api.AuthInterceptor;
import com.example.spotipeng.api.JsonPlaceHolderApi;
import com.example.spotipeng.events.GetCurrentSongEvent;
import com.example.spotipeng.events.GetSongListEvent;
import com.example.spotipeng.events.MusicPlaybackStoppedEvent;
import com.example.spotipeng.events.StartFragmentEvent;
import com.example.spotipeng.fragment.MiniPlayerFragment;
import com.example.spotipeng.model.Song;
import com.example.spotipeng.model.User;
import com.example.spotipeng.service.MusicService;
import com.example.spotipeng.utils.Constants;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import jp.wasabeef.recyclerview.adapters.ScaleInAnimationAdapter;
import okhttp3.OkHttpClient;
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
    User user = new User("John Doe", "Example@mail.com");
    private Uri currentSongArtwork;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    TextView nameTV;
    TextView emailTV;
    // Define constants for sorting options
    private static final int SORT_BY_TITLE = 1;
    private static final int SORT_BY_ARTIST = 2;
    private static final int ASC = 1;
    private static final int DESC = 2;
    private static final int LIST = 1;
    private static final int GRID = 2;

    // Define a variable to keep track of the current sorting option
    private int currentSortOption = SORT_BY_TITLE;
    private int currentAlphabetOption = ASC;
    private int currentViewOption = LIST;
    private int spanCount = 2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        SharedPreferences sharedPreferences = getSharedPreferences("spotipeng", Context.MODE_PRIVATE);;
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(sharedPreferences)) // Pass your SharedPreferences here
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        jsonPlaceHolderApi = retrofit.create(JsonPlaceHolderApi.class);
        fetchSongs();
        fetchUser();
        setContentView(R.layout.activity_main);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        spanCount = screenWidth / 200;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getResources().getString(R.string.app_name));

        drawerLayout = findViewById(R.id.drawerLayout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open_drawer, R.string.close_drawer);
        actionBarDrawerToggle.getDrawerArrowDrawable().setColor(getResources().getColor(R.color.white));
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set up the navigation drawer
        NavigationView navigationView = findViewById(R.id.navigationView);
        nameTV = navigationView.getHeaderView(0).findViewById(R.id.NavbarName);
        emailTV = navigationView.getHeaderView(0).findViewById(R.id.NavbarEmail);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.menu_logout) {
                    // Handle the Logout action here
                    logout();
                }
                // Close the drawer
                drawerLayout.closeDrawers();
                return true;
            }
        });

        Button sortButton = findViewById(R.id.sortButton);

// Create a PopupMenu
        PopupMenu popupMenu = new PopupMenu(this, sortButton);

// Inflate the menu resource (menu_sort.xml) containing the sorting options
        popupMenu.getMenuInflater().inflate(R.menu.sort_menu, popupMenu.getMenu());

// Set an item click listener for the menu items
        popupMenu.setOnMenuItemClickListener(item -> {
            if (currentSortOption == SORT_BY_TITLE) {
                if (item.getItemId() == R.id.sortByArtist) {
                    // Sort by artist
                    onSortOptionSelected(SORT_BY_ARTIST, currentAlphabetOption);
                }
            } else if (currentSortOption == SORT_BY_ARTIST) {
                if (item.getItemId() == R.id.sortByTitle) {
                    // Sort by title
                    onSortOptionSelected(SORT_BY_TITLE, currentAlphabetOption);
                }
            }
            return true; // Return true to indicate that the item click has been handled
        });

        Button sortAlphabetButton = findViewById(R.id.sortAlphabetButton);

        PopupMenu popupAlphabetMenu = new PopupMenu(this, sortAlphabetButton);

// Inflate the menu resource (menu_sort.xml) containing the sorting options
        popupAlphabetMenu.getMenuInflater().inflate(R.menu.sort_alphabet_menu, popupAlphabetMenu.getMenu());

// Set an item click listener for the menu items
        popupAlphabetMenu.setOnMenuItemClickListener(item -> {
            if (currentAlphabetOption == ASC) {
                if (item.getItemId() == R.id.sortDesc) {
                    // Sort by artist
                    onSortOptionSelected(currentSortOption, DESC);
                }
            } else if (currentAlphabetOption == DESC) {
                if (item.getItemId() == R.id.sortAsc) {
                    // Sort by title
                    onSortOptionSelected(currentSortOption ,ASC);
                }
            }
            return true; // Return true to indicate that the item click has been handled
        });
        ImageButton listViewButton = findViewById(R.id.listViewButton);
        ImageButton gridViewButton = findViewById(R.id.gridViewButton);
        currentViewOption = sharedPreferences.getInt("viewmode", LIST);
        if (currentViewOption == GRID){
            listViewButton.setImageResource(R.drawable.ic_listview);
            gridViewButton.setImageResource(R.drawable.ic_grid_on);
        }
        listViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentViewOption == GRID){
                    currentViewOption = LIST;
                    getViewMode();
                    listViewButton.setImageResource(R.drawable.ic_listview_on);
                    gridViewButton.setImageResource(R.drawable.ic_grid);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("viewmode", LIST);
                    editor.apply();
                }
            }
        });


        gridViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentViewOption == LIST){
                    currentViewOption = GRID;
                    getViewMode();
                    listViewButton.setImageResource(R.drawable.ic_listview);
                    gridViewButton.setImageResource(R.drawable.ic_grid_on);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("viewmode", GRID);
                    editor.apply();
                }
            }
        });

        sortButton.setOnClickListener(v -> popupMenu.show());
        sortAlphabetButton.setOnClickListener(v -> popupAlphabetMenu.show());

        recyclerView = findViewById(R.id.recyclerview);
        FragmentManager fragmentManager = getSupportFragmentManager();
        miniPlayerFragment = (MiniPlayerFragment) fragmentManager.findFragmentById(R.id.miniPlayerContainer);
        Intent serviceIntent = new Intent(this, MusicService.class);
        this.startService(serviceIntent);
    }

    private void fetchSongs() {
        Call<JsonObject> call = jsonPlaceHolderApi.getSongs(); // Assuming your API call returns a JsonObject
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Log.i("Get Song", "Code: " + response.code());
                    return;
                }

                JsonObject jsonObject = response.body(); // Get the entire JSON response

                if (jsonObject != null && jsonObject.has("song")) {
                    JsonArray songArray = jsonObject.get("song").getAsJsonArray(); // Access the "song" array

                    for (JsonElement element : songArray) {
                        JsonObject songObject = element.getAsJsonObject();

                        String singer = songObject.get("singer").getAsString();
                        String title = songObject.get("title").getAsString();
                        String url = Constants.Song_URL + songObject.get("url").getAsString();
                        String album = songObject.get("album").getAsString();

                        Song currentSong = new Song(singer, title, url, album);
                        songs.add(currentSong);
                    }

                    showSongs(currentViewOption);
                    EventBus.getDefault().post(new GetSongListEvent(allSongs));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.i("Get Song", "Call failed " + t);
            }
        });
    }

    private void fetchUser() {
        Call<JsonObject> call = jsonPlaceHolderApi.getUser(); // Assuming your API call returns a JsonObject
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Log.i("Get Song", "Code: " + response.code());
                    return;
                }

                JsonObject jsonObject = response.body(); // Get the entire JSON response

                if (jsonObject != null && jsonObject.has("user")) {
                    JsonObject userObject = jsonObject.getAsJsonObject("user"); // Assuming "user" is a nested object

                    User user = new User(userObject.get("name").getAsString(), userObject.get("email").getAsString());
                    nameTV.setText(user.getName());
                    emailTV.setText(user.getEmail());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.i("Get Song", "Call failed " + t);
            }
        });
    }

    public void showSongs(int currentViewOption) {
        if (songs.size() == 0){
            Toast.makeText(this, "No Songs", Toast.LENGTH_SHORT).show();
            return;
        }
        allSongs.clear();
        allSongs.addAll(songs);
        String title = getResources().getString(R.string.app_name);
        Objects.requireNonNull(getSupportActionBar()).setTitle(title);
        getViewMode();
        onSortOptionSelected(currentSortOption, currentAlphabetOption);
        EventBus.getDefault().post(new GetSongListEvent(allSongs));
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
        overridePendingTransition(R.anim.slide_in, 1);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Function to sort the songs based on the current sorting option
    private void sortSongs() {
        switch (currentSortOption) {
            case SORT_BY_TITLE:
                // Sort by Title
                Collections.sort(allSongs, new Comparator<Song>() {
                    @Override
                    public int compare(Song song1, Song song2) {
                        if (currentAlphabetOption == ASC){
                            return song1.getTitle().compareToIgnoreCase(song2.getTitle());
                        } else {
                            return song2.getTitle().compareToIgnoreCase(song1.getTitle());
                        }

                    }
                });
                break;
            case SORT_BY_ARTIST:
                // Sort by Artist
                Collections.sort(allSongs, new Comparator<Song>() {
                    @Override
                    public int compare(Song song1, Song song2) {
                        if (currentAlphabetOption == ASC){
                            return song1.getSinger().compareToIgnoreCase(song2.getSinger());
                        } else {
                            return song2.getSinger().compareToIgnoreCase(song1.getSinger());
                        }
                    }
                });
                break;
        }
    }


    // Function to handle sorting option selection
    private void onSortOptionSelected(int sortOption, int alphabetOption) {
        currentSortOption = sortOption;
        currentAlphabetOption = alphabetOption;
        sortSongs();
        songAdapter.notifyDataSetChanged(); // Notify the adapter that the data has changed
    }

    private void getViewMode() {
        if (currentViewOption == LIST){
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(layoutManager);
        } else {
            GridLayoutManager layoutManager = new GridLayoutManager(this, spanCount);
            recyclerView.setLayoutManager(layoutManager);
        }
        songAdapter = new SongAdapter(this, allSongs, currentViewOption);
        recyclerView.setAdapter(songAdapter);
        ScaleInAnimationAdapter scaleInAnimationAdapter = new ScaleInAnimationAdapter(songAdapter);
        scaleInAnimationAdapter.setDuration(1000);
        scaleInAnimationAdapter.setInterpolator(new OvershootInterpolator());
        scaleInAnimationAdapter.setFirstOnly(false);
        recyclerView.setAdapter(scaleInAnimationAdapter);
    }

    private void logout() {
        SharedPreferences sharedPreferences = getSharedPreferences("spotipeng", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        EventBus.getDefault().post(new MusicPlaybackStoppedEvent());
        // Redirect to the login activity or perform other necessary actions
        Intent loginIntent = new Intent(this, LoginActivity.class);
        startActivity(loginIntent);
        finish(); // Close the current activity
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
