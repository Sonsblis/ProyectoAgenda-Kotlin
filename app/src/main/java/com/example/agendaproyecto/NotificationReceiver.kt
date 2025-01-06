package com.example.agendaproyecto

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val tarea = intent.getStringExtra("tarea").orEmpty()
        val notificationId = intent.getIntExtra("notificationId", 0)
        if (tarea.isNotEmpty() && isTaskIdInPreferences(context, notificationId)) {
            showNotification(context, tarea, notificationId)
        }
    }

    private fun showNotification(context: Context, tarea: String, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear el canal de notificación (para Android O y superior)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "default_channel_id"
            val channelName = "Default Channel"
            val soundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Channel description"
                setSound(soundUri, null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Crear un intent para abrir la actividad cuando se toque la notificación
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Crear la notificación
        val notification = NotificationCompat.Builder(context, "default_channel_id")
            .setContentTitle("¡No te olvides de tu tarea pendiente!")
            .setContentText(tarea)
            .setSmallIcon(R.drawable.agendalogo)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)) // Establecer el sonido de la notificación
            .setContentIntent(pendingIntent) // Asignar el PendingIntent
            .build()

        // Mostrar la notificación
        notificationManager.notify(notificationId, notification)
    }

    private fun isTaskIdInPreferences(context: Context, id: Int): Boolean {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val taskIds = sharedPreferences.getStringSet("task_ids", mutableSetOf()) ?: mutableSetOf()
        return taskIds.contains(id.toString())
    }
}