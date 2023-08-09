package com.example.spotipeng.events;

import com.example.spotipeng.model.Song;

public class MusicPlaybackStartedEvent {
    private static Song song;

    public MusicPlaybackStartedEvent(Song song) {
        this.song = song;
    }

    public Song getSong() {
        return song;
    }

    public boolean getStatus() { return true; }
}