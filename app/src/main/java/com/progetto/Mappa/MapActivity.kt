package com.progetto.Mappa

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.res.Configuration
import android.graphics.Color
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
import androidx.core.app.NotificationCompat
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
    @SuppressLint("MissingPermission", "MissingInflatedId", "PotentialBehaviorOverride")
    override fun onCreate(savedInstanceState: Bundle?) {
        loadSavedLanguage()

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
                        .setTitle("Parcheggio a tempo?")
                        .setMessage("Vuoi salvare questo parcheggio come temporaneo?")
                        .setPositiveButton("Sì") { _, _ ->
                            chiediDurataParcheggio(currentLatLng)
                        }
                        .setNegativeButton("No") { _, _ ->
                            googleMap.clear()
                            googleMap.addMarker(
                                MarkerOptions().position(currentLatLng)
                                    .title(getString(R.string.posizione_auto))
                            )
                        }
                        .show()

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
                    Toast.makeText(this, "da tradurre", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, if (isChecked) "Attivato" else "Disattivato", Toast.LENGTH_SHORT).show()
        }
    }

    // ######################################## DIALOGO PARCHEGGIO ########################################
    private fun chiediDurataParcheggio(position: LatLng) {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = "Durata in minuti"

        AlertDialog.Builder(this)
            .setTitle("Durata parcheggio")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val durataStr = input.text.toString()
                Log.d("DEBUG", "Durata inserita: '$durataStr'")
                val durata = durataStr.toIntOrNull()
                if (durata != null && durata > 0) {
                    googleMap.clear()
                    googleMap.addMarker(
                        MarkerOptions().position(position).title("Parcheggio a tempo")
                    )
                    programmaNotifica(durata)
                } else {
                    Toast.makeText(this, "Inserisci un valore valido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun programmaNotifica(minuti: Int) {
        val intent = Intent(this, NotificaReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + minuti * 60 * 1000
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }

    class NotificaReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "parking_channel",
                    "Parcheggio",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, "parking_channel")
                .setContentTitle("Parcheggio in scadenza")
                .setContentText("Il tuo parcheggio a tempo sta per scadere!")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()

            notificationManager.notify(1, notification)
        }
    }

    // ######################################## POPUP LINGUE ########################################
    private fun showLanguagePopup(anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menu.add("Italiano")
        popup.menu.add("English")
        popup.menu.add("Español")

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Italiano" -> changeAppLanguage("it")
                "English" -> changeAppLanguage("en")
                "Español" -> changeAppLanguage("es")
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
                                apiKey = "YOUR_API_KEY"
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
                    Toast.makeText(this, "Impossibile ottenere la posizione attuale", Toast.LENGTH_SHORT).show()
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
            title.text = marker.title
            button.text = "Portami all'auto"
            return view
        }

        override fun getInfoWindow(marker: Marker): View? = null
    }
}
