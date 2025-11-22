package com.claritypilot
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class WidgetUpdateService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("WidgetUpdateService", "Service started")
        if (!isRunning) {
            try {
                startForegroundService()
                startUpdateLoop()
                isRunning = true
            } catch (e: Exception) {
                Log.e("WidgetUpdateService", "Failed to start foreground", e)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "widget_updater_channel"
        val channelName = "Widget Updates"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Clarity Pilot is here for you")
            .setContentText("Keeping widget updated")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private fun startUpdateLoop() {
        serviceScope.launch {
            updateWidget("Loading...")

            while (true) {
                try {
                    Log.d("WidgetUpdateService", "Fetching data...")
                    val newData = fetchFromApi()
                    Log.d("WidgetUpdateService", "Data received: $newData")
                    updateWidget(newData)
                } catch (e: Exception) {
                    Log.e("WidgetUpdateService", "Error fetching data", e)
                    updateWidget("Err: ${e.javaClass.simpleName}")
                }
                delay(15_000)
            }
        }
    }

    private fun fetchFromApi(): String {
        val url = URL("https://clarity-pilot.com")
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun updateWidget(data: String) {
        val manager = GlanceAppWidgetManager(this)
        val widget = CanvasWidget()
        val glanceIds = manager.getGlanceIds(CanvasWidget::class.java)

        if (glanceIds.isEmpty()) {
            Log.w("WidgetUpdateService", "No widget ids found to update")
        }

        glanceIds.forEach { glanceId ->
            updateAppWidgetState(this, glanceId) { prefs ->
                prefs[CANVAS_DATA_KEY] = data
            }
            widget.update(this, glanceId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        isRunning = false
        Log.d("WidgetUpdateService", "Service destroyed")
    }
}