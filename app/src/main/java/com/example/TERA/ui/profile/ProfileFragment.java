package com.example.TERA.ui.profile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.TERA.R;
import com.example.TERA.ShowProfileActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import java.util.HashMap;

import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment {

    //Firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    private FirebaseDatabase database;
    private DatabaseReference databaseReference;
    private EditText usernameEt;
    private EditText emailEt;
    private EditText phoneNumberEt;
    private ProgressBar progressBar;
    private Button btnSave;
    private Dialog dialog;
    private ImageView add_user_avatar;
    private ImageView avatar_user;
    private StorageReference storageReference;
    private ProfileViewModel profileViewModel;

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;

    //path where image will be created
    String storagePath = "User_Profile_Imgs/";

    String cameraPermission[];
    String storagePermission[];

    //uri of picked image, untuk mengambil nama dari file foto profile
    Uri image_uri;

    //for checking profile picture
    String profile;
    String avatar;

    Uri personPhoto;


    private GoogleSignInAccount googleSignInAccount;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        profileViewModel =
                new ViewModelProvider(this).get(ProfileViewModel.class);
        googleSignInAccount = GoogleSignIn.getLastSignedInAccount(getActivity());
        View root = (googleSignInAccount != null)
                ? inflater.inflate(R.layout.fragment_profile2, container, false)
                : inflater.inflate(R.layout.fragment_profile, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();
        storageReference = FirebaseStorage.getInstance().getReference();

        //init array of permission
        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        usernameEt = root.findViewById(R.id.etUsername);
        emailEt = root.findViewById(R.id.etEmail);
        phoneNumberEt = root.findViewById(R.id.etPhoneNumber);
        progressBar = root.findViewById(R.id.progress_bar);
        btnSave = root.findViewById(R.id.save);
        add_user_avatar = root.findViewById(R.id.add_user_avatar);
        avatar_user = root.findViewById(R.id.imageView3);

        usernameEt.setVisibility(View.GONE);
        emailEt.setVisibility(View.GONE);
        phoneNumberEt.setVisibility(View.GONE);

        emailEt.setEnabled(false);


        //Log.d("test", googleSignInAccount.toString());

        /// Load Data Admin
        if (googleSignInAccount != null) {
            loadAdminDataFromGoogleSignIn();
            btnSave.setVisibility(View.INVISIBLE);
            add_user_avatar.setVisibility(View.INVISIBLE);
            usernameEt.setEnabled(false);
        } else {
            loadAdminDataFromFirebase();
        }


        return root;
    }

    private void loadAdminDataFromGoogleSignIn() {
        String personName = googleSignInAccount.getDisplayName();
        String personEmail = googleSignInAccount.getEmail();
        personPhoto = googleSignInAccount.getPhotoUrl();

        //set data to app
        usernameEt.setText(personName);
        emailEt.setText(personEmail);

        Glide.with(ProfileFragment.this).load(personPhoto)
                .placeholder(R.drawable.circle1)
                .error(R.drawable.circle1)
                .apply(RequestOptions.circleCropTransform())
                .into(avatar_user);

        /// ketika proses selesai, maka progress bar akan invisible
        progressBar.setVisibility(View.INVISIBLE);
        /// kolom username, dan email bakal di tampilin
        usernameEt.setVisibility(View.VISIBLE);
        emailEt.setVisibility(View.VISIBLE);
    }

    private void loadAdminDataFromFirebase() {
        String email = user.getEmail();
        String uid = user.getUid();
        Log.d("UID", uid);
        // Queri berfungsi untuk meretrieve/get data dari firebase, untuk ditampilkan ke aplikasi
        // terdapat batasan, ketika suatu akun yang di cari itu gak ketemu, maka sistem akan terus mencari data di dalam folder tersebut.
        databaseReference.child("Admins").child(uid).orderByChild("email").equalTo(email)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        /// Ketika sistem load data, maka progresbar jadi visible
                        progressBar.setVisibility(View.VISIBLE);

                        /// ketika kita bolak balik halaman, otomatis akan mempengaruhi siklus fragment,
                        /// jadi kita kasi kondisi dimana kalau fragment tersebut bernilai null,
                        // maka kembalian / return null pula, sehingga tidak menyebabkan error
                        if (getActivity() == null) {
                            return;
                        }


                        for (DataSnapshot ds : snapshot.getChildren()) {
                            //get data
                            String username = "" + ds.child("username").getValue();
                            String email = "" + ds.child("email").getValue();
                            avatar = "" + ds.child("avatar").getValue();
                            String phoneNumber = "" + ds.child("phoneNumber").getValue();

                            Log.d("PRINT: ", username + " " + email);

                            //set data to app
                            usernameEt.setText(username);
                            emailEt.setText(email);
                            phoneNumberEt.setText(phoneNumber);

                            Glide.with(ProfileFragment.this).load(avatar)
                                    .placeholder(R.drawable.circle1)
                                    .error(R.drawable.circle1)
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(avatar_user);

                            /// ketika proses selesai, maka progress bar akan invisible
                            progressBar.setVisibility(View.INVISIBLE);
                            /// kolom username, email, dan phoneNumber bakal di tampilin
                            usernameEt.setVisibility(View.VISIBLE);
                            emailEt.setVisibility(View.VISIBLE);
                            phoneNumberEt.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConformationAlertDialog();
            }
        });

        add_user_avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getImageWithCameraOrGallery();
            }
        });

        avatar_user.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), ShowProfileActivity.class);
                /// kita mengirim link dari avatar yang didapat dari firebase
                if (googleSignInAccount != null) {
                    intent.putExtra("EXTRA_AVATAR_URI", personPhoto.toString());
                } else {
                    intent.putExtra("EXTRA_AVATAR_URI", avatar);
                }
                startActivity(intent);
            }
        });

    }

    private void getImageWithCameraOrGallery() {
        profile = "avatar";
        String[] option;
        if (avatar.isEmpty()) {
            option = new String[]{"Camera", "Gallery"};
        } else {
            option = new String[]{"Camera", "Gallery", "Remove Picture"};
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Image take Mode");
        builder.setItems(option, new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (which == 0) {
                    //kamera clicked
                    if (!checkCameraPermission()) {
                        requestCameraPermission();
                    } else {
                        pickFromCamera();
                    }
                } else if (which == 1) {
                    //galeri clicked
                    if (!checkStoragePermission()) {
                        requsetStoragePermission();
                    } else {
                        pickFromGaleri();
                    }
                } else {
                    removePicture();
                }
            }
        });
        builder.create().show();
    }

    @SuppressLint("SetTextI18n")
    private void removePicture() {
        dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.custom_dialog3);
        TextView tvConfirmation = dialog.findViewById(R.id.textView2);
        tvConfirmation.setText("Are you sure you want to Delete this Profile?");
        Button btnYes = dialog.findViewById(R.id.btn_Yes1);
        Button btnNo = dialog.findViewById(R.id.btn_No2);
        btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                deleteLinkPictureOnDB();
            }
        });

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private void deleteLinkPictureOnDB() {
        progressBar.setVisibility(View.VISIBLE);
        HashMap<String, Object> result = new HashMap<>();
        result.put("avatar", "");

        databaseReference.child("Admins").child(user.getUid()).child("User").updateChildren(result)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(getContext(), "Your Profile Picture was Removed", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void pickFromGaleri() {
        Intent galeryIntent = new Intent(Intent.ACTION_PICK);
        galeryIntent.setType("image/*");
        startActivityForResult(galeryIntent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pict");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");

        //put image uri
        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values);

        //intent to start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }

    private void requsetStoragePermission() {
        requestPermissions(storagePermission, STORAGE_REQUEST_CODE);
    }

    private void requestCameraPermission() {
        requestPermissions(cameraPermission, CAMERA_REQUEST_CODE);
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
    }

    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (cameraAccepted && writeStorageAccepted) {
                        pickFromCamera();
                    } else {
                        Toast.makeText(getActivity(), "Please, Allow TERA to Access the Camera and using the Storage Access", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;

            case STORAGE_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (writeStorageAccepted) {
                        pickFromGaleri();
                    } else {
                        Toast.makeText(getActivity(), "Allow Storage Access", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (resultCode == RESULT_OK) {

            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                //image is picked from gallery, get uri image
                image_uri = data.getData();
                uploadProfileCoverPhoto(image_uri);
            }

            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                uploadProfileCoverPhoto(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadProfileCoverPhoto(final Uri image_uri) {
        //show progress bar
        progressBar.setVisibility(View.VISIBLE);

        //path and name of image ti be stored in firebase storage
        String filePathAndName = storagePath + "" + profile + "_" + user.getUid();

        StorageReference storageReference2nd = storageReference.child(filePathAndName);
        storageReference2nd.putFile(image_uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();

                        while (!uriTask.isSuccessful()) ;
                        final Uri downloadUri = uriTask.getResult();

                        //check if image is uploaded or not and uri is received
                        if (uriTask.isSuccessful()) {
                            //image uploaded
                            //add/update uri in user's datase
                            HashMap<String, Object> result = new HashMap<>();
                            result.put(profile, downloadUri.toString());

                            databaseReference.child("Admins").child(user.getUid()).child("User").updateChildren(result)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            progressBar.setVisibility(View.INVISIBLE);
                                            Toast.makeText(getActivity(), "Your Profile Picture was Changed", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            progressBar.setVisibility(View.INVISIBLE);
                                            Toast.makeText(getActivity(), "Sorry, Upload was Unsuccessful", Toast.LENGTH_SHORT).show();
                                        }
                                    });


                        }

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    @SuppressLint("SetTextI18n")
    private void showConformationAlertDialog() {
        dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.custom_dialog3);
        Button btnYes = dialog.findViewById(R.id.btn_Yes1);
        Button btnNo = dialog.findViewById(R.id.btn_No2);
        btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                /// ketika proses berjalan, maka progress bar akan tampil
                progressBar.setVisibility(View.VISIBLE);
                updateNameAndPhoneNumber();
            }
        });

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private void updateNameAndPhoneNumber() {
        String name = usernameEt.getText().toString().trim();
        String phoneNumber = phoneNumberEt.getText().toString().trim();
        boolean digitOnly = TextUtils.isDigitsOnly(phoneNumber);
        if (!name.isEmpty() && !phoneNumber.isEmpty() && digitOnly && name.length() <= 20) {
            HashMap<String, Object> result = new HashMap<>();
            result.put("username", name);
            result.put("phoneNumber", phoneNumber);


            databaseReference.child("Admins").child(user.getUid()).child("User").updateChildren(result)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(getContext(), "Data profile has been Updated", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

            /// Ketika inputan user failure, maka sistem akan me-reload ulang data admin, melalui method loadAdminDataFromFirebase();
        } else if (!digitOnly) {
            Toast.makeText(getContext(), "Phone Number not Valid", Toast.LENGTH_SHORT).show();
            loadAdminDataFromFirebase();
        } else if (name.length() > 20) {
            Toast.makeText(getContext(), "Your Display Name must be within 1-20 Character", Toast.LENGTH_SHORT).show();
            loadAdminDataFromFirebase();
        } else {
            Toast.makeText(getContext(), "Username or Phone Number can't be Empty", Toast.LENGTH_SHORT).show();
            loadAdminDataFromFirebase();
        }

        progressBar.setVisibility(View.INVISIBLE);

    }
}