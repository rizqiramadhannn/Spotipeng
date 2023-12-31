package com.example.spotipeng.activity;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.example.spotipeng.R;
import com.example.spotipeng.events.CloseLyricsEvent;
import com.example.spotipeng.events.MusicPlaybackLoopEvent;
import com.example.spotipeng.events.MusicPlaybackPausedEvent;
import com.example.spotipeng.events.MusicPlaybackResumedEvent;
import com.example.spotipeng.events.MusicPlaybackShuffleEvent;
import com.example.spotipeng.events.MusicPlaybackStartedEvent;
import com.example.spotipeng.events.MusicPlaybackStoppedEvent;
import com.example.spotipeng.events.UpdatePlaybackPositionEvent;
import com.example.spotipeng.events.UpdatePlaybackSeekbarPositionEvent;
import com.example.spotipeng.fragment.LyricsFragment;
import com.example.spotipeng.model.Song;
import com.example.spotipeng.service.MusicService;
import com.squareup.picasso.Picasso;

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
    private int shuffleStatus = 0;
    private int rightduration;
    private Handler handler = new Handler();
    private SharedPreferences preferences;
    private Runnable updateProgressRunnable;

    private int currentBackgroundColor = Color.BLACK;

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

        preferences = getSharedPreferences("spotipeng", MODE_PRIVATE);
        shuffleStatus = preferences.getInt("shuffleStatus", 0); // Default value 0 (or any default you prefer)
        loopStatus = preferences.getInt("loopStatus", 0); // Default value 0 (or any default you prefer)
        if (shuffleStatus == 1){
            shuffleButton.setImageResource(R.drawable.ic_shuffle_on);
            EventBus.getDefault().post(new MusicPlaybackShuffleEvent(shuffleStatus));
        }
        if (loopStatus == 1){
            loopButton.setImageResource(R.drawable.ic_loop_on);
            EventBus.getDefault().post(new MusicPlaybackLoopEvent(loopStatus));
        }
        // Set click listeners for control buttons
        loopButton.setOnClickListener(v -> {
            if (loopStatus == 1){
                loopStatus = 0;
            } else {
                loopStatus = 1;
            }
            preferences = getSharedPreferences("spotipeng", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("loopStatus", loopStatus);
            editor.apply();
            EventBus.getDefault().post(new MusicPlaybackLoopEvent(loopStatus));
        });

        shuffleButton.setOnClickListener(v -> {
            if (shuffleStatus == 1){
                shuffleStatus = 0;
            } else {
                shuffleStatus = 1;
            }
            SharedPreferences preferences = getSharedPreferences("spotipeng", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("shuffleStatus", shuffleStatus);
            editor.apply();
            EventBus.getDefault().post(new MusicPlaybackShuffleEvent(shuffleStatus));
        });
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
                displayAlbum(song);
                titleTextView.setText(song.getTitle());
                artistTextView.setText(song.getSinger());
                leftDurationTextView.setText(formatDuration(0));
                rightDurationTextView.setText(formatDuration(song.getDuration()));
            }
        }

        ImageButton backArrowButton = findViewById(R.id.backArrowButton);

        backArrowButton.setOnClickListener(v -> {
            // Apply the slide-out animation when the back arrow button is clicked
            onBackPressed();
        });

        backArrowButton.setVisibility(View.VISIBLE);
    }

    private void displayAlbum(Song song) {
        ImageView artworkView = findViewById(R.id.artworkView);
        String albumUrl = song.getAlbum();
        Picasso.get().load(albumUrl).into(artworkView);
        Bitmap bitmap = ((BitmapDrawable) artworkView.getDrawable()).getBitmap();
        Palette.from(bitmap).generate(palette -> {
            // Get the prominent color from the palette
            int prominentColor = palette.getDominantColor(currentBackgroundColor);

            // Calculate the color's lightness
            float[] hsv = new float[3];
            Color.colorToHSV(prominentColor, hsv);
            float lightness = hsv[2]; // Extract lightness component

            // Check if the color is too light (close to white)
            if (lightness > 0.8f) {
                // Darken the color (e.g., by reducing lightness)
                lightness = 0.5f; // Adjust this value as needed
                hsv[2] = lightness;
                prominentColor = Color.HSVToColor(hsv);
            }

            // Create a new GradientDrawable for the target background
            GradientDrawable targetDrawable = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{prominentColor, Color.BLACK}
            );
            RelativeLayout layout = findViewById(R.id.Layout);
            targetDrawable.setCornerRadius(0f); // Set corner radius if needed

            // Create a ValueAnimator to transition between the current and target GradientDrawables
            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(10000); // Set the duration to 250 milliseconds

            // Add an update listener to smoothly transition the background
            int finalProminentColor = prominentColor;
            animator.addUpdateListener(valueAnimator -> {
                float fraction = valueAnimator.getAnimatedFraction();
                GradientDrawable transitionDrawable = new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{blendColors(currentBackgroundColor, finalProminentColor, fraction), Color.BLACK}
                );
                transitionDrawable.setCornerRadius(0f); // Set corner radius if needed
                layout.setBackground(transitionDrawable);
            });
            changeStatusBarColor(prominentColor);
            showFragment(prominentColor, song);
            // Start the animation
            animator.start();

            // Update the current background color
            currentBackgroundColor = prominentColor;
        });
    }

    private void showFragment(int color, Song song) {
        LyricsFragment lyricsFragment = LyricsFragment.newInstance(song, color);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.lyricsContainer, lyricsFragment) // Replace R.id.lyricsContainer with the actual ID
                .commit();

    }

    // Helper method to blend two colors based on a fraction
    private int blendColors(int color1, int color2, float fraction) {
        float inverseFraction = 1f - fraction;

        int red = (int) (Color.red(color1) * fraction + Color.red(color2) * inverseFraction);
        int green = (int) (Color.green(color1) * fraction + Color.green(color2) * inverseFraction);
        int blue = (int) (Color.blue(color1) * fraction + Color.blue(color2) * inverseFraction);

        return Color.rgb(red, green, blue);
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
        displayAlbum(song);
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

    @Subscribe
    public void onMusicPlaybackShuffleEvent(MusicPlaybackShuffleEvent event) {
        int status = event.getStatus();
        if (status == 1){
            shuffleButton.setImageResource(R.drawable.ic_shuffle_on);
        } else {
            shuffleButton.setImageResource(R.drawable.ic_shuffle_off);
        }
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
        playPauseButton.invalidate();
    }

    private void changeStatusBarColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(color);
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

    @Subscribe
    public void onMusicPlaybackStopped(MusicPlaybackStoppedEvent event) {
        updatePlayPauseButton(false);
        // Update UI or perform other actions
        seekBar.setProgress(0);
        leftDurationTextView.setText(formatDuration(0));
        rightDurationTextView.setText(formatDuration(rightduration));
    }

    @Subscribe
    public void onClosedLyricsEvent(CloseLyricsEvent event){
        Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction("STOP");
        this.startService(serviceIntent);
        serviceIntent.setAction("PLAY");
        this.startService(serviceIntent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Set custom animations for the transition
        overridePendingTransition(0, R.anim.slide_out);
    }

    @Override
    protected void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }
}
