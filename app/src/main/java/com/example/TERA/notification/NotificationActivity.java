package com.example.TERA.notification;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.TERA.R;
import com.google.firebase.auth.FirebaseAuth;

public class NotificationActivity extends AppCompatActivity {

    private NotificationViewModel notificationViewModel;
    private RecyclerView rvNotification;
    private NotificationAdapter notificationAdapter;
    private ProgressBar progressBar;
    private TextView no_notification;
    private String uid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        /// untuk nampilin back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("Notification");

        notificationViewModel = new ViewModelProvider(this, new ViewModelProvider.NewInstanceFactory()).get(NotificationViewModel.class);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        progressBar = findViewById(R.id.notification_pb);
        no_notification = findViewById(R.id.no_notification);
        rvNotification = findViewById(R.id.rv_notification);

        rvNotification.setLayoutManager(new LinearLayoutManager(this));
        notificationAdapter = new NotificationAdapter();
        notificationAdapter.notifyDataSetChanged();
        rvNotification.setAdapter(notificationAdapter);

        setDataToNotificationList();

    }

    private void setDataToNotificationList() {
        progressBar.setVisibility(View.VISIBLE);
        notificationViewModel.setNotificationList(uid);
        notificationViewModel.getNotificationList().observe(this, notificationSignal -> {
            if(notificationSignal != null) {
                no_notification.setVisibility(View.INVISIBLE);
                notificationAdapter.setData(notificationSignal);
            } else {
                no_notification.setVisibility(View.VISIBLE);
            }
            progressBar.setVisibility(View.INVISIBLE);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}