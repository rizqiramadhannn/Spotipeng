package com.example.spotipeng;

import android.net.Uri;
public class Song {
    String title;
    Uri uri;
    Uri artworkUri;
    int size;
    int duration;

    public Song(String title, Uri uri, Uri artworkUri, int duration) {
        this.title = title;
        this.uri = uri;
        this.artworkUri = artworkUri;
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }

    public Uri getUri() {
        return uri;
    }

    public Uri getArtworkUri() {
        return artworkUri;
    }

    public int getDuration() {
        return duration;
    }
}
