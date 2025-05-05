package com.example.aplikacja_moodtracker;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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

public class HistoryActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private EditText startDateInput, endDateInput;
    private Button startDatePickerButton, endDatePickerButton, checkMoodButton;
    private ListView historyList;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> historyEntries;
    private DatabaseHelper dbHelper;
    private DrawerLayout drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

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
        startDateInput = findViewById(R.id.startDateInput);
        endDateInput = findViewById(R.id.endDateInput);
        startDatePickerButton = findViewById(R.id.startDatePickerButton);
        endDatePickerButton = findViewById(R.id.endDatePickerButton);
        checkMoodButton = findViewById(R.id.checkMoodButton);
        historyList = findViewById(R.id.historyList);

        // Inicjalizacja listy historii
        historyEntries = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, historyEntries);
        historyList.setAdapter(adapter);

        // Inicjalizacja bazy danych
        dbHelper = new DatabaseHelper(this);

        // Wybór dat
        startDatePickerButton.setOnClickListener(v -> showDatePicker(startDateInput));
        endDatePickerButton.setOnClickListener(v -> showDatePicker(endDateInput));

        // Sprawdzanie historii
        checkMoodButton.setOnClickListener(v -> loadHistory());
    }

    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            target.setText(sdf.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void loadHistory() {
        String startDate = startDateInput.getText().toString();
        String endDate = endDateInput.getText().toString();

        if (startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Wybierz obie daty!", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM mood_entries WHERE date BETWEEN ? AND ? ORDER BY date",
                new String[]{startDate, endDate});

        historyEntries.clear();
        while (cursor.moveToNext()) {
            // Konwersja daty z yyyy-MM-dd na dd/MM/yyyy do wyświetlenia
            String storedDate = cursor.getString(1); // Data w formacie yyyy-MM-dd
            String displayDate;
            try {
                SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat sdfOutput = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                displayDate = sdfOutput.format(sdfInput.parse(storedDate));
            } catch (Exception e) {
                displayDate = storedDate; // W razie błędu wyświetl surową datę
            }

            String entry = "Data: " + displayDate +
                    "\nGłówny nastrój: " + cursor.getString(2) +
                    "\nDodatkowe nastroje: " + cursor.getString(3) +
                    "\nNotatka: " + cursor.getString(4);
            historyEntries.add(entry);
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_main) {
            Intent intent = new Intent(HistoryActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else if (id == R.id.nav_history) {
            // Jesteśmy już na HistoryActivity
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}