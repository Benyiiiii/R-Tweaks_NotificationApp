package com.nubi.r_tweaks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Si el sistema avisa que ya prendió por completo...
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {

            // Revisamos piolita si tenemos el permiso para flotar
            val tienePermisoFlotar = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }

            // Si está todo en orden, ¡prendemos el motor!
            if (tienePermisoFlotar) {
                val serviceIntent = Intent(context, OverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}