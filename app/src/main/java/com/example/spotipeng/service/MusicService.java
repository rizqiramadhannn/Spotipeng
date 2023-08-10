package com.example.spotipeng.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.spotipeng.R;
import com.example.spotipeng.activity.SongDetailActivity;
import com.example.spotipeng.api.JsonPlaceHolderApi;
import com.example.spotipeng.events.GetSongListEvent;
import com.example.spotipeng.events.MusicPlaybackLoopEvent;
import com.example.spotipeng.events.MusicPlaybackPausedEvent;
import com.example.spotipeng.events.MusicPlaybackResumedEvent;
import com.example.spotipeng.events.MusicPlaybackStartedEvent;
import com.example.spotipeng.events.MusicPlaybackStoppedEvent;
import com.example.spotipeng.events.UpdatePlaybackPositionEvent;
import com.example.spotipeng.events.UpdatePlaybackSeekbarPositionEvent;
import com.example.spotipeng.model.Song;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MusicService extends Service {
    private JsonPlaceHolderApi jsonPlaceHolderApi;
    private static final int NOTIFICATION_ID = 3;
    private static final String CHANNEL_ID = "MusicPlayerChannel";

    private MediaPlayer mediaPlayer;
    private Song currentSong;
    private boolean isPlaying = false;
    private int loopStatus = 0;
    List<Song> songs = new ArrayList<>();
    private int currentSongIndex = -1;
    private Handler handler = new Handler();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MusicBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "MusicPlayerChannel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setSound(null, null);
            channel.enableVibration(false);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        mediaPlayer = new MediaPlayer();
    }

    @Subscribe
    public void onGetSongList(GetSongListEvent event){
        songs = event.getSongs();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "PLAY":
                        playSong(currentSong);

                        break;
                    case "PAUSE":
                        pause();
                        break;
                    case "RESUME": // Handle resume action
                        resume();
                        break;
                    case "STOP":
                        stop();
                        break;
                    case "NEXT":
                        playNextSong();
                        break;
                    case "PREVIOUS":
                        playPreviousSong();
                        break;
                }
            }
        }
        return START_NOT_STICKY;
    }

    @Subscribe
    public void onMusicPlaybackStartedEvent(MusicPlaybackStartedEvent event) {
        int duration = mediaPlayer.getDuration();
        currentSong = event.getSong();
        currentSong.setDuration(duration);
    }

    @Subscribe
    public void onUpdateSeekbarPosition(UpdatePlaybackSeekbarPositionEvent event){
        mediaPlayer.seekTo(event.getCurrentPosition());
    }

    @Subscribe
    public void onMusicPlaybackLoop(MusicPlaybackLoopEvent event){
        loopStatus = event.getStatus();
    }

    @Subscribe
    public void onMusicPlaybackResumedEvent(MusicPlaybackResumedEvent event){
        showNotification(currentSong);
    }

    @Subscribe
    public void onMusicPlaybackPausedEvent(MusicPlaybackPausedEvent event){
        showNotification(currentSong);
    }

    private void playNextSong() {
        if (songs.isEmpty()) {
            return; // No songs in the playlist
        }
        if (currentSongIndex == songs.size()-1 && loopStatus == 0){
            Toast.makeText(this, "This is the end of Playlist", Toast.LENGTH_SHORT).show();
            return;
        }
        currentSongIndex = (currentSongIndex + 1) % songs.size(); // Get the index of the next song
        EventBus.getDefault().post(new MusicPlaybackStartedEvent(songs.get(currentSongIndex)));
        playSong(songs.get(currentSongIndex)); // Play the next song
    }

    private void playPreviousSong() {
        if (songs.isEmpty()) {
            return; // No songs in the playlist
        }
        if (currentSongIndex == 0 && loopStatus == 0){
            Toast.makeText(this, "This is the beginning of Playlist", Toast.LENGTH_SHORT).show();
            return;
        }
        currentSongIndex = (currentSongIndex - 1 + songs.size()) % songs.size(); // Get the index of the previous song
        EventBus.getDefault().post(new MusicPlaybackStartedEvent(songs.get(currentSongIndex)));
        playSong(songs.get(currentSongIndex)); // Play the previous song
    }

    private void playSong(Song song) {
        isPlaying = true;

        if (mediaPlayer != null) {
            mediaPlayer.reset();
            try {
                mediaPlayer.setDataSource(this, Uri.parse(song.getUrl()));
                mediaPlayer.prepare();
                int duration = mediaPlayer.getDuration();
                song.setDuration(duration);
                showNotification(song);
                mediaPlayer.start();
                currentSongIndex = songs.indexOf(song);
                EventBus.getDefault().post(new MusicPlaybackStartedEvent(currentSong));
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        // Playback completed, play the next song

                        if (loopStatus == 0 && currentSongIndex == songs.size()-1){
                            EventBus.getDefault().post(new MusicPlaybackStoppedEvent());
                        } else {
                            playNextSong();
                        }
                    }
                });
                EventBus.getDefault().post(new UpdatePlaybackPositionEvent(isPlaying, mediaPlayer.getCurrentPosition()));
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            int currentPosition = mediaPlayer.getCurrentPosition();
                            EventBus.getDefault().post(new UpdatePlaybackPositionEvent(true, currentPosition));
                            handler.postDelayed(this, 1000); // Update every 1 second
                        }
                    }
                }, 1000);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void pause() {
        mediaPlayer.pause();
        isPlaying = false;
        handler.removeCallbacksAndMessages(null);
        EventBus.getDefault().post(new MusicPlaybackPausedEvent());
    }

    private void resume() {
        mediaPlayer.start();
        isPlaying = true;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    EventBus.getDefault().post(new UpdatePlaybackPositionEvent(true, currentPosition));
                    handler.postDelayed(this, 1000); // Update every 1 second
                }
            }
        }, 1000); // Start the time ticker again after resume
        EventBus.getDefault().post(new MusicPlaybackResumedEvent());
    }

    private void stop() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            isPlaying = false;
            stopForeground(true);
        }
    }

    private void showNotification(Song song) {
        String playResumeAction = "PAUSE";
        if (!isPlaying){
            playResumeAction = "RESUME";
        }
        PendingIntent pendingPlayIntent = PendingIntent.getService(this, 0, new Intent(this, MusicService.class).setAction(playResumeAction), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent pendingNextIntent = PendingIntent.getService(this, 0, new Intent(this, MusicService.class).setAction("NEXT"), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent pendingPrevIntent = PendingIntent.getService(this, 0, new Intent(this, MusicService.class).setAction("PREVIOUS"), PendingIntent.FLAG_IMMUTABLE);
        Intent detailIntent = new Intent(this, SongDetailActivity.class);
        detailIntent.putExtra("song", song);
        PendingIntent pendingDetailIntent = PendingIntent.getActivity(this, 0, detailIntent, PendingIntent.FLAG_IMMUTABLE);

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout_expanded);
        if (!isPlaying) {
            remoteViews.setImageViewResource(R.id.notification_play_pause, R.drawable.ic_play_black);
        }
        remoteViews.setOnClickPendingIntent(R.id.notification_layout, pendingDetailIntent);
        remoteViews.setTextViewText(R.id.notification_title, song.getTitle() + " - " + song.getSinger());
        remoteViews.setOnClickPendingIntent(R.id.notification_play_pause, pendingPlayIntent);
        remoteViews.setOnClickPendingIntent(R.id.notification_previous, pendingPrevIntent);
        remoteViews.setOnClickPendingIntent(R.id.notification_next, pendingNextIntent);

// Calculate and set up the progress bar
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        int playbackProgress = calculatePlaybackProgress();
        progressBar.setMax(100); // Set the maximum value for progress
        progressBar.setProgress(playbackProgress); // Set the current progress
        remoteViews.setProgressBar(R.id.notification_progress_bar, 100, playbackProgress, false);

        RemoteViews collapsedView = new RemoteViews(getPackageName(), R.layout.notification_layout);
        collapsedView.setTextViewText(R.id.notification_title, song.getTitle() + " - " + song.getSinger());

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(collapsedView)
                .setCustomBigContentView(remoteViews)
                .setSmallIcon(R.drawable.spotipeng_logo_only)
                .build();

        startForeground(NOTIFICATION_ID, notification);

    }

    private int calculatePlaybackProgress() {
        if (mediaPlayer != null) {
            int currentPosition = mediaPlayer.getCurrentPosition();
            int totalDuration = mediaPlayer.getDuration();

            // Calculate the playback progress in percentage
            double progressPercentage = (currentPosition * 100.0) / totalDuration;

            // Ensure the progress is within the valid range (0 to 100)
            return (int) Math.min(100, Math.max(0, progressPercentage));
        }

        return 0; // Return 0 if mediaPlayer is not available
    }


    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }


}
