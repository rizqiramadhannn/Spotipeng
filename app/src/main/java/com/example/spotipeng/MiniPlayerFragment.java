package com.example.spotipeng;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import jp.wasabeef.recyclerview.adapters.ScaleInAnimationAdapter;

public class MiniPlayerFragment extends Fragment {

    private TextView songTitleTextView;
    private TextView artistTextView;
    private ImageView artworkView;
    private ImageButton playPauseButton;
    private boolean isPlaying = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_mini_player, container, false);
        songTitleTextView = rootView.findViewById(R.id.titleView);
        artistTextView = rootView.findViewById(R.id.artistView);
        artworkView = rootView.findViewById(R.id.artworkView);
        playPauseButton = rootView.findViewById(R.id.playPauseButton);

        // Set click listener for the play/pause button
        playPauseButton.setOnClickListener(v -> {
            if (isPlaying) {
                // Pause the music
                pauseMusic();
            } else {
                // Start playing the music
                startMusic();
            }
        });

        // Check if the song title is already set in the MainActivity and update it in the MiniPlayerFragment
        String songTitle = ((MainActivity) requireActivity()).getCurrentSongTitle();
        String artist = ((MainActivity) requireActivity()).getCurrentArtist();

        if (songTitle != null) {
            updateSongTitle(songTitle);
        }
        if (artist != null) {
            updateArtist(artist);
        }
        // Apply fade-in animation to the root view of the fragment
        Animation fadeInAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in);
        rootView.startAnimation(fadeInAnimation);

        return rootView;
    }

    private void startMusic() {
        // Implement the code to start playing the music
        isPlaying = true;
        // Update the play/pause button to show pause icon
        updatePlayPauseButton(true);

        // Start the music in the SongAdapter if available
        if (((MainActivity) requireActivity()).getSongAdapter() != null) {
            ((MainActivity) requireActivity()).getSongAdapter().startMusic();
        }
    }

    private void pauseMusic() {
        // Implement the code to pause the music
        isPlaying = false;
        // Update the play/pause button to show play icon
        updatePlayPauseButton(false);

        // Pause the music in the SongAdapter
        if (((MainActivity) requireActivity()).getSongAdapter() != null) {
            ((MainActivity) requireActivity()).getSongAdapter().pauseMusic();
        }
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

