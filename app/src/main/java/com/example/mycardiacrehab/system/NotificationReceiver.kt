package com.example.mycardiacrehab.system

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mycardiacrehab.R

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicationName = intent.getStringExtra("MED_NAME") ?: "Medication"
        val dosage = intent.getStringExtra("DOSAGE") ?: "Dose"
        val reminderId = intent.getIntExtra("REMINDER_ID", 0)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle("Medication Reminder")
            .setContentText("It's time to take your $dosage of $medicationName.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            // FIX: Add a check for the POST_NOTIFICATIONS permission before calling notify()
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // You have permission, so you can post the notification
                notify(reminderId, builder.build())
            } else {
                // Permission has not been granted.
                // For a BroadcastReceiver, you can't request permission here.
                // You should log this or handle it silently.
                // The permission should be requested from your UI (Activity/Composable).
                println("Notification permission not granted. Cannot post notification.")
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "medication_reminder_channel"
        const val CHANNEL_NAME = "Medication Reminders"
    }
}