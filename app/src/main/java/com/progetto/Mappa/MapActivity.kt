package com.progetto.Mappa

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.res.stringResource
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    // ######################################## VARIABILI ########################################
    private lateinit var googleMap: GoogleMap
    private lateinit var db: AppDatabase

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    // ######################################## UI IMMERSIVA ########################################
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
    @SuppressLint("MissingPermission", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        supportActionBar?.hide()
        hideSystemUI()

        // Inizializza DB
        db = AppDatabase.getInstance(applicationContext)

        // Inizializza mappa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // BOTTONE BUSSOLA
        val compassButton: ImageButton = findViewById(R.id.my_bussolina)
        compassButton.setOnClickListener {
            val currentCameraPosition = googleMap.cameraPosition
            val newCameraPosition = com.google.android.gms.maps.model.CameraPosition.Builder(currentCameraPosition)
                .bearing(0f) // sempre nord
                .tilt(0f)    // resetta anche l'inclinazione
                .build()
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition))
        }

        // BOTTONE SALVA POSIZIONE
        val actionButton: Button = findViewById(R.id.custom_action_button)
        actionButton.setOnClickListener {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    googleMap.clear()
                    googleMap.addMarker(
                        MarkerOptions().position(currentLatLng).title(getString(R.string.posizione_auto))
                    )

                    // Salva nel database Room
                    CoroutineScope(Dispatchers.IO).launch {
                        db.savedLocationDao().insertLocation(
                            SavedLocation(latitude = location.latitude, longitude = location.longitude)
                        )
                    }

                    Toast.makeText(this, getString(R.string.posizione_salvata), Toast.LENGTH_SHORT).show()
                }
            }
        }

        // BOTTONE MENU (3 puntini)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        val menuButton = findViewById<ImageButton>(R.id.menu_button)

        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.voice_settings -> {
                    Toast.makeText(this, "Impostazioni", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.voice_about -> {
                    Toast.makeText(this, "Info sull'app", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }.also {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }
    }

    // ######################################## FULLSCREEN SEMPRE ATTIVO ########################################
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    // ######################################## CALLBACK MAPPA PRONTA ########################################
    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Applica lo stile in base al tema del sistema
        val isDarkTheme = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES

        if (isDarkTheme) {
            try {
                val success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_dark_style)
                )
                if (!success) {
                    Log.e("MapActivity", "Style parsing failed.")
                }
            } catch (e: Exception) {
                Log.e("MapActivity", "Cannot load map style", e)
            }
        }

        googleMap.uiSettings.isMyLocationButtonEnabled = false
        googleMap.isMyLocationEnabled = true
        googleMap.uiSettings.isCompassEnabled = false
        googleMap.uiSettings.isRotateGesturesEnabled = true

        // Centra sulla posizione attuale
        centerMapOnUserLocation()

        // ######################################## BOTTONE POSIZIONE UTENTE ########################################
        val myLocationButton: ImageButton = findViewById(R.id.my_location_button)
        myLocationButton.setOnClickListener {
            centerMapOnUserLocation()
        }

        // ######################################## CARICA POSIZIONE SALVATA ########################################
        CoroutineScope(Dispatchers.IO).launch {
            val saved = db.savedLocationDao().getLocation()
            saved?.let {
                val savedLatLng = LatLng(it.latitude, it.longitude)
                runOnUiThread {
                    googleMap.addMarker(
                        MarkerOptions().position(savedLatLng).title(getString(R.string.auto_parcheggiata))
                    )
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(savedLatLng, 17f))
                }
            }
        }
    }

    // ######################################## CENTRA MAPPA SU POSIZIONE ATTUALE ########################################
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun centerMapOnUserLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
            }
        }
    }
}
