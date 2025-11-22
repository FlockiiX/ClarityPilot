package com.claritypilot

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class CanvasWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CanvasWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        tryStartService(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: android.appwidget.AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        tryStartService(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, WidgetUpdateService::class.java))
    }

    private fun tryStartService(context: Context) {
        val intent = Intent(context, WidgetUpdateService::class.java)
        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            Log.e("CanvasWidgetReceiver", "Could not start service from background", e)
        }
    }
}