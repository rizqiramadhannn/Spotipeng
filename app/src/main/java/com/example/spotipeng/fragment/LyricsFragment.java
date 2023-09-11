package com.example.spotipeng.fragment;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.spotipeng.R;
import com.example.spotipeng.activity.AllLyricsActivity;
import com.example.spotipeng.events.GetCurrentSongEvent;
import com.example.spotipeng.events.OpenLyricsEvent;
import com.example.spotipeng.model.Song;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LyricsFragment extends Fragment {
    FrameLayout lyricsContainer;
    private TextView lyricsTextView;
    private Song song;
    private int color;
    public LyricsFragment() {
        // Required empty public constructor
    }

    public static LyricsFragment newInstance(Song song, int color) {
        LyricsFragment fragment = new LyricsFragment();
        Bundle args = new Bundle();
        args.putInt("color", color);
        args.putParcelable("song", song);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            song = getArguments().getParcelable("song");
            color = getArguments().getInt("color", Color.BLACK);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_lyrics, container, false);
        lyricsTextView = view.findViewById(R.id.lyricsTextView);
        if (song.getLyrics().length() < 150){
            downloadLyrics(song.getLyrics());
        } else {
            lyricsTextView.setText(song.getLyrics());
        }


        // Get the background color of the CardView
        TypedValue typedValue = new TypedValue();
        getActivity().getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true);
        int[] attribute = new int[] { android.R.attr.colorBackground };
        TypedArray typedArray = getActivity().obtainStyledAttributes(typedValue.resourceId, attribute);
        int currentColor = typedArray.getColor(0, Color.BLACK);
        typedArray.recycle();
        // Calculate the color's lightness
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        float lightness = hsv[2]; // Extract lightness component

        // Check if the color is too light (close to white)
        if (lightness > 0.5f) {
            // Darken the color (e.g., by reducing lightness)
            lightness = 0.5f; // Adjust this value as needed
            hsv[2] = lightness;
            color = Color.HSVToColor(hsv);
        }
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), currentColor, color);
        colorAnimation.setDuration(500); // Set the duration in milliseconds
        colorAnimation.addUpdateListener(animator -> {
            int color = (int) animator.getAnimatedValue();
            view.setBackgroundColor(color);
        });
        colorAnimation.start();
        // Set an onClickListener to open the AllLyricsActivity when the lyrics are clicked
        lyricsTextView.setOnClickListener(v -> {
            EventBus.getDefault().post(new OpenLyricsEvent());
            Log.d("MusicService", "Lyrics TextView clicked");
            Intent intent = new Intent(getActivity(), AllLyricsActivity.class);
            intent.putExtra("song", song);
            intent.putExtra("color", color);
            startActivity(intent);
        });

        return view;
    }

    private void downloadLyrics(String lyricsUrl) {
        // You can use an AsyncTask or another background thread to download the lyrics
        // Here, I'll provide an example using AsyncTask
        new DownloadLyricsTask().execute(lyricsUrl);
    }

    private class DownloadLyricsTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String url = params[0];
            try {
                return downloadLyricsFromUrl(url);
            } catch (IOException e) {
                e.printStackTrace();
                return "Failed to download lyrics.";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    lyricsTextView.setText(result); // Update the TextView with the lyrics
                }
            });

            song.setLyrics(result); // Set the lyrics in the song object (if needed)
        }

    }

    private String downloadLyricsFromUrl(String url) throws IOException {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        StringBuilder lyrics = new StringBuilder();

        try {
            // Create a URL object from the given URL string
            URL lyricsUrl = new URL(url);

            // Open a connection to the URL
            connection = (HttpURLConnection) lyricsUrl.openConnection();

            // Set the request method to GET
            connection.setRequestMethod("GET");

            // Connect to the URL
            connection.connect();

            // Check if the response code indicates success (HTTP 200 OK)
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                // Open an input stream to read the content
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;

                // Read the content line by line and append it to the lyrics StringBuilder
                while ((line = reader.readLine()) != null) {
                    lyrics.append(line).append("\n");
                }
            } else {
                // Handle the case where the request did not succeed (e.g., handle errors)
                return "Failed to retrieve lyrics. HTTP response code: " + connection.getResponseCode();
            }
        } finally {
            // Close the reader and the connection
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

        // Return the lyrics as a string
        return lyrics.toString();
    }

    @Override
    public void onStop() {
        //lyricsTextView.setText("");
        super.onStop();
    }
}
