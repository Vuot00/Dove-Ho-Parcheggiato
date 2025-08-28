package com.progetto.Mappa

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat


class NotificaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "parking_channel"
        val channelName = context.getString(R.string.parcheggio)

        // Crea il canale (solo da Android O in su)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 👉 Intent per aprire la MainActivity quando l'utente clicca la notifica
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Costruzione della notifica
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.parcheggio_in_scadenza))
            .setContentText(context.getString(R.string.notifica_funzionante))
            .setSmallIcon(android.R.drawable.ic_dialog_info) // icona di sistema
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // chiude la notifica al click
            .setContentIntent(pendingIntent) // 👉 collega la notifica all'app
            .build()

        notificationManager.notify(1, notification)
    }
}

