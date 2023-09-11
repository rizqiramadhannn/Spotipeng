package com.example.spotipeng.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.utils.StopLogic;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.spotipeng.R;
import com.example.spotipeng.activity.SongDetailActivity;
import com.example.spotipeng.api.JsonPlaceHolderApi;
import com.example.spotipeng.events.GetSongListEvent;
import com.example.spotipeng.events.CloseLyricsEvent;
import com.example.spotipeng.events.MiniplayerFragmentRestarted;
import com.example.spotipeng.events.MusicPlaybackLoopEvent;
import com.example.spotipeng.events.MusicPlaybackPausedEvent;
import com.example.spotipeng.events.MusicPlaybackResumedEvent;
import com.example.spotipeng.events.MusicPlaybackShuffleEvent;
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
import java.util.Random;

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
    private int shuffleStatus = 0;
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
    public void onGetSongList(GetSongListEvent event) {
        songs.clear(); // Clear the existing songs
        songs.addAll(event.getSongs()); // Update with new songs
        currentSongIndex = songs.indexOf(currentSong);

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
    public void onUpdateSeekbarPosition(UpdatePlaybackSeekbarPositionEvent event) {
        mediaPlayer.seekTo(event.getCurrentPosition());
    }

    @Subscribe
    public void onMusicPlaybackLoop(MusicPlaybackLoopEvent event) {
        loopStatus = event.getStatus();
    }

    @Subscribe
    public void onMusicPlaybackShuffle(MusicPlaybackShuffleEvent event) {
        shuffleStatus = event.getStatus();
    }

    @Subscribe
    public void onMusicPlaybackResumedEvent(MusicPlaybackResumedEvent event) {
        showNotification(currentSong);
    }

    @Subscribe
    public void onMusicPlaybackPausedEvent(MusicPlaybackPausedEvent event) {
        showNotification(currentSong);
    }

    private void playNextSong() {
        if (songs.isEmpty()) {
            Log.i("TAG", "playNextSong: " + "empty");
            return; // No songs in the playlist
        }
        if (currentSongIndex == songs.size() - 1 && loopStatus == 0 && shuffleStatus != 1) {
            Toast.makeText(this, "This is the end of Playlist", Toast.LENGTH_SHORT).show();
            return;
        }
        if (shuffleStatus == 1){
            int randomIndex = new Random().nextInt(songs.size());
            currentSongIndex = randomIndex;
        } else {
            currentSongIndex = (currentSongIndex + 1) % songs.size();
        }
         // Get the index of the next song
        EventBus.getDefault().post(new MusicPlaybackStartedEvent(songs.get(currentSongIndex)));
        playSong(songs.get(currentSongIndex)); // Play the next song
    }

    private void playPreviousSong() {
        if (songs.isEmpty()) {
            return; // No songs in the playlist
        }
        if (currentSongIndex == 0 && loopStatus == 0) {
            Toast.makeText(this, "This is the beginning of Playlist", Toast.LENGTH_SHORT).show();
            return;
        } else if (loopStatus == 1) {
            currentSongIndex = songs.size();
        }
        if (shuffleStatus == 1){
            int randomIndex = new Random().nextInt(songs.size());
            currentSongIndex = randomIndex;
        } else {
            currentSongIndex = (currentSongIndex - 1) % songs.size();
        } // Get the index of the previous song
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
                currentSong = song;
                stopForeground(true);
                showNotification(currentSong);
                mediaPlayer.start();
                currentSongIndex = songs.indexOf(song);
                EventBus.getDefault().post(new MusicPlaybackStartedEvent(currentSong));
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        EventBus.getDefault().post(new CloseLyricsEvent());
                        if (loopStatus == 0 && currentSongIndex == songs.size() - 1) {
                            EventBus.getDefault().post(new MusicPlaybackStoppedEvent());
                        } else {
                            playNextSong();
                        }
                    }
                });
                EventBus.getDefault().post(new UpdatePlaybackPositionEvent(song, isPlaying, mediaPlayer.getCurrentPosition()));
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            int currentPosition = mediaPlayer.getCurrentPosition();
//                            int remainingTime = song.getDuration() - currentPosition;
//
//                            if (remainingTime <= 1000 && remainingTime >= 0) {
//                                EventBus.getDefault().post(new CloseLyricsEvent());
//                                Log.i("TAG", "runaa: " + remainingTime + " " + currentPosition + " " + song.getDuration());
//                            }

                            EventBus.getDefault().post(new UpdatePlaybackPositionEvent(song,true, currentPosition));
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
                    EventBus.getDefault().post(new UpdatePlaybackPositionEvent(currentSong,true, currentPosition));
                    handler.postDelayed(this, 1000); // Update every 1 second
                }
            }
        }, 1000); // Start the time ticker again after resume
        EventBus.getDefault().post(new MusicPlaybackResumedEvent());
    }

    private void stop() {
        mediaPlayer.stop();
        isPlaying = false;
        stopForeground(true);
    }

    private void showNotification(Song song) {
        String playResumeAction = "PAUSE";
        // Create PendingIntent for notification buttons
        if (!isPlaying) {
            playResumeAction = "RESUME";
        }
        PendingIntent pendingPlayIntent = PendingIntent.getService(this, 0, new Intent(this, MusicService.class).setAction(playResumeAction), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent pendingNextIntent = PendingIntent.getService(this, 0, new Intent(this, MusicService.class).setAction("NEXT"), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent pendingPrevIntent = PendingIntent.getService(this, 0, new Intent(this, MusicService.class).setAction("PREVIOUS"), PendingIntent.FLAG_IMMUTABLE);
        Intent detailIntent = new Intent(this, SongDetailActivity.class);
        detailIntent.putExtra("song", song);
        PendingIntent pendingDetailIntent = PendingIntent.getActivity(this, 0, detailIntent, PendingIntent.FLAG_MUTABLE);

        // Set up custom notification views
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout_expanded);
        RemoteViews collapsedView = new RemoteViews(getPackageName(), R.layout.notification_layout);

        // Set the song title and artist in both collapsed and expanded views
        collapsedView.setTextViewText(R.id.notification_title, song.getTitle() + " - " + song.getSinger());
        remoteViews.setTextViewText(R.id.notification_title, song.getTitle() + " - " + song.getSinger());

        // Set the play/pause action based on the playback state
        remoteViews.setImageViewResource(R.id.notification_play_pause, isPlaying ? R.drawable.ic_pause_black : R.drawable.ic_play_black);
        remoteViews.setOnClickPendingIntent(R.id.notification_play_pause, pendingPlayIntent);

        // Set the next and previous actions
        remoteViews.setOnClickPendingIntent(R.id.notification_previous, pendingPrevIntent);
        remoteViews.setOnClickPendingIntent(R.id.notification_next, pendingNextIntent);

        // Set up the progress bar
        remoteViews.setProgressBar(R.id.notification_progress_bar, song.getDuration(), 0, false);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(collapsedView)
                .setCustomBigContentView(remoteViews)
                .setSmallIcon(R.drawable.spotipeng_logo_only)
                .setContentIntent(pendingDetailIntent) // Set the detail intent when user taps the notification
                .build();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();

                    remoteViews.setProgressBar(R.id.notification_progress_bar, song.getDuration(), currentPosition, false);

                    // Update elapsed time and remaining time TextViews
                    remoteViews.setTextViewText(R.id.notification_elapsed_time, formatDuration(currentPosition));
                    remoteViews.setTextViewText(R.id.notification_remaining_time, formatDuration(song.getDuration() - currentPosition));

                    // Update the notification
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MusicService.this);
                    if (ActivityCompat.checkSelfPermission(MusicService.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        // Handle permission checks
                        return;
                    }
                    notificationManager.notify(NOTIFICATION_ID, notification);

                    // Schedule the next update after a short delay (e.g., 1 second)
                    handler.postDelayed(this, 1000); // Update every 1 second
                }
            }

        });
        startForeground(NOTIFICATION_ID, notification);
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

    private String formatDuration(int duration) {
        int seconds = duration / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }


}
