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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.spotipeng.R;
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMusicPlaybackStartedEvent(MusicPlaybackStartedEvent event) {

        currentSong = event.getSong();
    }

    @Subscribe
    public void onUpdateSeekbarPosition(UpdatePlaybackSeekbarPositionEvent event){
        mediaPlayer.seekTo(event.getCurrentPosition());
    }

    @Subscribe
    public void onMusicPlaybackLoop(MusicPlaybackLoopEvent event){
        loopStatus = event.getStatus();
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
        showNotification(currentSong.getTitle(), currentSong.getSinger());
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            try {
                mediaPlayer.setDataSource(this, Uri.parse(song.getUrl()));
                mediaPlayer.prepare();
                int duration = mediaPlayer.getDuration();
                song.setDuration(duration);
                mediaPlayer.start();
                currentSongIndex = songs.indexOf(song);
                isPlaying = true;
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
        handler.removeCallbacksAndMessages(null);
        EventBus.getDefault().post(new MusicPlaybackPausedEvent());
    }

    private void resume() {
        mediaPlayer.start();
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

    private void showNotification(String title, String artist) {
        String playResumeTV = "Play";
        String playResumeAction = "PLAY";
        // Create PendingIntent for notification buttons
        if (isPlaying){
            playResumeTV = "Resume";
            playResumeAction = "RESUME";
        }
        PendingIntent pendingPlayIntent = PendingIntent.getService(this, 0, new Intent(this, MusicService.class).setAction(playResumeAction), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent pendingPauseIntent = PendingIntent.getService(this, 0, new Intent(this, MusicService.class).setAction("PAUSE"), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent pendingStopIntent = PendingIntent.getService(this, 0, new Intent(this, MusicService.class).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(R.drawable.ic_default_artwork)
                .addAction(R.drawable.ic_pause, "Pause", pendingPauseIntent)
                .addAction(R.drawable.ic_play, playResumeTV, pendingPlayIntent)
                .addAction(R.drawable.ic_pause, "Stop", pendingStopIntent)
                .build();

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


}
