package com.example.spotipeng;
//test
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spotipeng.events.MusicPlaybackStartedEvent;
import com.example.spotipeng.events.StartFragmentEvent;
import com.example.spotipeng.model.Song;
import com.example.spotipeng.service.MusicService;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    List<Song> songs;

    MediaPlayer mediaPlayer;
    private MusicPlaybackListener musicPlaybackListener;

    private static final int LIST = 1;
    private static final int GRID = 2;

    private int viewMode;

    public SongAdapter(Context context, List<Song> songs, int viewMode) {
        this.context = context;
        this.songs = songs;
        this.mediaPlayer = new MediaPlayer();
        this.musicPlaybackListener = musicPlaybackListener;
        this.viewMode = viewMode;
    }

    public void setViewMode(int viewMode) {
        this.viewMode = viewMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutResId = (viewMode == LIST) ? R.layout.song_row_item : R.layout.song_grid_item;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false);
        return new SongViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Song song = songs.get(position);
        SongViewHolder viewHolder = (SongViewHolder) holder;

        viewHolder.titleHolder.setText(song.getTitle());
        viewHolder.artistHolder.setText(song.getSinger());

        String artworkUri = song.getAlbum();
        if (artworkUri != null){
            Picasso.get().load(artworkUri).into(viewHolder.artworkHolder);
            if (viewHolder.artworkHolder.getDrawable() == null){
                viewHolder.artworkHolder.setImageResource(R.drawable.ic_default_artwork);
            }
        }

        viewHolder.itemView.setOnClickListener(view -> {
            EventBus.getDefault().post(new StartFragmentEvent());
            EventBus.getDefault().post(new MusicPlaybackStartedEvent(song));
            Intent serviceIntent = new Intent(context, MusicService.class);
            serviceIntent.setAction("PLAY");
            context.startService(serviceIntent);
        });
    }


    public interface MusicPlaybackListener {
        void onMusicPlaybackStarted(Song song);
        void onMusicPlaybackStopped();
    }


    public static class SongViewHolder extends RecyclerView.ViewHolder{
        ImageView artworkHolder;
        TextView titleHolder;
        TextView artistHolder;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            artworkHolder = itemView.findViewById(R.id.artworkView);
            titleHolder = itemView.findViewById(R.id.titleView);
            artistHolder = itemView.findViewById(R.id.artistView);
        }
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filterSongs(List<Song> filteredList){
        songs = filteredList;
        notifyDataSetChanged();
    }
}
