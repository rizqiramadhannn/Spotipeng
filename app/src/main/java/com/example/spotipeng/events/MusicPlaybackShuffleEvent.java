package com.example.spotipeng.events;

public class MusicPlaybackShuffleEvent {
    private final int status;
    public MusicPlaybackShuffleEvent(int status){this.status = status; }

    public int getStatus() { return status; }
}
