package com.nubi.r_tweaks

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var panelPermisos: LinearLayout
    private lateinit var panelAjustes: LinearLayout
    private lateinit var btnPermisoFlotar: Button
    private lateinit var btnPermisoNotif: Button
    private lateinit var btnPermisoMusica: Button // Agregado como variable global

    private lateinit var switchBateria: Switch
    private lateinit var switchMusica: Switch
    private lateinit var switchTemperatura: Switch
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializamos la mini base de datos
        sharedPrefs = getSharedPreferences("RTweaksPrefs", Context.MODE_PRIVATE)

        panelPermisos = findViewById(R.id.panel_permisos)
        panelAjustes = findViewById(R.id.panel_ajustes)
        btnPermisoFlotar = findViewById(R.id.btn_permiso_flotar)
        btnPermisoNotif = findViewById(R.id.btn_permiso_notif)
        btnPermisoMusica = findViewById(R.id.btn_permiso_musica) // Inicializado aquí

        switchBateria = findViewById(R.id.switch_bateria)
        switchMusica = findViewById(R.id.switch_musica)
        switchTemperatura = findViewById(R.id.switch_temperatura)

        // Cargamos cómo dejaste los switches
        switchBateria.isChecked = sharedPrefs.getBoolean("aviso_bateria", true)
        switchMusica.isChecked = sharedPrefs.getBoolean("aviso_musica", true)
        switchTemperatura.isChecked = sharedPrefs.getBoolean("aviso_temp", true)

        // Listeners para guardar ajustes
        switchBateria.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("aviso_bateria", isChecked).apply()
        }
        switchMusica.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("aviso_musica", isChecked).apply()
        }
        switchTemperatura.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("aviso_temp", isChecked).apply()
        }

        // Listeners de los botones
        btnPermisoFlotar.setOnClickListener { pedirPermisoFlotar() }
        btnPermisoNotif.setOnClickListener { pedirPermisoNotificaciones() }
        btnPermisoMusica.setOnClickListener { pedirPermisoListenerMusica() }
    }

    override fun onResume() {
        super.onResume()
        actualizarInterfaz()
    }

    private fun actualizarInterfaz() {
        val tieneFlotar = tienePermisoParaFlotar()
        val tieneNotif = tienePermisoNotificaciones()
        val tieneMusica = tienePermisoListenerNotificaciones() // Nueva comprobación para Spotify

        if (tieneFlotar) {
            btnPermisoFlotar.text = "✅ Superposición Lista"
            btnPermisoFlotar.isEnabled = false
            btnPermisoFlotar.setBackgroundColor(Color.parseColor("#4CAF50"))
        }

        if (tieneNotif) {
            btnPermisoNotif.text = "✅ Notificaciones Listas"
            btnPermisoNotif.isEnabled = false
            btnPermisoNotif.setBackgroundColor(Color.parseColor("#4CAF50"))
        }

        if (tieneMusica) {
            btnPermisoMusica.text = "✅ Acceso Spotify Listo"
            btnPermisoMusica.isEnabled = false
            btnPermisoMusica.setBackgroundColor(Color.parseColor("#4CAF50"))
        }

        // AHORA EXIGE LOS 3 PERMISOS PARA PASAR DE PANTALLA
        if (tieneFlotar && tieneNotif && tieneMusica) {
            panelPermisos.visibility = View.GONE
            panelAjustes.visibility = View.VISIBLE
            iniciarServicioInmortal()
        } else {
            panelPermisos.visibility = View.VISIBLE
            panelAjustes.visibility = View.GONE
        }
    }

    private fun tienePermisoParaFlotar(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
    }

    private fun tienePermisoNotificaciones(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    // NUEVA FUNCIÓN: Comprueba si nos diste la llave maestra de la música
    private fun tienePermisoListenerNotificaciones(): Boolean {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabledListeners?.contains(packageName) == true
    }

    private fun pedirPermisoFlotar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            Toast.makeText(this, "Activa el interruptor para R-Tweaks", Toast.LENGTH_LONG).show()
        }
    }

    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    private fun pedirPermisoListenerMusica() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "Busca R-Tweaks y actívalo", Toast.LENGTH_LONG).show()
    }

    private fun iniciarServicioInmortal() {
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}