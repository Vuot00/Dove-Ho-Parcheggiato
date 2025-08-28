package com.progetto.Mappa

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.navigation.NavigationView
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import android.provider.Settings
import android.os.Handler
import android.os.Looper

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    // ######################################## VARIABILI ########################################
    private lateinit var googleMap: GoogleMap
    private lateinit var db: AppDatabase
    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private var gyroEnabled = false


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
    @SuppressLint("MissingPermission", "MissingInflatedId", "PotentialBehaviorOverride")
    override fun onCreate(savedInstanceState: Bundle?) {
        loadSavedLanguage()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        supportActionBar?.hide()
        hideSystemUI()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

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
            val newCameraPosition =
                CameraPosition.Builder(currentCameraPosition)
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

                    // Mostra un dialogo di scelta
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.parcheggio_a_tempo_domanda))
                        .setMessage(getString(R.string.domanda_dialogo))
                        .setPositiveButton(getString(R.string.si)) { _, _ ->
                            chiediDurataParcheggio(currentLatLng)
                        }
                        .setNegativeButton("No") { _, _ ->
                            googleMap.clear()
                            googleMap.addMarker(
                                MarkerOptions().position(currentLatLng)
                                    .title(getString(R.string.posizione_auto))
                            )
                            Toast.makeText(this, getString(R.string.posizione_salvata), Toast.LENGTH_SHORT).show()
                        }
                        .show()

                    // Salva nel database Room
                    CoroutineScope(Dispatchers.IO).launch {
                        db.savedLocationDao().insertLocation(
                            SavedLocation(latitude = location.latitude, longitude = location.longitude)
                        )
                    }


                }
            }
        }

        // ######################################## MENU LATERALE ########################################
        val isDarkTheme = resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        navigationView.setBackgroundColor(
            if (isDarkTheme) Color.parseColor("#162340") else Color.WHITE
        )
        val menuButton = findViewById<ImageButton>(R.id.menu_button)

        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.voice_about -> {
                    Toast.makeText(this, getString(R.string.informazioni), Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.voice_language -> {
                    showLanguagePopup(menuButton)
                    true
                }
                else -> false
            }.also {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }

        // GESTIONE SWITCH
        val toggleItem = navigationView.menu.findItem(R.id.voice_toggle_option)
        val toggleSwitch = toggleItem.actionView?.findViewById<Switch>(R.id.nav_switch)

        toggleSwitch?.setOnCheckedChangeListener { _, isChecked ->
            gyroEnabled = isChecked
            if (isChecked) {
                rotationSensor?.also { sensor ->
                    sensorManager.registerListener(rotationListener, sensor, SensorManager.SENSOR_DELAY_UI)
                }
                Toast.makeText(this, getString(R.string.gyro_abilitato), Toast.LENGTH_SHORT).show()            } else {
                sensorManager.unregisterListener(rotationListener)
                Toast.makeText(this, getString(R.string.gyro_disasabilitato), Toast.LENGTH_SHORT).show()
            }
        }

    }

    private val rotationListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!gyroEnabled) return

            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            val orientationAngles = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()

            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder(googleMap.cameraPosition)
                    .bearing(azimuth)
                    .build()
            ))
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(rotationListener)
    }


    // ######################################## DIALOGO PARCHEGGIO ########################################
    private fun chiediDurataParcheggio(position: LatLng) {

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = getString(R.string.inserisci_minuti)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.durata_parcheggio))
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val durata = input.text.toString().trim().toIntOrNull()
                if (durata != null && durata > 0) {
                    googleMap.clear()
                    googleMap.addMarker(
                        MarkerOptions().position(position).title(getString(R.string.parcheggio_a_tempo))
                    )

                    // Controllo permesso exact alarm
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val alarmManager = getSystemService(AlarmManager::class.java)
                        if (!alarmManager.canScheduleExactAlarms()) {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            startActivity(intent)
                            Toast.makeText(this, getString(R.string.permesso_notifica), Toast.LENGTH_LONG).show()
                            return@setPositiveButton
                        }
                    }

                    try {
                        programmaNotifica(durata)
                        Toast.makeText(this, getString(R.string.notifica_programmata, durata), Toast.LENGTH_SHORT).show()
                        Handler(Looper.getMainLooper()).postDelayed({
                            Toast.makeText(this, getString(R.string.posizione_salvata), Toast.LENGTH_SHORT).show()
                        }, 2000)
                    } catch (e: Exception) {
                        Toast.makeText(this, getString(R.string.notifica_programmata, durata), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, getString(R.string.valore_invalido, durata), Toast.LENGTH_SHORT).show()                }
            }
            .setNegativeButton(getString(R.string.annulla), null)
            .show()
    }




    private fun programmaNotifica(minuti: Int) {
        val intent = Intent(this, NotificaReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            System.currentTimeMillis().toInt(), // requestCode unico
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + 1000L * 60 * minuti
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }


    // ######################################## POPUP LINGUE ########################################
    private fun showLanguagePopup(anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menu.add("🇮🇹 Italiano")
        popup.menu.add("🇬🇧 English")
        popup.menu.add("🇪🇸 Español")

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "🇮🇹 Italiano" -> changeAppLanguage("it")
                "🇬🇧 English" -> changeAppLanguage("en")
                "🇪🇸 Español"-> changeAppLanguage("es")
            }
            true
        }

        popup.show()
    }

    private fun changeAppLanguage(languageCode: String) {
        saveLanguage(languageCode)
        setLocale(languageCode)
        recreate()
    }


    // ######################################## FULLSCREEN SEMPRE ATTIVO ########################################
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    // ######################################## CALLBACK MAPPA ########################################
    @SuppressLint("MissingPermission", "PotentialBehaviorOverride")
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        val isDarkTheme = resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

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
        googleMap.uiSettings.isMapToolbarEnabled = false



        centerMapOnUserLocation()

        // Info window personalizzata
        googleMap.setInfoWindowAdapter(CustomInfoWindowAdapter(this))

        // Click su info window → direzioni
        googleMap.setOnInfoWindowClickListener { marker ->
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val origin = "${location.latitude},${location.longitude}"
                    val destination = "${marker.position.latitude},${marker.position.longitude}"

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val response = RetrofitInstance.directionsApi.getDirections(
                                origin = origin,
                                destination = destination,
                                apiKey = "AIzaSyACCZa1bZWIwXEudrPwyC9DLYLtZR9VJB0"
                            )

                            if (response.isSuccessful) {
                                val directions = response.body()
                                runOnUiThread {
                                    val route = directions?.routes?.firstOrNull()
                                    route?.overviewPolyline?.points?.let { polylineEncoded ->
                                        val decodedPath = PolyUtil.decode(polylineEncoded)
                                        googleMap.addPolyline(
                                            PolylineOptions()
                                                .addAll(decodedPath)
                                                .color(Color.BLUE)
                                                .width(8f)
                                        )
                                    }
                                }
                            } else {
                                Log.e("DirectionsAPI", "Errore: ${response.errorBody()?.string()}")
                            }
                        } catch (e: Exception) {
                            Log.e("DirectionsAPI", "Exception: ${e.message}")
                        }
                    }
                } else {
                    Toast.makeText(this, getString(R.string.impossibile_ottenere_posizione_attuale), Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Bottone posizione utente
        val myLocationButton: ImageButton = findViewById(R.id.my_location_button)
        myLocationButton.setOnClickListener {
            centerMapOnUserLocation()
        }

        // Carica posizione salvata
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

    // ######################################## CENTRA MAPPA ########################################
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun centerMapOnUserLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
            }
        }
    }

    private fun loadSavedLanguage() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val languageCode = prefs.getString("language", "it") ?: "it"
        setLocale(languageCode)
    }

    private fun saveLanguage(langCode: String) {
        getSharedPreferences("settings", MODE_PRIVATE)
            .edit()
            .putString("language", langCode)
            .apply()
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    // ######################################## CUSTOM INFO WINDOW ########################################
    class CustomInfoWindowAdapter(val context: Context) : GoogleMap.InfoWindowAdapter {
        override fun getInfoContents(marker: Marker): View? {
            val view = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null)
            val title = view.findViewById<TextView>(R.id.title)
            val button = view.findViewById<Button>(R.id.btn_action)

            title.text = marker.title ?: context.getString(R.string.auto_parcheggiata)
            button.text = context.getString(R.string.portami_all_auto)


            return view
        }

        override fun getInfoWindow(marker: Marker): View? = null
    }

}
