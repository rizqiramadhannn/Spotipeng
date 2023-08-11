package com.example.spotipeng.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.spotipeng.events.GetCurrentSongEvent;
import com.example.spotipeng.events.UpdatePlaybackPositionEvent;
import com.example.spotipeng.service.MusicService;
import com.example.spotipeng.R;
import com.example.spotipeng.activity.SongDetailActivity;
import com.example.spotipeng.events.MusicPlaybackPausedEvent;
import com.example.spotipeng.events.MusicPlaybackResumedEvent;
import com.example.spotipeng.events.MusicPlaybackStartedEvent;
import com.example.spotipeng.events.MusicPlaybackStoppedEvent;
import com.example.spotipeng.model.Song;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class MiniPlayerFragment extends Fragment {

    private TextView songTitleTextView;
    private TextView artistTextView;
    private ImageView artworkView;
    private ImageButton playPauseButton;
    private boolean isPlaying = false;
    private Song currentSong;

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
        updatePlayPauseButton(isPlaying);
    }

    @Subscribe
    public void onMusicPlaybackResumed(MusicPlaybackResumedEvent event){
        isPlaying = event.getStatus();
        updatePlayPauseButton(isPlaying);
    }

    @Subscribe
    public void onMusicPlaybackStarted(MusicPlaybackStartedEvent event) {
        // Handle the event here
        Song song = event.getSong();
        updateSongTitle(song.getTitle());
        updateArtist(song.getSinger());
        isPlaying = event.getStatus();
        updatePlayPauseButton(isPlaying);
        currentSong = song;
        // Other logic
    }

    @Subscribe
    public void onUpdatePlaybackPosition(UpdatePlaybackPositionEvent event) {
        // Handle the event here
        Song song = event.getSong();
        updateSongTitle(song.getTitle());
        updateArtist(song.getSinger());
        currentSong = song;
        // Other logic
    }

    @Subscribe
    public void onMusicPlaybackStopped(MusicPlaybackStoppedEvent event) {
        // Handle the event here
        // Update UI or perform other actions
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
    }
}
