package com.example.TERA;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

public class ForgotPassword extends AppCompatActivity implements View.OnClickListener {

    private EditText etEmail;
    private Button btnSend, btnOke;
    private TextView backToLoginPage, tvEmail;
    private Dialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        //Diatur sesuai id komponennya
        etEmail = findViewById(R.id.emailtext);
        btnSend = findViewById(R.id.forgotpass);
        backToLoginPage = findViewById(R.id.backtologin2);

        //Menambahkan method onClick, biar tombolnya bisa diklik
        btnSend.setOnClickListener(this);
        backToLoginPage.setOnClickListener(this);

    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.forgotpass) {
            btnSend.setEnabled(false);
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            String email = etEmail.getText().toString().trim();

            if (!validateForm(email)) {
                btnSend.setEnabled(true);
                return;
            }
            firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                       if(task.isSuccessful()){
                           showAlertDialog();
                       } else {
                           btnSend.setEnabled(true);
                           try {
                               forgotPasswordException(task);
                           } catch (Exception e) {
                               showAlertDialogNetworkError();
                               //Toast.makeText(getApplicationContext(), "Current Network is Unavailable. Please Check if you have Connected to a Valid Wifi or Mobile Network", Toast.LENGTH_SHORT).show();
                           }
                       }
                    })
                    .addOnFailureListener(e -> Log.d("NO INTERNET: ", e.toString()));


        }else if (i == R.id.backtologin2) {
           startActivity(new Intent(this, LoginActivity.class));
           finish();
        }
    }

    private void showAlertDialogNetworkError() {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.custom_dialog4);
        Button btnDismiss = dialog.findViewById(R.id.button_TryAgain);
        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }


    private boolean validateForm(String email) {
        boolean result = true;
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Required");
            result = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email not Valid");
            result = false;
        }
        return result;
    }


    @SuppressLint("SetTextI18n")
    private void showAlertDialog() {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.custom_dialog);
        btnOke = dialog.findViewById(R.id.btn_email);
        tvEmail = dialog.findViewById(R.id.textView4);
        String email = etEmail.getText().toString().trim();
        tvEmail.setText(tvEmail.getText().toString().trim() + "\n" + email);
        btnOke.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                startActivity(new Intent(ForgotPassword.this, LoginActivity.class));
                finish();
            }
        });

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private void forgotPasswordException(Task<Void> task) {
        String errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();

        switch (errorCode) {

//            case "ERROR_INTERNET_DISCONNECTED":
//                Toast.makeText(ForgotPassword.this, "Current Network is Unavailable. Please Check if you have Connected to a Valid Wifi or Mobile Network", Toast.LENGTH_LONG).show();
//                break;

            case "ERROR_USER_NOT_FOUND":
                Toast.makeText(ForgotPassword.this, "There is no user record corresponding to this identifier. The user may have been deleted.", Toast.LENGTH_SHORT).show();
                break;

        }
    }
}