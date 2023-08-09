package com.example.spotipeng.events;

import com.example.spotipeng.model.Song;

import java.util.List;

public class GetSongListEvent {
    List<Song> songs;
    public GetSongListEvent(List<Song> songs){ this.songs = songs; }

    public List<Song> getSongs() {
        return songs;
    }
}
