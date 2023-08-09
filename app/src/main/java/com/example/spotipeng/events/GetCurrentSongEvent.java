package com.example.spotipeng.events;

import com.example.spotipeng.model.Song;

public class GetCurrentSongEvent {

    Song song;
    public GetCurrentSongEvent(Song song){ this.song = song; }

    public Song getSong() {
        return song;
    }
}
