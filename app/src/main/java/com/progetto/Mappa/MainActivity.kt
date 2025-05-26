package com.progetto.Mappa

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.progetto.Mappa.ui.theme.MappaTheme
import com.progetto.Mappa.ui.theme.components.LanguageMenuBox
import java.util.*

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
    /*
==============================================
🚀 COSA ABBIAMO CAMBIATO E PERCHÉ
==============================================

✅ 1. SPOSTATO IL CONTROLLO DEI PERMESSI PRIMA DEL setContent:
   Prima, anche se i permessi erano già stati dati, la UI Compose veniva comunque mostrata per un istante
   (in particolare il menu a tendina della lingua), creando un “flash” visivo fastidioso.

   ➤ Ora controlliamo subito se i permessi sono già presenti. Se sì, lanciamo direttamente MapActivity
     e chiudiamo MainActivity PRIMA di arrivare a setContent.

✅ 2. Rimossa la variabile permissionState e usato direttamente il controllo di sistema.
   Meno codice da gestire, e più immediato.

✅ 3. Inserito requestPermissionLauncher dentro Compose usando rememberLauncherForActivityResult.
   Così tutto il flusso dei permessi è gestito dentro Compose, più semplice e moderno.

✅ 4. Organizzato il codice in sezioni commentate per chiarezza futura.

==============================================
🎯 RISULTATO
==============================================
✔️ Nessun lampeggiamento iniziale
✔️ Cambio lingua con persistenza
✔️ UI visibile solo se serve davvero chiedere il permesso

*/

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        hideSystemUI()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()

        // Applica lingua salvata prima di qualsiasi UI
        val lang = getSharedPreferences("settings", MODE_PRIVATE)
            .getString("language", "it") ?: "it"
        setLocale(lang)

        // ######################################## CONTROLLO PERMESSI IMMEDIATO ########################################
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startActivity(Intent(this, MapActivity::class.java))
            finish()
            return
        }

        // ######################################## UI COMPOSABLE SOLO SE SERVE PERMESSO ########################################
        setContent {
            MappaTheme {
                var currentLang by rememberSaveable {
                    mutableStateOf(lang)
                }

                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        startActivity(Intent(this, MapActivity::class.java))
                        finish()
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {

                        // MENU A TENDINA PER CAMBIO LINGUA
                        LanguageMenuBox(
                            currentLang = currentLang,
                            onLanguageSelected = { langCode ->
                                currentLang = langCode
                                saveLanguage(langCode)
                                setLocale(langCode)
                                recreate()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, top = 16.dp)
                        )

                        // UI PER RICHIEDERE IL PERMESSO
                        RequestLocationPermissionScreen {
                            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
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
