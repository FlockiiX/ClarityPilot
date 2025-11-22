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
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class WidgetUpdateService : Service() {
    private val API_URL = "https://clarity-pilot.com//user/1/widget/android"

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            startForegroundService()
            startUpdateLoop()
            isRunning = true
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "widget_sync"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, "Widget Sync", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("We are currently fine-tuning a new health tip for you.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private fun startUpdateLoop() {
        serviceScope.launch {
            while (true) {
                try {
                    Log.d("WidgetService", "Fetching data from $API_URL")
                    val json = fetchFromApi(API_URL)
                    Log.d("WidgetService", "Data received (Length: ${json.length})")
                    updateWidget(json)
                } catch (e: Exception) {
                    Log.e("WidgetService", "Fetch error", e)
                    updateWidget("ERROR: ${e.localizedMessage}")
                }
                delay(15_000)
            }
        }
    }

    private fun fetchFromApi(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                connection.inputStream.bufferedReader().readText()
            } else {
                throw Exception("HTTP ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun updateWidget(data: String) {
        val manager = GlanceAppWidgetManager(this)
        val widget = CanvasWidget()
        val glanceIds = manager.getGlanceIds(CanvasWidget::class.java)

        glanceIds.forEach { glanceId ->
            val prefs = getAppWidgetState(this, PreferencesGlanceStateDefinition, glanceId)
            val currentData = prefs[CANVAS_DATA_KEY]
            if(currentData == data) return@forEach

            updateAppWidgetState(this, glanceId) { prefs ->
                prefs[CANVAS_DATA_KEY] = data
            }
            widget.update(this, glanceId)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}