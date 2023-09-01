package com.example.spotipeng.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.spotipeng.R;
import com.example.spotipeng.api.JsonPlaceHolderApi;
import com.example.spotipeng.model.LoginPayload;
import com.example.spotipeng.model.LoginResponse;
import com.example.spotipeng.model.RegisterPayload;
import com.example.spotipeng.utils.Constants;
import com.google.android.material.snackbar.Snackbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RegistrationActivity extends AppCompatActivity {

    private EditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private TextView loginTextView;
    private JsonPlaceHolderApi api;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL) // Replace with your API base URL
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(JsonPlaceHolderApi.class);

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        loginTextView = findViewById(R.id.loginTextView);
        SpannableString spannable = new SpannableString("Already have an account? Login here");
        int color = ContextCompat.getColor(this, R.color.green);
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);
        spannable.setSpan(colorSpan, 24, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        loginTextView.setText(spannable);
        // Set a click listener for the register button
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get user input from the EditText fields
                String name = nameEditText.getText().toString();
                String email = emailEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                String confirmPassword = confirmPasswordEditText.getText().toString();

                // Perform registration logic here (e.g., validate input and send to server)
                if (email.equals("") || password.equals("") || name.equals("") || confirmPassword.equals("")){
                    Snackbar.make(view, "Please fill all the form to log in", Snackbar.LENGTH_LONG).show();
                } else if (!password.equals(confirmPassword)) {
                    Snackbar.make(view, "Passwords do not match. Please try again.", Snackbar.LENGTH_LONG).show();
                } else {
                    RegisterPayload registerPayload = new RegisterPayload(name, email, password);

                    Call<String> call = api.register(registerPayload);
                    call.enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            if (response.isSuccessful()) {
                                Snackbar.make(view, "Registration Success. Please log in", Snackbar.LENGTH_LONG).show();
                                Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                // Handle login failure
                                Snackbar.make(view, "Registration failed. An error has been occurred.", Snackbar.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            // Handle network or API call failure
                            Snackbar.make(view, "Network or API call failed. Please try again later.", Snackbar.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });

        // Set a click listener for the login text view to navigate to the login activity
        loginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Optional: Close the registration activity
            }
        });
    }
}
