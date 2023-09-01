package com.example.spotipeng.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Song implements Parcelable {
    String Singer;
    String Title;
    String URL;
    String Album;
    private int Duration;

    // Other constructors and methods

    public Song(String Singer, String Title, String URL, String Album) {
        this.Singer = Singer;
        this.Title = Title;
        this.URL = URL;
        this.Album = Album;
    }



    public int getDuration() {
        return Duration;
    }

    public void setDuration(int Duration) {
        this.Duration = Duration;
    }

    public String getSinger() {
        return Singer;
    }

    public String getTitle() {
        return Title;
    }

    public String getUrl() {
        return URL;
    }

    public String getAlbum() {
        return Album;
    }

    protected Song(Parcel in) {
        Singer = in.readString();
        Title = in.readString();
        URL = in.readString();
        Album = in.readString();
        Duration = in.readInt();
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
        dest.writeString(Singer);
        dest.writeString(Title);
        dest.writeString(URL);
        dest.writeString(Album);
        dest.writeInt(Duration);
    }
}
