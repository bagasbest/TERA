package com.example.TERA.ui.application;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.TERA.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

public class ApplicationFragment extends Fragment {
    private Button button3;
    private ApplicationViewModel applicationViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        applicationViewModel =
                new ViewModelProvider(this).get(ApplicationViewModel.class);
        View root = inflater.inflate(R.layout.fragment_application, container, false);
        final TextView textView = root.findViewById(R.id.textView2);
        applicationViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        button3 = view.findViewById(R.id.button3);
        button3.setOnClickListener(vv -> Toast.makeText(getActivity(), "TERA APP V1.0", Toast.LENGTH_SHORT).show());

    }
}