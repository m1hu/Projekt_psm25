package com.example.aplikacja_moodtracker;


import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;



import com.google.android.material.navigation.NavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class StatsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private EditText startDateInput, endDateInput;
    private Button startDatePickerButton, endDatePickerButton, generateStatsButton;
    private BarChart moodChart;
    private TextView percentageSummary;
    private DatabaseHelper dbHelper;
    private DrawerLayout drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        // Inicjalizacja Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Inicjalizacja DrawerLayout i NavigationView
        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_stats);

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
        generateStatsButton = findViewById(R.id.generateStatsButton);
        moodChart = findViewById(R.id.moodChart);
        percentageSummary = findViewById(R.id.percentageSummary);

        // Inicjalizacja bazy danych
        dbHelper = new DatabaseHelper(this);

        // Wybór dat
        startDatePickerButton.setOnClickListener(v -> showDatePicker(startDateInput));
        endDatePickerButton.setOnClickListener(v -> showDatePicker(endDateInput));

        // Generowanie statystyk
        generateStatsButton.setOnClickListener(v -> generateStatistics());
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

    private void generateStatistics() {
        String startDateStr = startDateInput.getText().toString();
        String endDateStr = endDateInput.getText().toString();

        if (startDateStr.isEmpty() || endDateStr.isEmpty()) {
            Toast.makeText(this, "Wybierz obie daty!", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date startDate = sdf.parse(startDateStr);
            Date endDate = sdf.parse(endDateStr);

            if (startDate.after(endDate)) {
                // Zamień daty, jeśli startDate jest późniejsza niż endDate
                String temp = startDateStr;
                startDateStr = endDateStr;
                endDateStr = temp;
                startDateInput.setText(startDateStr);
                endDateInput.setText(endDateStr);
                startDate = sdf.parse(startDateStr);
                endDate = sdf.parse(endDateStr);
            }

            // Pobierz dane z bazy danych
            Map<String, String> moodData = new HashMap<>();
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT date, main_mood FROM mood_entries WHERE date BETWEEN ? AND ? ORDER BY date",
                    new String[]{startDateStr, endDateStr});
            while (cursor.moveToNext()) {
                String date = cursor.getString(0);
                String mainMood = cursor.getString(1);
                moodData.put(date, mainMood);
            }
            cursor.close();

            // Przygotuj dane do wykresu
            List<BarEntry> entries = new ArrayList<>();
            List<Integer> colors = new ArrayList<>(); // Lista kolorów dla każdego słupka
            List<String> labels = new ArrayList<>();
            long diffInMillies = Math.abs(endDate.getTime() - startDate.getTime());
            long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS) + 1;

            int happyCount = 0, neutralCount = 0, sadCount = 0;
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);

            for (int i = 0; i < diffInDays; i++) {
                String currentDate = sdf.format(calendar.getTime());
                String mood = moodData.get(currentDate);
                float value;

                if (mood == null) {
                    value = 0f; // Brak nastroju
                    colors.add(Color.TRANSPARENT); // Brak słupka (przezroczysty)
                } else {
                    switch (mood) {
                        case "Szczęśliwy":
                            value = 3f; // Wysoki słupek
                            colors.add(Color.GREEN); // Zielony dla Szczęśliwego
                            happyCount++;
                            break;
                        case "Neutralny":
                            value = 2f; // Średni słupek
                            colors.add(Color.YELLOW); // Żółty dla Neutralnego
                            neutralCount++;
                            break;
                        case "Smutny":
                            value = 1f; // Niski słupek
                            colors.add(Color.RED); // Czerwony dla Smutnego
                            sadCount++;
                            break;
                        default:
                            value = 0f; // Brak nastroju
                            colors.add(Color.TRANSPARENT);
                    }
                }

                entries.add(new BarEntry(i, value));
                SimpleDateFormat displaySdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                labels.add(displaySdf.format(calendar.getTime()));
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }

            // Ustawienie wykresu
            BarDataSet dataSet = new BarDataSet(entries, "Nastrój");
            dataSet.setColors(colors); // Ustaw indywidualne kolory dla każdego słupka
            dataSet.setValueTextSize(0f); // Ukryj wartości na słupkach

            BarData barData = new BarData(dataSet);
            barData.setBarWidth(0.5f);

            moodChart.setData(barData);
            moodChart.setFitBars(true);
            moodChart.getDescription().setEnabled(false);
            moodChart.getLegend().setEnabled(false);

            // Ustawienie osi X
            XAxis xAxis = moodChart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setLabelRotationAngle(45f);
            xAxis.setLabelCount(labels.size());

            // Ustawienie osi Y
            moodChart.getAxisLeft().setAxisMinimum(0f);
            moodChart.getAxisLeft().setAxisMaximum(3f);
            moodChart.getAxisLeft().setLabelCount(4, true); // 4 etykiety: 0, 1, 2, 3
            moodChart.getAxisLeft().setValueFormatter(new IndexAxisValueFormatter(new String[]{"Brak", "Smutny", "Neutralny", "Szczęśliwy"}));
            moodChart.getAxisLeft().setGranularity(1f); // Odstęp między etykietami
            moodChart.getAxisLeft().setDrawGridLines(true); // Włącz siatkę dla lepszej czytelności
            moodChart.getAxisLeft().setGridColor(Color.LTGRAY); // Kolor siatki
            moodChart.getAxisRight().setEnabled(false);

            moodChart.invalidate(); // Odśwież wykres

            // Oblicz procenty
            int totalDays = (int) diffInDays;
            int moodDays = happyCount + neutralCount + sadCount;
            if (moodDays == 0) {
                percentageSummary.setText("Zestawienie procentowe:\nSzczęśliwy: 0%\nNeutralny: 0%\nSmutny: 0%");
            } else {
                int happyPercent = (happyCount * 100) / moodDays;
                int neutralPercent = (neutralCount * 100) / moodDays;
                int sadPercent = (sadCount * 100) / moodDays;
                percentageSummary.setText(String.format("Zestawienie procentowe:\nSzczęśliwy: %d%%\nNeutralny: %d%%\nSmutny: %d%%",
                        happyPercent, neutralPercent, sadPercent));
            }

        } catch (Exception e) {
            Toast.makeText(this, "Błąd podczas generowania statystyk: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_main) {
            Intent intent = new Intent(StatsActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else if (id == R.id.nav_history) {
            Intent intent = new Intent(StatsActivity.this, HistoryActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else if (id == R.id.nav_stats) {
            // Jesteśmy już na StatsActivity
        } else if (id == R.id.nav_options) {
            Intent intent = new Intent(StatsActivity.this, SettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
