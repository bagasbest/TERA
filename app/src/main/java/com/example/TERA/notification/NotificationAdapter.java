package com.example.TERA.notification;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.TERA.R;
import java.util.ArrayList;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {


    private final ArrayList<NotificationModel> notificationList = new ArrayList<>();
    public void setData(ArrayList<NotificationModel> notifications) {
        notificationList.clear();
        notificationList.addAll(notifications);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(notificationList.get(position));
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {

        TextView dateTime;
        TextView title;
        TextView description;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTime = itemView.findViewById(R.id.timestamp_notification);
            title = itemView.findViewById(R.id.title_notification);
            description = itemView.findViewById(R.id.description_notification);
        }

        public void bind(NotificationModel notificationModel) {
            dateTime.setText(notificationModel.getDateTime());
            title.setText(notificationModel.getTitle());
            description.setText(notificationModel.getDescription());
        }
    }
}
