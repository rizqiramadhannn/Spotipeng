package com.example.spotipeng;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class Song implements Parcelable {
    String singer;
    String title;
    String url;
    String album;

    // Other constructors and methods

    public Song(String singer, String title, String url, String album) {
        this.singer = singer;
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

    protected Song(Parcel in) {
        singer = in.readString();
        title = in.readString();
        url = in.readString();
        album = in.readString();
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(singer);
        dest.writeString(title);
        dest.writeString(url);
        dest.writeString(album);
    }
}
