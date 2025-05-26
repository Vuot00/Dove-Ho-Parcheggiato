package com.progetto.Mappa

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.progetto.Mappa.ui.theme.MappaTheme
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var permissionState: MutableState<Boolean>

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        permissionState.value = isGranted
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        supportActionBar?.hide()

        permissionState = mutableStateOf(
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )

        setContent {
            MappaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LanguageSelector { langCode ->
                            setLocale(langCode)
                            recreate()
                        }

                        if (permissionState.value) {
                            LaunchedEffect(Unit) {
                                startActivity(Intent(this@MainActivity, MapActivity::class.java))
                                finish()
                            }
                        } else {
                            RequestLocationPermissionScreen {
                                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun LanguageSelector(onLanguageSelected: (String) -> Unit) {
        var expanded by remember { mutableStateOf(false) }
        val languages = mapOf("Italiano" to "it", "English" to "en", "Español" to "es")

        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Button(onClick = { expanded = true }) {
                Text("🌐 " + stringResource(id = R.string.language_button))
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                languages.forEach { (name, code) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            expanded = false
                            onLanguageSelected(code)
                        }
                    )
                }
            }
        }
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
