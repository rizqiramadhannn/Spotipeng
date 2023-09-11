package com.example.spotipeng.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.spotipeng.R;
import com.example.spotipeng.events.CloseLyricsEvent;
import com.example.spotipeng.events.MusicPlaybackPausedEvent;
import com.example.spotipeng.events.MusicPlaybackResumedEvent;
import com.example.spotipeng.events.MusicPlaybackStartedEvent;
import com.example.spotipeng.events.UpdatePlaybackPositionEvent;
import com.example.spotipeng.events.UpdatePlaybackSeekbarPositionEvent;
import com.example.spotipeng.model.Song;
import com.example.spotipeng.service.MusicService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AllLyricsActivity extends AppCompatActivity {
    private int color;
    private boolean isPlaying = true;
    private TextView titleTV;
    private TextView artistTV;
    private TextView lyricsTextView;
    private ImageButton back;
    private FrameLayout lyricsContainer;
    private TextView leftDurationTextView;
    private TextView rightDurationTextView;
    private SeekBar seekBar;
    private ImageButton playPauseButton;
    private Handler handler = new Handler();
    private Song song;
    private int rightduration;
    private boolean isOpened = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_lyrics);

        // Retrieve the lyrics from the intent
        song = getIntent().getParcelableExtra("song");
        color = getIntent().getIntExtra("color", Color.BLACK);

        titleTV = findViewById(R.id.songTitle);
        artistTV = findViewById(R.id.songArtist);
        lyricsTextView = findViewById(R.id.allLyricsTextView);
        back = findViewById(R.id.backArrowButton);
        lyricsContainer = findViewById(R.id.lyricsContainer);
        leftDurationTextView = findViewById(R.id.leftDurationTextView);
        rightDurationTextView = findViewById(R.id.rightDurationTextView);
        seekBar = findViewById(R.id.seekBar);
        playPauseButton = findViewById(R.id.playPauseButton);
        rightduration = song.getDuration();
        titleTV.setText(song.getTitle());
        artistTV.setText(song.getSinger());
        lyricsTextView.setText(song.getLyrics());
//        downloadLyrics(song.getLyrics());
        leftDurationTextView.setText(formatDuration(0));
        rightDurationTextView.setText(formatDuration(song.getDuration()));
        lyricsContainer.setBackgroundColor(color);
        back.setOnClickListener(view -> {
            isOpened = !isOpened;
            onBackPressed();
        });
        changeStatusBarColor(color);
        playPauseButton.setOnClickListener(v -> togglePlayback());

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
    }

    private void downloadLyrics(String lyricsUrl) {
        // You can use an AsyncTask or another background thread to download the lyrics
        // Here, I'll provide an example using AsyncTask
        new DownloadLyricsTask().execute(lyricsUrl);
    }

    private class DownloadLyricsTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String url = params[0];
            try {
                return downloadLyricsFromUrl(url);
            } catch (IOException e) {
                e.printStackTrace();
                return "Failed to download lyrics.";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            lyricsTextView.setText(result); // Set the lyrics in the TextView
        }
    }

    private String downloadLyricsFromUrl(String url) throws IOException {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        StringBuilder lyrics = new StringBuilder();

        try {
            // Create a URL object from the given URL string
            URL lyricsUrl = new URL(url);

            // Open a connection to the URL
            connection = (HttpURLConnection) lyricsUrl.openConnection();

            // Set the request method to GET
            connection.setRequestMethod("GET");

            // Connect to the URL
            connection.connect();

            // Check if the response code indicates success (HTTP 200 OK)
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                // Open an input stream to read the content
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;

                // Read the content line by line and append it to the lyrics StringBuilder
                while ((line = reader.readLine()) != null) {
                    lyrics.append(line).append("\n");
                }
            } else {
                // Handle the case where the request did not succeed (e.g., handle errors)
                return "Failed to retrieve lyrics. HTTP response code: " + connection.getResponseCode();
            }
        } finally {
            // Close the reader and the connection
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

        // Return the lyrics as a string
        return lyrics.toString();
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
    public void onMusicPlaybackPaused(MusicPlaybackPausedEvent event){
        isPlaying = event.getStatus();
        updatePlayPauseButton(isPlaying);
    }

    @Subscribe
    public void onMusicPlaybackResumed(MusicPlaybackResumedEvent event){
        isPlaying = event.getStatus();
        updatePlayPauseButton(isPlaying);
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

    private String formatDuration(int duration) {
        int seconds = duration / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

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

    @Override
    public void onBackPressed() {
        Intent loginIntent = new Intent(this, MainActivity.class);
        startActivity(loginIntent);
        overridePendingTransition(0, R.anim.slide_out);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);

        if (isOpened) {
            EventBus.getDefault().post(new CloseLyricsEvent());
        }

        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Subscribe
    public void onCloseLyricsEvent(CloseLyricsEvent event) {
        super.onBackPressed();
    }

    private void changeStatusBarColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(color);
        }
    }
}
