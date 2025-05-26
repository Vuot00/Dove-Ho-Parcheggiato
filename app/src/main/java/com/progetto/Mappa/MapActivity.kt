package com.progetto.Mappa

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var db: AppDatabase

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

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

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        supportActionBar?.hide()
        hideSystemUI()

        db = AppDatabase.getInstance(applicationContext)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val actionButton: Button = findViewById(R.id.custom_action_button)

        val compassButton: ImageButton = findViewById(R.id.my_bussolina)
        compassButton.setOnClickListener {
            val currentCameraPosition = googleMap.cameraPosition

            val newCameraPosition = com.google.android.gms.maps.model.CameraPosition.Builder(currentCameraPosition)
                .bearing(0f) // sempre nord
                .tilt(0f)    // resetta anche l'inclinazione
                .build()

            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition))
        }

        actionButton.setOnClickListener {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    googleMap.clear()
                    googleMap.addMarker(
                        MarkerOptions().position(currentLatLng).title("La tua auto è qui!")
                    )

                    // Salva nel database
                    CoroutineScope(Dispatchers.IO).launch {
                        db.savedLocationDao().insertLocation(
                            SavedLocation(latitude = location.latitude, longitude = location.longitude)
                        )
                    }

                    Toast.makeText(this, "Posizione salvata!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap.uiSettings.isMyLocationButtonEnabled = false
        googleMap.isMyLocationEnabled = true
        googleMap.uiSettings.isCompassEnabled = false
        googleMap.uiSettings.isRotateGesturesEnabled = true



        centerMapOnUserLocation()

        val myLocationButton: ImageButton = findViewById(R.id.my_location_button)
        myLocationButton.setOnClickListener {
            centerMapOnUserLocation()
        }

        // Carica posizione salvata e mostra marker
        CoroutineScope(Dispatchers.IO).launch {
            val saved = db.savedLocationDao().getLocation()
            saved?.let {
                val savedLatLng = LatLng(it.latitude, it.longitude)
                runOnUiThread {
                    googleMap.addMarker(
                        MarkerOptions().position(savedLatLng).title("Auto parcheggiata")
                    )
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(savedLatLng, 17f))
                }
            }
        }
    }

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
