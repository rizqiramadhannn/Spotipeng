package com.example.spotipeng.events;

public class MusicPlaybackLoopEvent {
    private final int status;
    public MusicPlaybackLoopEvent(int status){this.status = status; }

    public int getStatus() { return status; }
}
