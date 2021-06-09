package com.example.TERA.ui.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.example.TERA.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class LocationFragment extends Fragment implements OnMapReadyCallback {

    // varialbel itu tempat untuk nampung nilai
    // konstanta itu sama aja, tapi udah ada nilai di awalnya

    private Location currentLocation;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;

    // double = bilangan pecahan, string = huruf, int = bilangan bulat, bool = true atau false, adalah tipe data
    private double lastLatitude;
    private double lastLongitude;
    private String lastTimeStamp;
    private String timeStamp;
    private String getTimeStamp;
    private String realLatitude;
    private String realLongitude;
    private String timeStampForHistoryUser;

    // public staticatau private staticvariabel sering digunakan untuk konstanta
    private static String TAG = LocationFragment.class.getSimpleName();
    private GoogleMap mMap;
    private double historyLastLatitude;
    private double historyLastLongitude;
    private String realTimeStamp;

    private String latestNotificationCondition;
    private Query query;
    private int LATEST_PLACE_TONGKAT_USER = 1;

    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    SimpleDateFormat dateFormat;
    SimpleDateFormat dateFormatForHistoryUser;

    Marker userLocationMarker;

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "channel_01";
    private static final CharSequence CHANNEL_NAME = "TERA";


    @Override
    public void onStart() {
        /// Activity sudah terlihat tapi belum bisa berinteraksi. Method ini jarang dipakai, tapi bisa sangat berguna untuk mendaftarkan sebuah BroadcastReceiver untuk mengamati perubahan yang dapat mempengaruhi UI.
        super.onStart();
        // minta permission, aktifkan gps
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            // req permission
        }
    }

    /// Kebalikan dari onStart() Activity sudah tidak terlihat. Biasanya kita melakukan undo untuk pekerjaan yang dilakukan di dalam onStart().
    @Override
    public void onStop() {
        super.onStop();
        // misal ke halaman lain, proses yg lagi berjalan sekarang itu di berhenittiin
        stopLocationUpdates();
    }

    // ini method / fungsi yang pertama kali di jalanin
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // ini untuk memanggil file xml nya
        View root = inflater.inflate(R.layout.fragment_location, container, false);

        // inisiasi firebase
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();


