package com.example.spotipeng;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class SongDetailActivity extends AppCompatActivity {
    private ImageView artworkView;
    private TextView titleTextView;
    private TextView artistTextView;
    private TextView leftDurationTextView;
    private TextView rightDurationTextView;
    private SeekBar seekBar;
    private ImageButton loopButton;
    private ImageButton shuffleButton;
    private ImageButton nextButton;
    private ImageButton backButton;
    private ImageButton playPauseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.song_detail);

        // Initialize views
        artworkView = findViewById(R.id.artworkView);
        titleTextView = findViewById(R.id.titleTextView);
        artistTextView = findViewById(R.id.artistTextView);
        leftDurationTextView = findViewById(R.id.leftDurationTextView);
        rightDurationTextView = findViewById(R.id.rightDurationTextView);
        seekBar = findViewById(R.id.seekBar);
        loopButton = findViewById(R.id.loopButton);
        shuffleButton = findViewById(R.id.shuffleButton);
        nextButton = findViewById(R.id.nextButton);
        backButton = findViewById(R.id.backButton);
        playPauseButton = findViewById(R.id.playPauseButton);

        // Set click listeners for control buttons
        loopButton.setOnClickListener(v -> toggleLooping());
        shuffleButton.setOnClickListener(v -> toggleShuffling());
        nextButton.setOnClickListener(v -> playNextSong());
        backButton.setOnClickListener(v -> playPreviousSong());
        playPauseButton.setOnClickListener(v -> togglePlayback());

        // Set seek bar change listener to update media player progress
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // Implement seek bar progress update logic
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Remove callbacks when user starts dragging the seek bar
                // to prevent updating the seek bar progress while dragging
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Implement logic to update media player position when user stops dragging seek bar
            }
        });

        // Retrieve the song data from the intent
        Intent intent = getIntent();
        if (intent != null) {
            Song song = intent.getParcelableExtra("song");
            if (song != null) {
                // Update views with song data
                titleTextView.setText(song.getTitle());
                artistTextView.setText(song.getSinger());
                leftDurationTextView.setText("00:00");
                rightDurationTextView.setText("00:00");
                Glide.with(this).load(song.getAlbum()).into(artworkView);
            }
        }
    }

    // Implement helper methods to control music playback and update UI

    private void toggleLooping() {
        // Implement logic to toggle looping
    }

    private void toggleShuffling() {
        // Implement logic to toggle shuffling
    }

    private void playNextSong() {
        // Implement logic to play the next song
    }

    private void playPreviousSong() {
        // Implement logic to play the previous song
    }

    private void togglePlayback() {
        // Implement logic to toggle music playback
    }
}
