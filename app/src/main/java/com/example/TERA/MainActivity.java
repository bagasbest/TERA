package com.example.TERA;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences preferences = getSharedPreferences("login", Context.MODE_PRIVATE);
        //waktu splash screen selama 2 detik
        int time = 2000;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //fungsi intent yaitu perpindahan dari satu tampilan ke tampilan lainnya
                if (preferences.contains("isUserLogin")) {
                    Intent intent = new Intent(MainActivity.this, Main2Activity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        }, time);
    }
}