package com.example.TERA.ui.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.TERA.R;

import java.util.ArrayList;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final ArrayList<HistoryModel> userList = new ArrayList<>();
    public void setData(ArrayList<HistoryModel> users) {
        userList.clear();
        userList.addAll(users);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryAdapter.HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryAdapter.HistoryViewHolder holder, int position) {
        holder.bind(userList.get(position));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {

        TextView tvLastLatitude;
        TextView tvLastLongitude;
        TextView tvLastDateTime;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLastLatitude = itemView.findViewById(R.id.lastLatitude);
            tvLastLongitude = itemView.findViewById(R.id.lastLongitude);
            tvLastDateTime = itemView.findViewById(R.id.lastDateTime);
        }

        public void bind(HistoryModel historyModel) {
            tvLastLatitude.setText(String.valueOf(historyModel.getLastLatitude()));
            tvLastLongitude.setText(String.valueOf(historyModel.getLastLongitude()));
            tvLastDateTime.setText(historyModel.getLastDateTime());
        }
    }
}
