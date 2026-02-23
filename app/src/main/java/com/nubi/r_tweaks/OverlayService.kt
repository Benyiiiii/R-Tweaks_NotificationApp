package com.nubi.r_tweaks

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
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
            // Leemos la configuración actual guardada en la app
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
                        // La temperatura viene en décimas (ej: 400 es 40°C)
                        val temperatura = (intent.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0)) / 10
                        if (temperatura >= 45) {
                            actualizarUI("Alerta de Calor", "Consola a $temperatura°C - ¡Ojo!", "#FF5722", R.drawable.ic_temperatura)
                        }
                    }
                }
                "com.spotify.music.metadatachanged", "com.android.music.metadatachanged" -> {
                    if (avisarMusica) {
                        val track = intent.getStringExtra("track") ?: "Canción"
                        val artist = intent.getStringExtra("artist") ?: "Artista"
                        actualizarUI(track, artist, "#1DB954", R.drawable.ic_musica)
                    }
                }
            }
        }
    }

    // Esta es la función mágica que hace el cambio visual
    private fun actualizarUI(titulo: String, subtitulo: String, colorHex: String, iconoRes: Int) {
        tituloNotificacion.text = titulo
        textoNotificacion.text = subtitulo
        textoNotificacion.setTextColor(Color.parseColor(colorHex))

        // ¡Aquí le cambiamos el dibujo al ícono!
        iconoNotificacion.setImageResource(iconoRes)
        iconoNotificacion.setColorFilter(Color.parseColor(colorHex))

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
            WindowManager.LayoutParams.WRAP_CONTENT, // ¡Aquí está el cambio clave!
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL

        // ESTADO INICIAL (El modo "Gota escondida")
        // Achicamos la vista al 20% de su tamaño, la subimos un poco y la hacemos invisible
        overlayView.alpha = 0f
        overlayView.translationY = -150f
        overlayView.scaleX = 0.2f
        overlayView.scaleY = 0.2f

        windowManager.addView(overlayView, params)

        // Define el filtro con las acciones de energía y Spotify
        val filtroMúsica = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction("com.spotify.music.metadatachanged")
        }

        // Esta es la forma segura de registrarlo para que tu Motorola no lo mate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receptorEnergia, filtroMúsica, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(receptorEnergia, filtroMúsica)
        }
    }

    // --- LA MAGIA DE LA DYNAMIC ISLAND ---

    private fun mostrarCapsulaAnimada() {
        handler.removeCallbacks(esconderRunnable)

        // El detector: preguntamos al sistema si la pantalla está acostada (horizontal)
        val esHorizontal = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        // Si está en horizontal, la frenamos 60 píxeles más abajo. Si está vertical, frena en 0.
        val posicionDescanso = if (esHorizontal) 20f else 0f

        // Se infla como un globo mientras cae
        overlayView.animate()
            .translationY(posicionDescanso) // ¡Aquí aplicamos el empujoncito!
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(500)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        handler.postDelayed(esconderRunnable, 3000)
    }

    private fun esconderCapsula() {
        // Se chupa como una pasa, toma vuelito hacia abajo (Anticipate) y sale disparada p'arriba
        overlayView.animate()
            .translationY(-150f)
            .scaleX(0.2f) // Vuelve a ser una bolita enana
            .scaleY(0.2f)
            .alpha(0f)
            .setDuration(400)
            .setInterpolator(AnticipateInterpolator()) // Este efecto le da un "vuelito" antes de subir
            .start()
    }

    // -----------------------------------

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