//      send 1 second interval time repeated
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        //initialize fused location, untuk ngedapetin lokasi dari maps
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

        // method / fungsi untuk memanggil maps api, supaya bisa dimuat dalam aplikasi
        fetchLastLocation();


        // save user latitude & longitude every 1 minute interval
        // handler, untuk ngejalanin suatu proses di latar belakang, agar si pengguna gak perlu tau proses nya bagaimana,
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // cek dulu, apakah mikro kontroller on, jika ya, maka buat transaksi histori
                checkIfMicrocontrollerOn();
                handler.postDelayed(this, 60000);
            }
        }, 60000);

        // ini untuk nampilin notifikasi tiap 10 detik sekali
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // cek apakah tongkat dalam keadaan on atau off
                checkConditionSwitchButtonArduino();
                handler.postDelayed(this, 10000);
            }
        }, 10000);

        return root;

    }

    private void checkIfMicrocontrollerOn() {
        DatabaseReference refDataGps = FirebaseDatabase.getInstance().getReference("data_gps");
        refDataGps.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String stat = "" + snapshot.child("stat").getValue();
                String getStatus = stat.substring(0, 1);

                // kalo misal gps on, maka sistem akan bisa nge save history
                if (getStatus.equals("1")) {
                    saveLatitudeLongitudeUserHistory();
                } else {
                    // untuk nampilin output datri suatu proses yang telah dikerjakan
                    Log.d("HISTORY OFF: ", "Mikrokontroller Mati, Tidak mengirimkan history");
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void checkConditionSwitchButtonArduino() {

        // ambil data notifikasi terakhir untuk cek apakah notifikasi terakhir menunjukkan ON tau OFF
        DatabaseReference notification = FirebaseDatabase.getInstance().getReference("Notification").child(user.getUid());
        notification.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // cek apakah ini pengguna baru atau tidak
                if (snapshot.exists()) {
                    queryLimitToLast(notification);
                } else {
                    latestNotificationCondition = "DATA KOSONG";
                    loadDataGps(latestNotificationCondition);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadDataGps(String ln) {

        // load data_gps
        DatabaseReference data_gps = FirebaseDatabase.getInstance().getReference("data_gps");
        data_gps.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = "" + snapshot.child("stat").getValue();

                //potong status untuk dapetin status yang asli (0 atau 1)
                String getStatus = status.substring(0, 1);
                String rawTimeStamp = status.substring(5);

                rawTimeStamp = rawTimeStamp.replaceAll("\\.", " ");
                rawTimeStamp = rawTimeStamp.replaceAll("_", ", ");

                Calendar cal = Calendar.getInstance();
                @SuppressLint("SimpleDateFormat")
                SimpleDateFormat month_date = new SimpleDateFormat("MMM");
                cal.set(Calendar.MONTH, Integer.parseInt(rawTimeStamp.substring(3, 5)) - 1);

                // fixed time stamp, ex : 10 MAY 2021, 12:00:00
                String fixedTimeStamp = rawTimeStamp.substring(0, 2) + " " + month_date.format(cal.getTime()) + " " + rawTimeStamp.substring(6);

                Log.d(TAG, "Status: " + getStatus + " Last Status " + LocationFragment.this.latestNotificationCondition);

                // cek apakah notifikasi menyala atau tidak, dan cek juga apakah user ini pengguna baru atau bukan
                // last notifikasi meyala, dan notifikasi saat ini tidak, maka bisa di tampilkan notifikasinya
                if (ln.equals("GPS MENYALA") && getStatus.equals("0")) {
                    saveNotification("GPS TIDAK MENYALA", "GPS pada tongkat tuna netra tidak menyala", fixedTimeStamp);
                    pushNotificationGPS("GPS TIDAK MENYALA", "GPS pada tongkat tuna netra tidak menyala");

                    // last notifikasi tidak meyala, dan notifikasi saat ini menyala, maka bisa di tampilkan notifikasinya
                } else if (ln.equals("GPS TIDAK MENYALA") && getStatus.equals("1")) {
                    saveNotification("GPS MENYALA", "GPS pada tongkat tuna netra menyala", fixedTimeStamp);
                    pushNotificationGPS("GPS MENYALA", "GPS pada tongkat tuna netra menyala");

                    // last notifikasi kosong, dan notifikasi saat ini tidak, maka bisa di tampilkan notifikasinya
                } else if (ln.equals("DATA KOSONG") && getStatus.equals("0")) {
                    saveNotification("GPS TIDAK MENYALA", "GPS pada tongkat tuna netra tidak menyala", fixedTimeStamp);
                    pushNotificationGPS("GPS TIDAK MENYALA", "GPS pada tongkat tuna netra tidak menyala");

                    // last notifikasi kosong, dan notifikasi saat ini menyala, maka bisa di tampilkan notifikasinya
                } else if (ln.equals("DATA KOSONG") && getStatus.equals("1")) {
                    saveNotification("GPS MENYALA", "GPS pada tongkat tuna netra menyala", fixedTimeStamp);
                    pushNotificationGPS("GPS MENYALA", "GPS pada tongkat tuna netra menyala");
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, error.getMessage());
            }
        });
    }

    private void queryLimitToLast(DatabaseReference notification) {
        Query query = notification.limitToLast(1);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    latestNotificationCondition = "" + ds.child("title").getValue();
                    loadDataGps(latestNotificationCondition);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    @SuppressLint("SimpleDateFormat")
    private void saveLatitudeLongitudeUserHistory() {

        // ambil data dari database, nama nya data_gps
        DatabaseReference lastLocation = FirebaseDatabase.getInstance().getReference("data_gps");
        lastLocation.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // simpen latitud sama longitudnya kedalem variabel
                // parse double itu maskudnya adalah, konversi dari nilai string ke bilangan double
                // jadi data lat & long itu awalnya bentukan string,
                // maka dari itu perlu konversi ke double supaya lat & longitude bisa di proses
                lastLatitude = Double.parseDouble("" + snapshot.child("lattitude").getValue());
                lastLongitude = Double.parseDouble("" + snapshot.child("longitude").getValue());
                String realStatus = "" + snapshot.child("stat").getValue();

                //potong status untuk dapetin status yang asli (0 atau 1)
                // substr = fungsi untuk motong string
                String getStatus = realStatus.substring(0, 1);
                String rawTimeStamp = realStatus.substring(5);

                // saya mau ngubah dari yg formatnya 29.05.2021 menjadi 29 05 2021
                // titik saya jadiin spasi
                // _ saya jadiin ,
                rawTimeStamp = rawTimeStamp.replaceAll("\\.", " ");
                rawTimeStamp = rawTimeStamp.replaceAll("_", ", ");

                // awalnya 29 05 2021, 19:00:00
                // untuk ngubah misal 05 menjadi MEI
                Calendar cal = Calendar.getInstance();
                @SuppressLint("SimpleDateFormat")
                SimpleDateFormat month_date = new SimpleDateFormat("MMM");
                cal.set(Calendar.MONTH, Integer.parseInt(rawTimeStamp.substring(3, 5)) - 1);

                // awalnya misal:

                // fixed time stamp, ex : 10 MAY 2021, 12:00:00
                lastTimeStamp = rawTimeStamp.substring(0, 2) + " " + month_date.format(cal.getTime()) + " " + rawTimeStamp.substring(6);

                // set posisi awal dan posisi akhir kedalam database untuk history pengguna
                setDataHistory();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        /// Mengambil data history terakhir
        dateFormatForHistoryUser = new SimpleDateFormat("dd MMM yyyy");
        timeStampForHistoryUser = dateFormatForHistoryUser.format(new Date());

        DatabaseReference getLasHistory = FirebaseDatabase.getInstance().getReference("History").child(user.getUid()).child(timeStampForHistoryUser);
        Query query = getLasHistory.limitToLast(1);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot ds : snapshot.getChildren()) {
                    historyLastLatitude = Double.parseDouble("" + ds.child("lastLatitude").getValue());
                    historyLastLongitude = Double.parseDouble("" + ds.child("lastLongitude").getValue());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    @SuppressLint("SimpleDateFormat")
    private void setDataHistory() {

        // untik nampilin tanggal sekarang
        dateFormatForHistoryUser = new SimpleDateFormat("dd MMM yyyy");
        timeStampForHistoryUser = dateFormatForHistoryUser.format(new Date());


        //Cek dulu ada berapa data dalam log, jika > 60, hapus indeks ke-0 / data pertama
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("History").child(user.getUid()).child(timeStampForHistoryUser);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long itemCount = snapshot.getChildrenCount();
                Log.d("ITEM COUNT: ", String.valueOf(itemCount));

                /// batas log history 60
                if (itemCount >= 60) {
                    if (itemCount == 60) {
                        query = reference.limitToFirst(1);
                    } else {
                        ///  Jika data yang masuk lebih dari > 60
                        query = reference.limitToLast((int) (itemCount - 59));
                    }
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            // delete data pertama
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                ds.getRef().removeValue();
                            }

                            // insert new data to db
                            insertLogHistoryUser();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                } else {
                    //insert data to db
                    insertLogHistoryUser();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    @SuppressLint("SimpleDateFormat")
    private void insertLogHistoryUser() {

        Log.d(TAG, "History Last Latitude: " + historyLastLatitude + " Last Latitude: " + lastLatitude);

        // pengecekan data history jika data seblumnya sama dengan history sekarang, maka gak usah di tulis database, klo beda baru ditulis
        if (historyLastLatitude != lastLatitude || historyLastLongitude != lastLongitude) {

            // membuat date time, yang kemudian akan disimpan kedalam database bersama dengan latitude dan longitude
            dateFormat = new SimpleDateFormat("HH:mm:ss");
            timeStamp = dateFormat.format(new Date());

            // kalo data sebelum sama sekarang beda, maka kita insert data history
            // HashMap adalah class implementasi dar Map, Map itu sendiri adalah interface yang memiliki fungsi untuk memetakan nilai dengan key unik.
            // hash map itu untuk nyimpen data berbasis key dan value
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("History");
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("lastLatitude", lastLatitude);
            hashMap.put("lastLongitude", lastLongitude);
            hashMap.put("lastDateTime", lastTimeStamp);
            ref.child(user.getUid()).child(timeStampForHistoryUser).child(timeStamp).setValue(hashMap);

            Log.d("CURRENT LOCATION : ", lastLatitude + " " + lastLongitude + " " + lastTimeStamp);
        } else {
            Log.d("TAG : ", "Data history tidak ditambah");
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        // ini fungsi untuk nge looping, 1 detik sekali
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    // ini untuk nampilin marker lokasi yg berisi lat, long, date time
    LocationCallback locationCallback = new LocationCallback() {
        @SuppressLint("SimpleDateFormat")
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);


            // get data ke data_gps
            DatabaseReference getUserGPS = FirebaseDatabase.getInstance().getReference("data_gps");
            getUserGPS.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // kita ambil lat, long, date time mentahan
                    realLatitude = "" + snapshot.child("lattitude").getValue();
                    realLongitude = "" + snapshot.child("longitude").getValue();
                    String realStatus = "" + snapshot.child("stat").getValue();

                    //potong status untuk dapetin status yang asli (0 atau 1)
                    String getStatus = realStatus.substring(0, 1);
                    String rawTimeStamp = realStatus.substring(5);

                    rawTimeStamp = rawTimeStamp.replaceAll("\\.", " ");
                    rawTimeStamp = rawTimeStamp.replaceAll("_", ", ");

                    Calendar cal = Calendar.getInstance();
                    @SuppressLint("SimpleDateFormat")
                    SimpleDateFormat month_date = new SimpleDateFormat("MMM");
                    cal.set(Calendar.MONTH, Integer.parseInt(rawTimeStamp.substring(3, 5)) - 1);

                    // fixed time stamp, ex : 10 MAY 2021, 12:00:00
                    realTimeStamp = rawTimeStamp.substring(0, 2) + " " + month_date.format(cal.getTime()) + " " + rawTimeStamp.substring(6);

                    // jika switch on, maka marker akan terupdate
                    // kalo status nya 1, maka pengguna bisa ngupdate marker
                    if (getStatus.equals("1") || LATEST_PLACE_TONGKAT_USER > 0) {
                        // untuk nampilin marker
                        setUserLocationMarker(realLatitude, realLongitude, realTimeStamp);
                        LATEST_PLACE_TONGKAT_USER--;
                    } else {
                        Log.d("MARKER OFF: ", "Tidak mengupdate marker karena mikrokontroller mati");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    };

    @SuppressLint("SimpleDateFormat")
    private void setUserLocationMarker(String realLatitude, String realLongitude, String realTimeStamp) {
        final LatLng latLng = new LatLng(Double.parseDouble(realLatitude), Double.parseDouble(realLongitude));

        // data dari data_gps, kita update ke Location
        DatabaseReference currentLocationRef = FirebaseDatabase.getInstance().getReference("Admins").child(user.getUid()).child("Location");
        HashMap<String, Object> result = new HashMap<>();
        result.put("latitude", realLatitude);
        result.put("longitude", realLongitude);
        result.put("dateTime", realTimeStamp);
        currentLocationRef.updateChildren(result);

        // tampilkan marker
        // locationMarker == null, itu maksudnya ketika pertama kali ke halaman location
        if (userLocationMarker == null) {
            // create new marker
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            markerOptions.anchor((float) 0.5, (float) 0.5);
            markerOptions.title("Current Location");
            markerOptions.snippet("Latitude: " + realLatitude + "\nLongitude: " + realLongitude + "\n" + realTimeStamp);
            userLocationMarker = mMap.addMarker(markerOptions);

            // untuk nge zoom google maps ke lat long tujuan,
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));


            // untuk nampilin dateTime
            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    LinearLayout info = new LinearLayout(getContext());
                    info.setOrientation(LinearLayout.VERTICAL);

                    TextView title = new TextView(getContext());
                    title.setTextColor(Color.BLACK);
                    title.setGravity(Gravity.LEFT);
                    title.setTypeface(null, Typeface.BOLD);
                    title.setText(marker.getTitle());

                    TextView snippet = new TextView(getContext());
                    snippet.setTextColor(Color.GRAY);
                    snippet.setText(marker.getSnippet());

                    info.addView(title);
                    info.addView(snippet);

                    return info;
                }
            });

        } else {
            // ketika halaman sudah repeat
            userLocationMarker.setPosition(latLng);
            userLocationMarker.setTitle("Current Location");
            userLocationMarker.setSnippet("Latitude: " + realLatitude + "\nLongitude: " + realLongitude + "\n" + realTimeStamp);
            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    LinearLayout info = new LinearLayout(getContext());
                    info.setOrientation(LinearLayout.VERTICAL);

                    TextView title = new TextView(getContext());
                    title.setTextColor(Color.BLACK);
                    title.setGravity(Gravity.LEFT);
                    title.setTypeface(null, Typeface.BOLD);
                    title.setText(marker.getTitle());

                    TextView snippet = new TextView(getContext());
                    snippet.setTextColor(Color.GRAY);
                    snippet.setText(marker.getSnippet());

                    info.addView(title);
                    info.addView(snippet);

                    return info;
                }
            });
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
        }
        Log.d(TAG, "Latitude: " + realLatitude + " Longitude: " + realLongitude + " Timestamp: " + realTimeStamp);
       // mMap.clear();
    }

    @SuppressLint("SimpleDateFormat")
    // save keterangan on / off, dateTime, dan deskripsi ke database notification
    private void saveNotification(String title, String description, String timeStamp) {
        //Cek dulu ada berapa data dalam log, jika >10, hapus indeks ke-0 / data pertama
        DatabaseReference getNotification = FirebaseDatabase.getInstance().getReference("Notification").child(user.getUid());
        getNotification.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long itemCount = snapshot.getChildrenCount();
                if (itemCount >= 10) {
                    if (itemCount == 10) {
                        query = getNotification.limitToFirst(1);
                    } else {
                        query = getNotification.limitToLast((int) (itemCount - 9));
                    }
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            // delete data pertama
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                ds.getRef().removeValue();
                            }
                            // insert new data to db
                            insertNotification(title, description, timeStamp);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                } else {
                    insertNotification(title, description, timeStamp);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @SuppressLint("SimpleDateFormat")
    private void insertNotification(String title, String description, String timeStamp) {

        long id = System.currentTimeMillis();

        // simpan notifikasi
        DatabaseReference setNotification = FirebaseDatabase.getInstance().getReference("Notification");
        HashMap<String, Object> saveStartLocation = new HashMap<>();
        saveStartLocation.put("title", title);
        saveStartLocation.put("description", description);
        saveStartLocation.put("dateTime", timeStamp);
        setNotification.child(user.getUid()).child(id + " " + timeStamp).setValue(saveStartLocation);
    }

    // ini untuk nampilin notifikasi dari smartphone kita
    private void pushNotificationGPS(String title, String description) {

        // untuk menghidupkan bunyi ringtone
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // ini fungsinya untuk membuat notifikasi supaya tampil
        NotificationManager mNotificationManager = (NotificationManager) getActivity()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getActivity(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_circle_notifications_24)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_baseline_circle_notifications_24))
                .setContentTitle(title)
                .setContentText(description)
                .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000}) // getaaran +- bertahan hingga 5 detik
                .setSound(alarmSound)
                .setAutoCancel(true);

        // jika OS android Oreo keatas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_NAME.toString());

            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{1000, 1000, 1000, 1000, 1000});

            mBuilder.setChannelId(CHANNEL_ID);
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(channel);
            }
        }

        // untuk membangun notifikasi / build notifikasi
        Notification notification = mBuilder.build();
        if (mNotificationManager != null) {
            mNotificationManager.notify(NOTIFICATION_ID, notification);
        }
    }


    // ini method untuk nampilin google maps menggunakan api key yang udah kita dapetin
    private void fetchLastLocation() {

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION}, 44);
            return;
        }

        // manggil map ggogle maps menggunakan API disini
        SupportMapFragment supportMapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.google_maps);
        supportMapFragment.getMapAsync(LocationFragment.this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 44) {
            if (grantResults.length > 0 && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {
                fetchLastLocation();
            }
        }
    }

}