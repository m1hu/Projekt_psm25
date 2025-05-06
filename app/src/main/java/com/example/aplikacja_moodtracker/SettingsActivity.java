package com.example.aplikacja_moodtracker;



import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawer;
    private Switch notificationsSwitch;
    private TextView timePickerLabel, selectedTimeText;
    private Button timePickerButton;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "MoodTrackerPrefs";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_NOTIFICATION_HOUR = "notification_hour";
    private static final String KEY_NOTIFICATION_MINUTE = "notification_minute";

    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

// Poproś o uprawnienie do powiadomień (dla Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
                Log.d("MoodTracker", "Prośba o uprawnienie POST_NOTIFICATIONS");
            } else {
                Log.d("MoodTracker", "Uprawnienie POST_NOTIFICATIONS już przyznane");
            }
        }



        // Inicjalizacja Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Inicjalizacja DrawerLayout i NavigationView
        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_options);

        // Ustawienie ActionBarDrawerToggle (przycisk hamburgera)
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Inicjalizacja widoków
        notificationsSwitch = findViewById(R.id.notificationsSwitch);
        timePickerLabel = findViewById(R.id.timePickerLabel);
        timePickerButton = findViewById(R.id.timePickerButton);
        selectedTimeText = findViewById(R.id.selectedTimeText);

        // Inicjalizacja SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Wczytaj zapisane ustawienia
        boolean notificationsEnabled = sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, false);
        int hour = sharedPreferences.getInt(KEY_NOTIFICATION_HOUR, 18); // Domyślnie 18:00
        int minute = sharedPreferences.getInt(KEY_NOTIFICATION_MINUTE, 0);

        // Ustaw początkowe wartości
        notificationsSwitch.setChecked(notificationsEnabled);
        updateTimePickerVisibility(notificationsEnabled);
        updateSelectedTimeText(hour, minute);

        // Listener dla przełącznika powiadomień
        notificationsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(KEY_NOTIFICATIONS_ENABLED, isChecked);
                editor.apply();

                updateTimePickerVisibility(isChecked);

                if (isChecked) {
                    // Włącz powiadomienia
                    int hour = sharedPreferences.getInt(KEY_NOTIFICATION_HOUR, 18);
                    int minute = sharedPreferences.getInt(KEY_NOTIFICATION_MINUTE, 0);
                    scheduleDailyNotification(hour, minute);
                } else {
                    // Wyłącz powiadomienia
                    cancelDailyNotification();
                }
            }
        });

        // Listener dla przycisku wyboru godziny
        timePickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hour = sharedPreferences.getInt(KEY_NOTIFICATION_HOUR, 18);
                int minute = sharedPreferences.getInt(KEY_NOTIFICATION_MINUTE, 0);

                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        SettingsActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putInt(KEY_NOTIFICATION_HOUR, hourOfDay);
                                editor.putInt(KEY_NOTIFICATION_MINUTE, minute);
                                editor.apply();

                                updateSelectedTimeText(hourOfDay, minute);

                                if (notificationsSwitch.isChecked()) {
                                    scheduleDailyNotification(hourOfDay, minute);
                                }
                            }
                        },
                        hour,
                        minute,
                        true // 24-godzinny format
                );
                timePickerDialog.show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Uprawnienie do powiadomień przyznane", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Uprawnienie do powiadomień odrzucone. Powiadomienia nie będą działać.", Toast.LENGTH_LONG).show();
                notificationsSwitch.setChecked(false); // Wyłącz przełącznik, jeśli uprawnienie nie zostało przyznane
            }
        }
    }

    private void updateTimePickerVisibility(boolean isEnabled) {
        timePickerLabel.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
        timePickerButton.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
        selectedTimeText.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
    }

    private void updateSelectedTimeText(int hour, int minute) {
        selectedTimeText.setText(String.format("Wybrana godzina: %02d:%02d", hour, minute));
    }

    private void scheduleDailyNotification(int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // Logowanie czasu powiadomienia
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Log.d("MoodTracker", "Planowane powiadomienie na: " + sdf.format(calendar.getTime()));
        Log.d("MoodTracker", "Aktualny czas: " + sdf.format(new Date()));

        // Jeśli wybrana godzina już minęła, ustaw na następny dzień
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Log.d("MoodTracker", "Godzina minęła, ustawiono na następny dzień: " + sdf.format(calendar.getTime()));
        }

        // Ustaw codzienne powiadomienie
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );

        Toast.makeText(this, "Powiadomienia zaplanowane na " + String.format("%02d:%02d", hour, minute), Toast.LENGTH_SHORT).show();
    }
    private void cancelDailyNotification() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
        Toast.makeText(this, "Powiadomienia wyłączone", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_main) {
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else if (id == R.id.nav_history) {
            Intent intent = new Intent(SettingsActivity.this, HistoryActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else if (id == R.id.nav_stats) {
            Intent intent = new Intent(SettingsActivity.this, StatsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else if (id == R.id.nav_options) {
            // Jesteśmy już na SettingsActivity
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
