package com.example.TERA.ui.history;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.TERA.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class HistoryFragment extends Fragment implements DatePickerDialog.OnDateSetListener {

    private HistoryViewModel historyViewModel;
    private ProgressBar progressBar;
    private RecyclerView rvHistory;
    private HistoryAdapter historyAdapter;
    private FirebaseAuth auth;
    private TextView noHistory;
    private String getDateTime = "early";
    private Button btnDatePicker;
    private SimpleDateFormat simpleDateFormat;

    private String uid;

    @SuppressLint("SimpleDateFormat")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        historyViewModel =
                new ViewModelProvider(this).get(HistoryViewModel.class);
        View root = inflater.inflate(R.layout.fragment_history, container, false);
        //  final TextView textView = root.findViewById(R.id.text_history);
        //  historyViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
          //  @Override
           // public void onChanged(@Nullable String s) {
             //   textView.setText(s);
            //  }
        //  });

        historyViewModel = new ViewModelProvider(this, new ViewModelProvider.NewInstanceFactory()).get(HistoryViewModel.class);

        auth = FirebaseAuth.getInstance();
        uid = auth.getCurrentUser().getUid();

        rvHistory = root.findViewById(R.id.rv_history);
        progressBar = root.findViewById(R.id.history_pb);
        noHistory = root.findViewById(R.id.no_history);
        btnDatePicker = root.findViewById(R.id.btn_date_picker);

        rvHistory.setLayoutManager(new LinearLayoutManager(getActivity()));
        historyAdapter = new HistoryAdapter();
        historyAdapter.notifyDataSetChanged();
        rvHistory.setAdapter(historyAdapter);

        if(getDateTime.equals("early")) {
            simpleDateFormat = new SimpleDateFormat("dd MMM yyyy");
            getDateTime = simpleDateFormat.format(new Date());
            historyViewModel.setUserList(uid, getDateTime);
            btnDatePicker.setText(getDateTime);
        }

        setDataToHistoryUserList();



        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnDatePicker.setOnClickListener(view1 -> {
            DialogFragment datePicker = new DatePickerFragment();
            datePicker.setTargetFragment(HistoryFragment.this, 0);
            datePicker.show(getFragmentManager(), "date picker");
        });
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);

        simpleDateFormat = new SimpleDateFormat("dd MMM yyyy");
        getDateTime = simpleDateFormat.format(calendar.getTime());
        historyViewModel.setUserList(uid, getDateTime);
        btnDatePicker.setText(getDateTime);

    }

    private void setDataToHistoryUserList() {
        progressBar.setVisibility(View.VISIBLE);
        historyViewModel.getHistoryList().observe(getViewLifecycleOwner(), historyList -> {
            if(historyList.size() > 0) {
                noHistory.setVisibility(View.INVISIBLE);
                historyAdapter.setData(historyList);
                rvHistory.setVisibility(View.VISIBLE);
            } else {
                rvHistory.setVisibility(View.INVISIBLE);
                noHistory.setVisibility(View.VISIBLE);
            }
            progressBar.setVisibility(View.INVISIBLE);
        });
    }

}