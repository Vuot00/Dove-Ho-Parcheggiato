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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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