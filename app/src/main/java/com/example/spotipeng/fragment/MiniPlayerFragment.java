package com.example.spotipeng.fragment;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;

import com.example.spotipeng.events.GetCurrentSongEvent;
import com.example.spotipeng.events.MiniplayerFragmentRestarted;
import com.example.spotipeng.events.UpdatePlaybackPositionEvent;
import com.example.spotipeng.service.MusicService;
import com.example.spotipeng.R;
import com.example.spotipeng.activity.SongDetailActivity;
import com.example.spotipeng.events.MusicPlaybackPausedEvent;
import com.example.spotipeng.events.MusicPlaybackResumedEvent;
import com.example.spotipeng.events.MusicPlaybackStartedEvent;
import com.example.spotipeng.events.MusicPlaybackStoppedEvent;
import com.example.spotipeng.model.Song;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class MiniPlayerFragment extends Fragment {

    private TextView songTitleTextView;
    private TextView artistTextView;
    private ImageView artworkView;
    private ImageButton playPauseButton;
    private boolean isPlaying = false;
    private Song currentSong;
    private String title;
    private String artist;
    private String album;
    private ConstraintLayout layout;
    private ConstraintLayout fragmentlayout;
    private SharedPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Register the fragment as an EventBus subscriber
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister the fragment from EventBus when it's destroyed
        EventBus.getDefault().unregister(this);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_mini_player, container, false);
        songTitleTextView = rootView.findViewById(R.id.titleView);
        artistTextView = rootView.findViewById(R.id.artistView);
        playPauseButton = rootView.findViewById(R.id.playPauseButton);
        artworkView = rootView.findViewById(R.id.artworkView);
        layout = rootView.findViewById(R.id.layout);
        fragmentlayout = rootView.findViewById(R.id.fragmentlayout);
        preferences = getActivity().getSharedPreferences("spotipeng", Context.MODE_PRIVATE);
        isPlaying = preferences.getBoolean("isplaying", false);
        title = preferences.getString("fragmenttitle", "Title");
        artist = preferences.getString("fragmentartist", "Artist");
        album = preferences.getString("fragmentalbum", "");
        if (isPlaying){
            fragmentlayout.setVisibility(View.VISIBLE);
        }
        // Set click listener for the play/pause button
        playPauseButton.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(getContext(), MusicService.class);
            if (isPlaying) {
                serviceIntent.setAction("PAUSE");
            } else {
                // Start playing the music
                serviceIntent.setAction("RESUME");
            }
            getContext().startService(serviceIntent);
        });
        if (title != "Title"){
            updateSongTitle(title);
            updateArtist(artist);
            displayAlbum(album);
        }
        // Apply fade-in animation to the root view of the fragment
        Animation fadeInAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in);
        rootView.startAnimation(fadeInAnimation);

        // Set click listener for the MiniPlayer to open the SongDetailActivity
        rootView.setOnClickListener(v -> {
            EventBus.getDefault().post(new GetCurrentSongEvent(currentSong));
        });

        return rootView;
    }

    @Subscribe
    public void onMusicPlaybackPaused(MusicPlaybackPausedEvent event){
        isPlaying = event.getStatus();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isplaying", isPlaying);
        editor.apply();
        updatePlayPauseButton(isPlaying);
    }

    @Subscribe
    public void onMusicPlaybackResumed(MusicPlaybackResumedEvent event){
        isPlaying = event.getStatus();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isplaying", isPlaying);
        editor.apply();
        updatePlayPauseButton(isPlaying);
    }

    @Subscribe
    public void onMusicPlaybackStarted(MusicPlaybackStartedEvent event) {
        // Handle the event here
        if (isPlaying){
            Intent serviceIntent = new Intent(getContext(), MusicService.class);
            serviceIntent.setAction("STOP");
            EventBus.getDefault().post(new MusicPlaybackStoppedEvent());
        }
        Song song = event.getSong();
        currentSong = song;
        fragmentlayout.setVisibility(View.VISIBLE);
        updateSongTitle(song.getTitle());
        updateArtist(song.getSinger());
        isPlaying = event.getStatus();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isplaying", isPlaying);
        editor.putString("fragmenttitle", song.getTitle());
        editor.putString("fragmentartist", song.getSinger());
        editor.putString("fragmentalbum", song.getAlbum());
        editor.apply();
        displayAlbum(song.getAlbum());
        updatePlayPauseButton(isPlaying);
        // Other logic
    }

    @Subscribe
    public void onMiniplayerFragmentRestarted(MiniplayerFragmentRestarted event) {
        // Handle the event here
        Song song = currentSong;
        updateSongTitle(song.getTitle());
        updateArtist(song.getSinger());
        displayAlbum(song.getAlbum());
        currentSong = song;
        // Other logic
    }

    @Subscribe
    public void onMusicPlaybackStopped(MusicPlaybackStoppedEvent event) {
        // Handle the event here
        // Update UI or perform other actions
        updatePlayPauseButton(isPlaying);
    }

    // Update the song title in the MiniPlayerFragment
    public void updateSongTitle(String songTitle) {
        if (songTitleTextView != null) {
            songTitleTextView.setText(songTitle);
        }
    }

    public void updateArtist(String artist) {
        if (artistTextView != null) {
            artistTextView.setText(artist);
        }
    }

    // Update the play/pause button state in the MiniPlayerFragment
    public void updatePlayPauseButton(boolean isPlaying) {
        if (playPauseButton != null) {
            if (isPlaying) {
                playPauseButton.setImageResource(R.drawable.ic_pause);
            } else {
                playPauseButton.setImageResource(R.drawable.ic_play);
            }
        }
        playPauseButton.invalidate();
    }

    private void displayAlbum(String albumUrl) {
        if (albumUrl == ""){
            return;
        }

        // Load the image with Picasso and use a Callback
        Picasso.get().load(albumUrl).into(artworkView, new Callback() {
            @Override
            public void onSuccess() {
                // The image has been successfully loaded into the artworkView.
                // Now, you can safely access the Drawable.
                Bitmap bitmap = ((BitmapDrawable) artworkView.getDrawable()).getBitmap();

                Palette.from(bitmap).generate(palette -> {
                    // Get the prominent color from the palette
                    int prominentColor = palette.getDominantColor(/* default color */ Color.BLACK);

                    // Calculate the color's lightness
                    float[] hsv = new float[3];
                    Color.colorToHSV(prominentColor, hsv);
                    float lightness = hsv[2]; // Extract lightness component

                    // Check if the color is too light (close to white)
                    if (lightness > 0.5f) {
                        // Darken the color (e.g., by reducing lightness)
                        lightness = 0.5f; // Adjust this value as needed
                        hsv[2] = lightness;
                        prominentColor = Color.HSVToColor(hsv);
                    }

                    // Get the current background drawable of the ConstraintLayout
                    Drawable currentBackground = layout.getBackground();

                    // If the current background is a ColorDrawable, extract the color
                    if (currentBackground instanceof ColorDrawable) {
                        int currentColor = ((ColorDrawable) currentBackground).getColor();
                        // Create a ValueAnimator for smooth color transition
                        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), currentColor, prominentColor);
                        colorAnimation.setDuration(500); // Set the duration in milliseconds
                        colorAnimation.addUpdateListener(animator -> {
                            int color = (int) animator.getAnimatedValue();
                            layout.setBackgroundColor(color);
                        });
                        colorAnimation.start();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "An Error has occurred", Toast.LENGTH_SHORT).show();
            }
        });
    }


}


