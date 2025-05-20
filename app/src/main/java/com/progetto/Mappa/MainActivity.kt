package com.progetto.Mappa

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.progetto.Mappa.ui.theme.MapScreen
import com.progetto.Mappa.ui.theme.MappaTheme

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MappaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    val isDark = isSystemInDarkTheme()
                    MapScreen(isDarkMode = isDark)
                }
            }
        }
    }
}
