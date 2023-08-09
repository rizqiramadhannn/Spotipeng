package com.example.spotipeng.events;

public class UpdatePlaybackSeekbarPositionEvent {
    private int currentPosition;
    private boolean isPlaying;
    public UpdatePlaybackSeekbarPositionEvent(boolean isPlaying, int currentPosition) {
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
