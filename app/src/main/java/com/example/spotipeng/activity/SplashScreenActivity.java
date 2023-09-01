package com.example.spotipeng.activity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
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

        // Fade-in animation for the logo
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(logoImageView, "alpha", 0f, 1f);
        fadeIn.setDuration(1000); // Duration of the animation in milliseconds
        fadeIn.start();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Check if the user is already logged in
                SharedPreferences sharedPreferences = getSharedPreferences("spotipeng", MODE_PRIVATE);
                String token = sharedPreferences.getString("token", null);

                if (token != null && !token.isEmpty()) {
                    // User is logged in, navigate to the main activity
                    Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                    startActivity(intent);
                } else {
                    // User is not logged in, navigate to the login activity
                    Intent intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
                    startActivity(intent);
                }

                finish();
            }
        }, 3000); // Delay for 3 seconds before navigating
    }
}
