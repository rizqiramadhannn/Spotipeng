package com.example.spotipeng.events;

public class UpdatePlaybackPositionEvent {
    private int currentPosition;
    private boolean isPlaying;
    public UpdatePlaybackPositionEvent(boolean isPlaying, int currentPosition) {
        this.isPlaying = isPlaying;
        this.currentPosition = currentPosition;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }
}
