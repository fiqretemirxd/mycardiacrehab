package com.example.mycardiacrehab.system

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.mycardiacrehab.model.MedicationReminder
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        createNotificationChannel()
    }

    fun scheduleReminder(medication: MedicationReminder) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("MED_NAME", medication.medicationName)
            putExtra("DOSAGE", medication.dosage)
            putExtra("REMINDER_ID", medication.timeOfDay.hashCode())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medication.timeOfDay.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate next dose time
        val nextDoseTime = calculateNextDoseTime(medication.timeOfDay)

        // 🟢 USE THE IMPORT: Add a human-readable log for debugging
        val minutesUntilNextDose = TimeUnit.MILLISECONDS.toMinutes(nextDoseTime - System.currentTimeMillis())
        println("Scheduling '${medication.medicationName}' reminder for ${Date(nextDoseTime)}, which is in $minutesUntilNextDose minutes.")

        // Schedule the alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextDoseTime,
                    pendingIntent
                )
            } else {
                println("Exact alarm permission denied.")
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextDoseTime,
                pendingIntent
            )
        }
    }

    // --- Helper: Calculate next time the alarm should fire ---
    private fun calculateNextDoseTime(timeString: String): Long {
        val parts = timeString.split(":") // timeString is "HH:mm"
        val targetHour = parts[0].toIntOrNull() ?: 8
        val targetMinute = parts[1].toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the calculated time is already passed today, set it for tomorrow
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Note: For recurring alarms (daily frequency), you would use setRepeating here.
        return calendar.timeInMillis
    }

    // --- Notification Channel Setup (Required since Android 8.0) ---
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                NotificationReceiver.CHANNEL_ID,
                NotificationReceiver.CHANNEL_NAME,
                importance
            ).apply {
                description = "Reminders for prescribed cardiac medications"
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}