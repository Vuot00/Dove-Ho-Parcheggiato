package com.progetto.Mappa

import MapScreenWithLocation
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.progetto.Mappa.ui.theme.MappaTheme

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

        // Nasconde l'Action Bar
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
                    if (permissionState.value) {
                        MapScreenWithLocation(isDarkMode = isSystemInDarkTheme())
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
fun RequestLocationPermissionScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 50.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = onRequestPermission) {
            Text("L'app ha bisogno dei permessi di localizzazione")
        }
    }
}