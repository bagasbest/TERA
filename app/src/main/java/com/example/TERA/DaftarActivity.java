package com.example.TERA;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class DaftarActivity extends AppCompatActivity implements View.OnClickListener {
    //deklarasi beberapa variabel kayak button editext, databaserefrence, firebaseauth
    private static final String TAG = "LoginActivity";

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private EditText daftaremail, daftarname, daftarpass, daftarphone;
    private TextView backtologin;
    private Button daftar;
    private Dialog dialog;
    private Button btnDismiss, btnDismiss2;
    private TextView tvEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daftar);
        //Variabel tadi untuk memanggil fungsi
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        //Diatur sesuai id komponennya
        daftaremail = findViewById(R.id.daftaremail);
        daftarname = findViewById(R.id.daftarname);
        daftarpass = findViewById(R.id.daftarpass);
        daftarphone = findViewById(R.id.daftarPhoneNumber);
        backtologin = findViewById(R.id.backtologin);
        daftar = findViewById(R.id.daftar);

        //Menambahkan method onClick, biar tombolnya bisa diklik
        daftar.setOnClickListener(this);
        backtologin.setOnClickListener(this);
    }

    //Fungsi ini untuk mendaftarkan data pengguna ke Firebase
    private void signUp() {
        Log.d(TAG, "signUp");
        if (!validateForm()) {
            daftar.setEnabled(true);
            return;
        }

        //ShowProgressDialog();
        String email = daftaremail.getText().toString();
        String password = daftarpass.getText().toString();

        // untuk daftar user
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUser:onComplete:" + task.isSuccessful());
                        //HideProgressDialog();
                        if (task.isSuccessful()) {

                            // kirim verifikasi
                            FirebaseAuth.getInstance().getCurrentUser().sendEmailVerification()
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            onAuthSuccess(task.getResult().getUser());

                                            //Menampilkan alert dialog ketika sukses registrasi
                                            showAlertDialog();
                                        } else {
                                            daftar.setEnabled(true);
                                            try {
                                                throw Objects.requireNonNull(task.getException());
                                            } catch (Exception e) {
                                                Toast.makeText(getApplicationContext(), "Ups, terdapat kendala ketika ingin mengirim email verifikasi", Toast.LENGTH_SHORT).show();
                                                Log.e(TAG, e.getMessage());
                                            }
                                        }
                                    });
                        } else {
                            daftar.setEnabled(true);
                            try {
                                throw Objects.requireNonNull(task.getException());
                            } catch (FirebaseAuthUserCollisionException e) {
                                Toast.makeText(getApplicationContext(), "The Email Address is Already in Use by Another Account.", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                showAlertDialogNetworkError();
                                //Toast.makeText(getApplicationContext(), "Current Network is Unavailable. Please Check if you have Connected to a Valid Wifi or Mobile Network", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, e.getMessage());
                            }

                        }
                    }
                })
                .addOnFailureListener(e -> Log.d("NO INTERNET: ", e.toString()));
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

    @SuppressLint("SetTextI18n")
    private void showAlertDialog() {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.custom_dialog);
        btnDismiss = dialog.findViewById(R.id.btn_email);
        tvEmail = dialog.findViewById(R.id.textView4);
        String email = daftaremail.getText().toString();
        tvEmail.setText(tvEmail.getText().toString().trim() + "\n" + email);
        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                startActivity(new Intent(DaftarActivity.this, LoginActivity.class));
                finish();
            }
        });

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    //Fungsi dipanggil ketika proses Authentikasi berhasil
    private void onAuthSuccess(FirebaseUser user) {
        String email = daftaremail.getText().toString();
        String name = daftarname.getText().toString();
        String password = daftarpass.getText().toString();
        String phoneNumber = daftarphone.getText().toString();
        String avatar = "";

        //Membuat User admin baru
        writeNewAdmin(user.getUid(), name, email, password, avatar, phoneNumber);

    }

    private boolean validateForm() {
        boolean result = true;
        if (TextUtils.isEmpty(daftaremail.getText().toString())) {
            daftaremail.setError("Required");
            result = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(daftaremail.getText()).matches()) {
            daftaremail.setError("Email not Valid");
            result = false;
        } else {
            daftaremail.setError(null);
        }
        if (TextUtils.isEmpty(daftarname.getText().toString())) {
            daftarname.setError("Required");
            result = false;
        } else if (daftarname.getText().toString().length() > 20) {
            Toast.makeText(this, "Your Display Name must be within 1-20 Character", Toast.LENGTH_SHORT).show();
            result = false;
        }
        else {
            daftarname.setError(null);
        }
        if (TextUtils.isEmpty(daftarpass.getText().toString())) {
            Toast.makeText(DaftarActivity.this, "Password Required",
                    Toast.LENGTH_SHORT).show();
            result = false;
        }else if (daftarpass.getText().length() < 6 || (daftarpass.getText().length() > 16 )) {
            Toast.makeText(DaftarActivity.this, "Password must be at least 6-16 Character",
                    Toast.LENGTH_SHORT).show();
            result = false;
        }
        if (TextUtils.isEmpty(daftarphone.getText().toString())) {
            daftarphone.setError("Required");
            result = false;
        } else if(!TextUtils.isDigitsOnly(daftarphone.getText().toString())) {
            daftarphone.setError("Phone Number not Valid");
            result = false;
        } else {
            daftarphone.setError(null);
        }

        return result;
    }

    // Menulis ke Database
    private void writeNewAdmin(String userId, String name, String email, String pass, String avatar, String phoneNumber) {
        Admin admin = new Admin(name, email, pass, avatar, phoneNumber);
        mDatabase.child("Admins").child(userId).child("User").setValue(admin);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.daftar) {
            daftar.setEnabled(false);
            signUp();
        } else if (i == R.id.backtologin) {
            Intent tologin = new Intent(DaftarActivity.this, LoginActivity.class);

            //Start Activity untuk Menjalankan Intent
            startActivity(tologin);
            finish();
        }
    }

    public void ShowHidePass(View view) {
        if (view.getId() == R.id.show_pass_btn) {

            if (daftarpass.getTransformationMethod().equals(PasswordTransformationMethod.getInstance())) {
                ((ImageView) (view)).setImageResource(R.drawable.passicon2);

                //Show Password
                daftarpass.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                ((ImageView) (view)).setImageResource(R.drawable.passicon);

                //Hide Password
                daftarpass.setTransformationMethod(PasswordTransformationMethod.getInstance());

            }
        }
    }
}