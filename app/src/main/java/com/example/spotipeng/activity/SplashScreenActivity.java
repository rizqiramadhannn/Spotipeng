package com.example.spotipeng.activity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spotipeng.R;

public class SplashScreenActivity extends AppCompatActivity {
    private ImageView logoImageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        logoImageView = findViewById(R.id.logoImageView);

        // Add any initialization or setup code here

        // Fade-in animation for the logo
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(logoImageView, "alpha", 0f, 1f);
        fadeIn.setDuration(1000); // Duration of the animation in milliseconds
        fadeIn.start();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, 3000);
    }
}