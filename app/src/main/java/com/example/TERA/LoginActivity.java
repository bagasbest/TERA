package com.example.TERA;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {
    //Deklarasi item yang digunakan pada tampilan
    EditText email, pass;
    Button loginn;
    TextView daftar, forgot;
    String getemail, getpass;
    FirebaseAuth firebaseAuth;
    private ProgressBar progressBar;
    private Task<Void> mDatabase;
    private FirebaseAuth mAuth;
    private Dialog dialog;
    private CheckBox cb_rememberMe;

    public static final String SHARED_PREFS = "shared_pred";
    public static final String TEXT = "text";
    public static final String SWITCH = "switch";

    private String text;
    private boolean remembered;
    private GoogleSignInClient mGoogleSignInClient;
    private final static int RC_SIGN_IN = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();

        //Instansiasi
        email = findViewById(R.id.email);
        forgot = findViewById(R.id.forgotlink);
        pass = findViewById(R.id.pass);
        progressBar = findViewById(R.id.progress_barLogin);
        loginn = findViewById(R.id.LOGIN);
        daftar = findViewById(R.id.DAFTAR);
        cb_rememberMe = findViewById(R.id.remember_me);
        Button btnSignIn = findViewById(R.id.google_signin);
        btnSignIn.setStateListAnimator(null);

        loginn.setOnClickListener(new View.OnClickListener() {
            @Override
            //Fungsi kliknya
            public void onClick(View v) {
                loginn.setEnabled(false);
                getemail = email.getText().toString();
                getpass = pass.getText().toString();
                loginUserAccount();

                if (cb_rememberMe.isChecked()) {
                    boolean cheked = cb_rememberMe.isChecked();
                    saveData(cheked);
                } else {
                    boolean cheked = cb_rememberMe.isChecked();
                    saveData(cheked);
                }

            }

        });
        forgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent forgotintent = new Intent(LoginActivity.this, ForgotPassword.class);
                startActivity(forgotintent);
                finish();
            }
        });
        daftar.setOnClickListener(new View.OnClickListener() {
            @Override
            //Fungsi kliknya
            public void onClick(View v) {
                Intent regis = new Intent(LoginActivity.this, DaftarActivity.class);
                startActivity(regis);
                finish();
            }
        });


        createRequest();
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });

        loadData();
        updateViews();

    }

    private void createRequest() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void signIn() {
        progressBar.setVisibility(View.VISIBLE);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("TAG", "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                if (e.getStatusCode() == 7 || e.getStatusCode() == 22) {
                    showAlertDialog();
                }
                progressBar.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();

                            checkIfHasDataBefore(user);

                            Intent intent = new Intent(getApplicationContext(), Main2Activity.class);
                            startActivity(intent);
                            progressBar.setVisibility(View.INVISIBLE);
                            finish();
                        } else {
                            displayToast("Sorry, we couldn't complete your request. Please check your connection or try again");
                        }
                    }
                });
    }

    private void checkIfHasDataBefore(FirebaseUser user) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Admins").child(user.getUid()).child("User");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!snapshot.hasChild("username")) {
                    saveUserToDB(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void saveUserToDB(FirebaseUser user) {
        try {
            HashMap<Object, String> document = new HashMap<>();
            document.put("avater", user.getPhotoUrl().toString());
            document.put("email", user.getEmail());
            document.put("username", user.getDisplayName());

            String uid = user.getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference("Admins").child(uid).child("User").setValue(document);


        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void displayToast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    private void loginUserAccount() {
        getemail = email.getText().toString();
        getpass = pass.getText().toString();
        if (!validateForm()) {
            loginn.setEnabled(true);
            return;
        }
        /// ini fungsi untuk login
        mAuth.signInWithEmailAndPassword(getemail, getpass)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        //Mengecek status keberhasilan saat login
                        if (task.isSuccessful()) {

                            if (mAuth.getCurrentUser().isEmailVerified()) {
                                Toast.makeText(getApplicationContext(), "Login Success", Toast.LENGTH_SHORT).show();
                                Intent sukses = new Intent(LoginActivity.this, Main2Activity.class);
                                startActivity(sukses);
                                finish();
                            } else {
                                loginn.setEnabled(true);
                                Toast.makeText(getApplicationContext(), "Please Verify your Account by Checking The Email you Registered", Toast.LENGTH_SHORT).show();
                            }
                        } else if (!task.isSuccessful()) {
                            loginn.setEnabled(true);
                            try {
                                LoginFailedException(task);
                            } catch (Exception e) {
                                showAlertDialog();

                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.d("NO INTERNET: ", e.toString()));
    }

    private void showAlertDialog() {
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

    private boolean validateForm() {
        boolean result = true;
        if (TextUtils.isEmpty(email.getText().toString())) {
            email.setError("Required");
            result = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email.getText()).matches()) {
            email.setError("Email not Valid");
            result = false;
        } else {
            email.setError(null);
        }

        if (TextUtils.isEmpty(pass.getText().toString())) {
            Toast.makeText(LoginActivity.this, "Password Required",
                    Toast.LENGTH_SHORT).show();
            result = false;
        } else if (pass.getText().length() < 6 || (pass.getText().length() > 16)) {
            Toast.makeText(LoginActivity.this, "Password must be at least 6-16 Character",
                    Toast.LENGTH_SHORT).show();
            result = false;
        }
        return result;
    }

    public void ShowHidePass(View view) {
        if (view.getId() == R.id.show_pass_btn) {

            if (pass.getTransformationMethod().equals(PasswordTransformationMethod.getInstance())) {
                ((ImageView) (view)).setImageResource(R.drawable.passicon2);

                //Show Password
                pass.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                ((ImageView) (view)).setImageResource(R.drawable.passicon);

                //Hide Password
                pass.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        }
    }

    private void LoginFailedException(Task<AuthResult> task) {
        String errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();


        switch (errorCode) {

//            case "ERROR_INTERNET_DISCONNECTED":
//                Toast.makeText(LoginActivity.this, "Current Network is Unavailable. Please Check if you have Connected to a Valid Wifi or Mobile Network", Toast.LENGTH_LONG).show();
//                break;

            case "ERROR_WRONG_PASSWORD":
                Toast.makeText(LoginActivity.this, "The password is invalid or the user does not have a password.", Toast.LENGTH_SHORT).show();
                break;

            case "ERROR_USER_NOT_FOUND":
                Toast.makeText(LoginActivity.this, "There is no user record corresponding to this identifier. The user may have been deleted.", Toast.LENGTH_SHORT).show();
                break;

        }
    }

    private void saveData(boolean cheked) {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        /// ketika check box di tekan, maka akan menyimpan email, tetapi sebaliknya kalo tidak ditekan, maka tidak menyimpan apapun
        editor.putString(TEXT, (cheked) ? email.getText().toString().trim() : "");
        editor.putBoolean(SWITCH, cheked);

        editor.apply();
    }

    public void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        text = sharedPreferences.getString(TEXT, "");
        remembered = sharedPreferences.getBoolean(SWITCH, false);
    }

    public void updateViews() {
        email.setText(text);
        cb_rememberMe.setChecked(remembered);
    }
}