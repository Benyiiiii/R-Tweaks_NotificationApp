package com.nubi.r_tweaks

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View

    private lateinit var textoNotificacion: TextView
    private lateinit var tituloNotificacion: TextView
    private lateinit var iconoNotificacion: ImageView

    private val handler = Handler(Looper.getMainLooper())
    private val esconderRunnable = Runnable { esconderCapsula() }

    private val receptorEnergia = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val sharedPrefs = context?.getSharedPreferences("RTweaksPrefs", Context.MODE_PRIVATE)
            val avisarBateria = sharedPrefs?.getBoolean("aviso_bateria", true) ?: true
            val avisarMusica = sharedPrefs?.getBoolean("aviso_musica", true) ?: true

            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    if (avisarBateria) {
                        actualizarUI("Batería Cargando", "Energía conectada", "#4CAF50", R.drawable.ic_bateria_carga)
                    }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    if (avisarBateria) {
                        actualizarUI("Modo Batería", "Usando energía portátil", "#FF9800", R.drawable.ic_bateria_baja)
                    }
                }
                Intent.ACTION_BATTERY_LOW -> {
                    if (avisarBateria) {
                        actualizarUI("Batería Crítica", "Conecta el cargador (15%)", "#F44336", R.drawable.ic_bateria_casi_vacia)
                    }
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    if (avisarBateria) {
                        val temperatura = (intent.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0)) / 10
                        if (temperatura >= 45) {
                            actualizarUI("Alerta de Calor", "Consola a $temperatura°C - ¡Ojo!", "#FF5722", R.drawable.ic_temperatura)
                        }
                    }
                }
                // ¡AQUÍ ESTÁ LA MAGIA NUEVA DE SPOTIFY!
                "com.spotify.music.metadatachanged", "com.android.music.metadatachanged" -> {
                    if (avisarMusica) {
                        val track = intent.getStringExtra("track") ?: "Canción"
                        val artist = intent.getStringExtra("artist") ?: "Artista"

                        var albumArt: Bitmap? = null

                        try {
                            val msm = context?.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
                            // Ojo: Esto asume que creaste el archivo MediaListenerService.kt como te mencioné antes
                            val componentName = ComponentName(context!!, MediaListenerService::class.java)
                            val controllers = msm.getActiveSessions(componentName)

                            for (controller in controllers) {
                                if (controller.packageName.contains("spotify")) {
                                    albumArt = controller.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                                    if (albumArt == null) {
                                        albumArt = controller.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                                    }
                                    break
                                }
                            }
                        } catch (e: SecurityException) {
                            // Si falta el permiso en el celular, pasa de largo y no se cae
                        }

                        actualizarUI(track, artist, "#1DB954", R.drawable.ic_musica, albumArt)
                    }
                }
            }
        }
    }

    private fun actualizarUI(titulo: String, texto: String, colorHex: String, iconoRes: Int, bitmap: Bitmap? = null) {
        val tvTitulo = overlayView.findViewById<TextView>(R.id.titulo_notificacion)
        val tvTexto = overlayView.findViewById<TextView>(R.id.texto_notificacion)
        tvTitulo.text = titulo
        tvTexto.text = texto

        // ¡AQUÍ LE DEVOLVEMOS EL COLOR AL TEXTO!
        tvTexto.setTextColor(android.graphics.Color.parseColor(colorHex))

        val imgAlbum = overlayView.findViewById<ImageView>(R.id.img_album)
        val iconoNormal = overlayView.findViewById<ImageView>(R.id.icono_notificacion)

        if (bitmap != null) {
            imgAlbum.setImageBitmap(bitmap)
            imgAlbum.visibility = View.VISIBLE
            iconoNormal.visibility = View.GONE
        } else {
            imgAlbum.visibility = View.GONE
            iconoNormal.setImageResource(iconoRes)
            // ¡Y AQUÍ TEÑIMOS EL ÍCONO PARA QUE COMBINE AL CIEN!
            iconoNormal.setColorFilter(android.graphics.Color.parseColor(colorHex))
            iconoNormal.visibility = View.VISIBLE
        }

        mostrarCapsulaAnimada()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        crearNotificacionForeground()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)

        tituloNotificacion = overlayView.findViewById(R.id.titulo_notificacion)
        textoNotificacion = overlayView.findViewById(R.id.texto_notificacion)
        iconoNotificacion = overlayView.findViewById(R.id.icono_notificacion)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Agregamos FLAG_NOT_TOUCHABLE para que los toques pasen de largo
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL

        overlayView.alpha = 0f
        overlayView.translationY = -150f
        overlayView.scaleX = 0.2f
        overlayView.scaleY = 0.2f

        windowManager.addView(overlayView, params)

        val filtroMúsica = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction("com.spotify.music.metadatachanged")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receptorEnergia, filtroMúsica, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(receptorEnergia, filtroMúsica)
        }
    }

    private fun mostrarCapsulaAnimada() {
        handler.removeCallbacks(esconderRunnable)

        val esHorizontal = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val posicionDescanso = if (esHorizontal) 20f else 0f

        overlayView.animate()
            .translationY(posicionDescanso)
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(500)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        handler.postDelayed(esconderRunnable, 3000)
    }

    private fun esconderCapsula() {
        overlayView.animate()
            .translationY(-150f)
            .scaleX(0.2f)
            .scaleY(0.2f)
            .alpha(0f)
            .setDuration(400)
            .setInterpolator(AnticipateInterpolator())
            .start()
    }

    private fun crearNotificacionForeground() {
        val channelId = "rtweaks_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Servicio R-Tweaks", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("R-Tweaks System")
            .setContentText("Vigilando el sistema en segundo plano...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
        unregisterReceiver(receptorEnergia)
        handler.removeCallbacks(esconderRunnable)
    }
}