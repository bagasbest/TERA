package com.example.TERA.ui.history;

import android.util.Log;

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

public class HistoryViewModel extends ViewModel {

    private final MutableLiveData<ArrayList<HistoryModel>> listUser = new MutableLiveData<>();

    public void setUserList(String uid, String dateTime) {
        Log.d("TEST: ", uid + " " + dateTime);
        final ArrayList<HistoryModel> historyArrayList = new ArrayList<>();

        try {
            DatabaseReference historyReference = FirebaseDatabase.getInstance().getReference("History").child(uid).child(dateTime);
            ValueEventListener eventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for(DataSnapshot ds : snapshot.getChildren()) {
                        String lastDateTime = "" + ds.child("lastDateTime").getValue();

                        int lenY = lastDateTime.length();

                        HistoryModel historyModel = new HistoryModel();
                        historyModel.setLastLatitude(Double.parseDouble("" + ds.child("lastLatitude").getValue()));
                        historyModel.setLastLongitude(Double.parseDouble("" + ds.child("lastLongitude").getValue()));
                        historyModel.setLastDateTime(lastDateTime.substring(13, lenY));

                        historyArrayList.add(historyModel);
                    }

                    listUser.postValue(historyArrayList);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            historyReference.addListenerForSingleValueEvent(eventListener);
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    public LiveData<ArrayList<HistoryModel>> getHistoryList() {
        return listUser;
    }
}