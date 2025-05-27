package com.progetto.Mappa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.progetto.Mappa.ui.theme.components.LanguageMenuBox
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // ######################################## SISTEMA UI IMMERSIVO ########################################
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
    }

    // ######################################## ONCREATE PRINCIPALE ########################################
    override fun onCreate(savedInstanceState: Bundle?) {
        hideSystemUI()
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)

        // Applica lingua salvata prima della UI
        val lang = getSharedPreferences("settings", MODE_PRIVATE)
            .getString("language", "it") ?: "it"
        setLocale(lang)

        // ######################################## CONTROLLO PERMESSI IMMEDIATO ########################################
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val hasPermission = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startActivity(Intent(this, MapActivity::class.java))
            finish()
            return
        }

        // ######################################## SET UI XML ########################################
        setContentView(R.layout.activity_main)

        val btnRequestPermission = findViewById<Button>(R.id.btn_request_permission)
        val btnOpenSettings = findViewById<Button>(R.id.btn_open_settings)

        // ######################################## CLICK SU "CONCEDI PERMESSO" ########################################
        btnRequestPermission.setOnClickListener {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 100)
        }

        // ######################################## CLICK SU "APRI IMPOSTAZIONI" ########################################
        btnOpenSettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }

        // ######################################## INSERISCI COMPOSABLE LANGUAGE MENU ########################################
        val composeView = findViewById<ComposeView>(R.id.language_menu_compose)
        composeView.setContent {
            LanguageMenuBox(
                currentLang = lang,
                onLanguageSelected = { selectedLang ->
                    saveLanguage(selectedLang)
                    recreate() // riavvia l'Activity per applicare la nuova lingua
                }
            )
        }
    }

    // ######################################## GESTIONE RISULTATO PERMESSI ########################################
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            val permission = Manifest.permission.ACCESS_FINE_LOCATION
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

            if (granted) {
                startActivity(Intent(this, MapActivity::class.java))
                finish()
            } else {
                val shouldShow = ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
                if (!shouldShow) {
                    findViewById<Button>(R.id.btn_open_settings).visibility = View.VISIBLE
                }
            }
        }
    }

    // ######################################## SALVATAGGIO LINGUA ########################################
    private fun saveLanguage(langCode: String) {
        getSharedPreferences("settings", MODE_PRIVATE)
            .edit()
            .putString("language", langCode)
            .apply()
    }

    // ######################################## CAMBIO LINGUA ########################################
    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
