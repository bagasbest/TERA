package com.example.TERA.notification;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class NotificationViewModel extends ViewModel {

    private final MutableLiveData<ArrayList<NotificationModel>> listNotification = new MutableLiveData<>();

    public void setNotificationList(String uid) {
        final ArrayList<NotificationModel> notificationArrayList = new ArrayList<>();

        try {
            DatabaseReference historyReference = FirebaseDatabase.getInstance().getReference("Notification").child(uid);
            ValueEventListener eventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for(DataSnapshot ds : snapshot.getChildren()) {
                        NotificationModel notificationModel = new NotificationModel();
                        notificationModel.setDateTime("" + ds.child("dateTime").getValue());
                        notificationModel.setTitle("" + ds.child("title").getValue());
                        notificationModel.setDescription("" + ds.child("description").getValue());

                        notificationArrayList.add(notificationModel);
                    }

                    listNotification.postValue(notificationArrayList);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            historyReference.addListenerForSingleValueEvent(eventListener);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public LiveData<ArrayList<NotificationModel>> getNotificationList() {
        return listNotification;
    }

}
