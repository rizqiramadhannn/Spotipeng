package com.example.spotipeng;

import android.net.Uri;
public class Song {
    String singer;
    String title;
    String url;
    String album;

    public Song(String singer, String title, String url, String album) {
        this.title = title;
        this.url = url;
        this.album = album;
    }

    public String getSinger() {
        return singer;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getAlbum() {
        return album;
    }

}
