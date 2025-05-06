package com.example.aplikacja_moodtracker;



import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "MoodTrackerChannel";
    private static final String CHANNEL_NAME = "Mood Tracker Reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("MoodTracker", "NotificationReceiver wywołany");

        // Utwórz kanał powiadomień (dla Android 8.0 i nowszych)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
            Log.d("MoodTracker", "Kanał powiadomień utworzony");
        }

        // Utwórz intencję, która otworzy MainActivity po kliknięciu powiadomienia
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Utwórz powiadomienie
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Przypomnienie Mood Tracker")
                .setContentText("Czas zarejestrować swój nastrój!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Wyświetl powiadomienie
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
        Log.d("MoodTracker", "Powiadomienie wyświetlone");
    }
}