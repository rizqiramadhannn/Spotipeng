package com.example.spotipeng.events;

import com.example.spotipeng.model.Song;

public class UpdatePlaybackPositionEvent {
    private int currentPosition;
    private boolean isPlaying;
    private Song song;
    public UpdatePlaybackPositionEvent(Song song, boolean isPlaying, int currentPosition) {
        this.song = song;
        this.isPlaying = isPlaying;
        this.currentPosition = currentPosition;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public Song getSong() {
        return song;
    }
}
