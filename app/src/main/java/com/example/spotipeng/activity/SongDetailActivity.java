package com.example.spotipeng.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.spotipeng.R;
import com.example.spotipeng.events.MusicPlaybackLoopEvent;
import com.example.spotipeng.events.MusicPlaybackPausedEvent;
import com.example.spotipeng.events.MusicPlaybackResumedEvent;
import com.example.spotipeng.events.MusicPlaybackStartedEvent;
import com.example.spotipeng.events.UpdatePlaybackPositionEvent;
import com.example.spotipeng.events.UpdatePlaybackSeekbarPositionEvent;
import com.example.spotipeng.model.Song;
import com.example.spotipeng.service.MusicService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

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
    private boolean isPlaying = true;
    private int loopStatus = 0;
    private int rightduration;
    private Handler handler = new Handler();
    private Runnable updateProgressRunnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
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
        loopButton.setOnClickListener(v -> {
            if (loopStatus == 1){
                loopStatus = 0;
            } else {
                loopStatus = 1;
            }
            EventBus.getDefault().post(new MusicPlaybackLoopEvent(loopStatus));
        });

        shuffleButton.setOnClickListener(v -> toggleShuffling());
        nextButton.setOnClickListener(v -> playNextSong());
        backButton.setOnClickListener(v -> playPreviousSong());
        playPauseButton.setOnClickListener(v -> togglePlayback());

        // Set seek bar change listener to update media player progress
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    EventBus.getDefault().post(new UpdatePlaybackSeekbarPositionEvent(true, progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Remove callbacks when user starts dragging the seek bar
                // to prevent updating the seek bar progress while dragging
                handler.removeCallbacksAndMessages(null);
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
                rightduration = song.getDuration();
                // Update views with song data
                titleTextView.setText(song.getTitle());
                artistTextView.setText(song.getSinger());
                leftDurationTextView.setText(formatDuration(0));
                rightDurationTextView.setText(formatDuration(song.getDuration()));
            }
        }
    }

    @Subscribe
    public void onUpdatePlaybackPosition(UpdatePlaybackPositionEvent event) {
        boolean isPlaying = event.isPlaying();
        int currentPosition = event.getCurrentPosition();
        updatePlayPauseButton(isPlaying);
        seekBar.setMax(rightduration);
        seekBar.setProgress(currentPosition);
        leftDurationTextView.setText(formatDuration(currentPosition));
        rightDurationTextView.setText("-"+formatDuration(rightduration - currentPosition));
    }

    @Subscribe
    public void onMusicPlaybackStartedEvent(MusicPlaybackStartedEvent event) {
        Song song = event.getSong();
        rightduration = song.getDuration();
        // Update views with song data
        titleTextView.setText(song.getTitle());
        artistTextView.setText(song.getSinger());
        leftDurationTextView.setText(formatDuration(0));
        rightDurationTextView.setText(formatDuration(song.getDuration()));
        seekBar.setMax(song.getDuration());
        seekBar.setProgress(0);
    }
    // Implement helper methods to control music playback and update UI

    @Subscribe
    public void onMusicPlaybackLoopEvent(MusicPlaybackLoopEvent event) {
        int status = event.getStatus();
        if (status == 1){
            loopButton.setImageResource(R.drawable.ic_loop_on);
        } else {
            loopButton.setImageResource(R.drawable.ic_loop_off);
        }
    }

    private void toggleShuffling() {
        // Implement logic to toggle shuffling
    }

    private void playNextSong() {
        Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction("NEXT");
        this.startService(serviceIntent);
    }

    private void playPreviousSong() {
        Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction("PREVIOUS");
        this.startService(serviceIntent);
    }

    private void togglePlayback() {
        Intent serviceIntent = new Intent(this, MusicService.class);
        if (isPlaying) {
            serviceIntent.setAction("PAUSE");
        } else {
            // Start playing the music
            serviceIntent.setAction("RESUME");
        }
        this.startService(serviceIntent);
    }

    public void updatePlayPauseButton(boolean isPlaying) {
        if (playPauseButton != null) {
            if (isPlaying) {
                playPauseButton.setImageResource(R.drawable.ic_pause);
            } else {
                playPauseButton.setImageResource(R.drawable.ic_play);
            }
        }
    }

    private String formatDuration(int duration) {
        int seconds = duration / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
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

}
