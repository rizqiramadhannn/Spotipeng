package com.example.spotipeng.activity;

import static com.example.spotipeng.R.id.emailEditText;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
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
import com.example.spotipeng.utils.Constants;
import com.google.android.material.snackbar.Snackbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {
    private EditText emailEditText, passwordEditText;
    private TextView registerTV;
    private Button loginButton;
    private JsonPlaceHolderApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerTV = findViewById(R.id.registerTextView);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL) // Replace with your API base URL
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(JsonPlaceHolderApi.class);
        SpannableString spannable = new SpannableString("Don't have an account? Register here");
        int color = ContextCompat.getColor(this, R.color.green);
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);
        spannable.setSpan(colorSpan, 23, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        registerTV.setText(spannable);
        registerTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivity(intent);
                finish(); // Optional: Close the registration activity
            }
        });
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = emailEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                if (email.equals("") || password.equals("")){
                    Snackbar.make(view, "Please fill all the form to log in", Snackbar.LENGTH_LONG).show();
                } else {
                    LoginPayload loginRequest = new LoginPayload(email, password);

                    Call<LoginResponse> call = api.login(loginRequest);
                    call.enqueue(new Callback<LoginResponse>() {
                        @Override
                        public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                            if (response.isSuccessful()) {
                                Snackbar.make(view, "Login Success", Snackbar.LENGTH_LONG).show();
                                Log.i("TAG", "onResponse: " + response.body());
                                LoginResponse loginResponse = response.body();
                                String token = loginResponse.getToken();
                                // Handle successful login, store token, navigate to another activity, etc.
                                // Storing the token in SharedPreferences
                                SharedPreferences sharedPreferences = getSharedPreferences("spotipeng", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("token", token);
                                editor.apply();
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                            } else {
                                // Handle login failure
                                Snackbar.make(view, "Login failed. Please check your credentials.", Snackbar.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<LoginResponse> call, Throwable t) {
                            // Handle network or API call failure
                            Snackbar.make(view, "Network or API call failed. Please try again later.", Snackbar.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }
}
