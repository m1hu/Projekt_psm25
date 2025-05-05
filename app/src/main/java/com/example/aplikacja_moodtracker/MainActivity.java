package com.example.aplikacja_moodtracker;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private EditText dateInput, additionalMoodInput, noteInput;
    private Button datePickerButton, happyButton, neutralButton, sadButton, addMoodButton, submitButton;
    private ListView additionalMoodsList;
    private ArrayList<String> additionalMoods;
    private ArrayAdapter<String> adapter;
    private String selectedMainMood = "";
    private DatabaseHelper dbHelper;
    private DrawerLayout drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicjalizacja Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Inicjalizacja DrawerLayout i NavigationView
        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Ustawienie ActionBarDrawerToggle (przycisk hamburgera)
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Inicjalizacja widoków
        dateInput = findViewById(R.id.dateInput);
        datePickerButton = findViewById(R.id.datePickerButton);
        happyButton = findViewById(R.id.happyButton);
        neutralButton = findViewById(R.id.neutralButton);
        sadButton = findViewById(R.id.sadButton);
        additionalMoodInput = findViewById(R.id.additionalMoodInput);
        addMoodButton = findViewById(R.id.addMoodButton);
        additionalMoodsList = findViewById(R.id.additionalMoodsList);
        noteInput = findViewById(R.id.noteInput);
        submitButton = findViewById(R.id.submitButton);

        // Inicjalizacja listy dodatkowych nastrojów
        additionalMoods = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, additionalMoods);
        additionalMoodsList.setAdapter(adapter);

        // Inicjalizacja bazy danych
        dbHelper = new DatabaseHelper(this);
        Log.d("Database", "Database initialized");

        // Wybór daty
        datePickerButton.setOnClickListener(v -> showDatePicker());

        // Wybór głównego nastroju
        happyButton.setOnClickListener(v -> selectedMainMood = "Szczęśliwy");
        neutralButton.setOnClickListener(v -> selectedMainMood = "Neutralny");
        sadButton.setOnClickListener(v -> selectedMainMood = "Smutny");

        // Dodawanie dodatkowego nastroju
        addMoodButton.setOnClickListener(v -> {
            String mood = additionalMoodInput.getText().toString().trim();
            if (!mood.isEmpty()) {
                additionalMoods.add(mood);
                adapter.notifyDataSetChanged();
                additionalMoodInput.setText("");
            }
        });

        // Usuwanie dodatkowego nastroju
        additionalMoodsList.setOnItemClickListener((parent, view, position, id) -> {
            additionalMoods.remove(position);
            adapter.notifyDataSetChanged();
        });

        // Zatwierdzenie wpisu
        submitButton.setOnClickListener(v -> saveMoodEntry());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            dateInput.setText(sdf.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }
    private void saveMoodEntry() {
        String date = dateInput.getText().toString();
        String note = noteInput.getText().toString();
        String additionalMoodsString = TextUtils.join(",", additionalMoods);

        if (date.isEmpty() || selectedMainMood.isEmpty()) {
            Toast.makeText(this, "Wybierz datę i główny nastrój!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sprawdź, czy wpis dla tej daty już istnieje
        if (checkIfEntryExists(date)) {
            Toast.makeText(this, "Wpis dla tej daty już istnieje! Możesz dodać tylko jeden nastrój na dzień.", Toast.LENGTH_LONG).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("INSERT INTO mood_entries (date, main_mood, additional_moods, note) VALUES (?, ?, ?, ?)",
                new Object[]{date, selectedMainMood, additionalMoodsString, note});
        Toast.makeText(this, "Wpis zapisany!", Toast.LENGTH_SHORT).show();

        // Resetowanie formularza
        dateInput.setText("");
        selectedMainMood = "";
        additionalMoods.clear();
        adapter.notifyDataSetChanged();
        noteInput.setText("");
    }
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_main) {
            // Jesteśmy już na MainActivity
        } else if (id == R.id.nav_history) {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        else if (id == R.id.nav_stats) {
            Intent intent = new Intent(MainActivity.this, StatsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    private boolean checkIfEntryExists(String date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM mood_entries WHERE date = ?", new String[]{date});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }
}