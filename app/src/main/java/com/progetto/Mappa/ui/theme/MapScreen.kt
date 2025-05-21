import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log // <--- AGGIUNGI QUESTO IMPORT
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

const val LOCATION_DEBUG_TAG = "LocationTracking" // Tag per i log
private const val DEFAULT_MAP_ZOOM = 17f
private val FALLBACK_INITIAL_LOCATION = LatLng(0.0, 0.0)

@SuppressLint("MissingPermission")
@Composable
fun MapScreenWithLocation(isDarkMode: Boolean) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var locationUpdatesActive by remember { mutableStateOf(false) }

    fun hasLocationPermission(): Boolean {
        val permissionCheck = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        Log.d(LOCATION_DEBUG_TAG, "hasLocationPermission: $permissionCheck")
        return permissionCheck
    }

    var permissionGranted by remember { mutableStateOf(hasLocationPermission()) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            Log.d(LOCATION_DEBUG_TAG, "Permission result: $granted")
            permissionGranted = granted
            if (granted) {
                locationUpdatesActive = true
                Log.d(LOCATION_DEBUG_TAG, "Permission GRANTED, locationUpdatesActive set to true")
            } else {
                showPermissionRationale = true
                Log.d(LOCATION_DEBUG_TAG, "Permission DENIED, showPermissionRationale set to true")
            }
        }
    )

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d(LOCATION_DEBUG_TAG, "onLocationResult CALLED") // <--- LOG IMPORTANTE
                if (locationResult.locations.isNotEmpty()) { // Controlla se la lista non è vuota
                    val latestLocation =
                        locationResult.locations.last() // Prendi l'ultima (o la prima, se preferisci)
                    Log.d(
                        LOCATION_DEBUG_TAG,
                        "Location received: Lat ${latestLocation.latitude}, Lng ${latestLocation.longitude}, Accuracy: ${latestLocation.accuracy}"
                    )
                    currentLocation = LatLng(latestLocation.latitude, latestLocation.longitude)
                    // Considera di fermare gli aggiornamenti se ti serve solo una posizione
                    // fusedLocationClient.removeLocationUpdates(this)
                    // locationUpdatesActive = false
                    // Log.d(LOCATION_DEBUG_TAG, "Stopping location updates after first fix.")
                } else {
                    Log.d(LOCATION_DEBUG_TAG, "onLocationResult: locationResult.locations is EMPTY")
                }

                // Codice originale che usava lastLocation (può essere nullo anche se locations ha elementi)
                locationResult.lastLocation?.let { location ->
                    Log.d(
                        LOCATION_DEBUG_TAG,
                        "locationResult.lastLocation is NOT NULL: Lat ${location.latitude}, Lng ${location.longitude}"
                    )
                    // Non sovrascrivere se abbiamo già preso da locationResult.locations
                    // currentLocation = LatLng(location.latitude, location.longitude)
                } ?: run {
                    Log.d(
                        LOCATION_DEBUG_TAG,
                        "onLocationResult: locationResult.lastLocation is NULL"
                    )
                }
            }
        }
    }

    LaunchedEffect(permissionGranted, locationUpdatesActive) {
        Log.d(
            LOCATION_DEBUG_TAG,
            "LaunchedEffect triggered. PermissionGranted: $permissionGranted, LocationUpdatesActive: $locationUpdatesActive"
        )
        if (permissionGranted && locationUpdatesActive) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
                .setMinUpdateIntervalMillis(5000L)
                .build()
            try {
                Log.d(LOCATION_DEBUG_TAG, "Attempting to request location updates...")
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    context.mainLooper
                )
                Log.d(LOCATION_DEBUG_TAG, "requestLocationUpdates SUCCEEDED")
            } catch (e: SecurityException) {
                Log.e(LOCATION_DEBUG_TAG, "SecurityException requesting updates: ${e.message}", e)
                permissionGranted = false // Assicurati che lo stato sia corretto
            } catch (e: Exception) {
                Log.e(LOCATION_DEBUG_TAG, "Generic Exception requesting updates: ${e.message}", e)
            }
        } else if (!locationUpdatesActive) {
            Log.d(
                LOCATION_DEBUG_TAG,
                "LaunchedEffect: Conditions not met or updates explicitly stopped. Removing updates."
            )
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(
                LOCATION_DEBUG_TAG,
                "DisposableEffect: onDispose CALLED. Removing location updates."
            )
            fusedLocationClient.removeLocationUpdates(locationCallback)
            locationUpdatesActive = false
        }
    }

    // Log per lo stato corrente prima del when
    Log.d(
        LOCATION_DEBUG_TAG,
        "UI State Check: permissionGranted=$permissionGranted, currentLocation=$currentLocation, showPermissionRationale=$showPermissionRationale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !permissionGranted -> {
                Log.d(LOCATION_DEBUG_TAG, "UI State: NOT PERMISSION GRANTED")
                // ... (resto del codice per il caso !permissionGranted)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (showPermissionRationale) {
                        Text(
                            "L'accesso alla posizione è necessario per mostrare la tua posizione sulla mappa.",
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", context.packageName, null)
                            intent.data = uri
                            context.startActivity(intent)
                            showPermissionRationale = false
                        }) {
                            Text("Apri Impostazioni")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            showPermissionRationale = false
                        }) {
                            Text("Riprova a concedere il permesso")
                        }

                    } else {
                        Button(
                            onClick = {
                                Log.d(
                                    LOCATION_DEBUG_TAG,
                                    "Button 'Concedi permesso' clicked. Launching permission request."
                                )
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        ) {
                            Text("Concedi permesso localizzazione")
                        }
                    }
                }
            }

            currentLocation == null -> {
                Log.d(LOCATION_DEBUG_TAG, "UI State: CURRENT LOCATION IS NULL (Loading...)")
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Caricamento posizione in corso...")
                }
            }

            else -> {
                Log.d(
                    LOCATION_DEBUG_TAG,
                    "UI State: CURRENT LOCATION AVAILABLE - SHOWING MAP. Actual currentLocation: $currentLocation"
                )

                // Anche se siamo in questo blocco 'else', è buona norma gestire la nullabilità
                // nel caso in cui `currentLocation` cambi stato molto rapidamente durante la ricomposizione.
                // Tuttavia, dato il `when` precedente, `currentLocation` qui NON DOVREBBE essere null.
                // Usiamo `currentLocation!!` qui con la consapevolezza che il when lo ha già verificato.
                // Se preferisci una sicurezza aggiuntiva, potresti usare `currentLocation ?: FALLBACK_INITIAL_LOCATION`
                // ma idealmente non dovrebbe essere necessario se la logica del when è corretta.
                val initialMapLocation = currentLocation!!

                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(initialMapLocation, DEFAULT_MAP_ZOOM)
                }

                // Effetto per animare la camera quando `currentLocation` cambia
                LaunchedEffect(currentLocation) {
                    // `currentLocation` qui potrebbe essere cambiato da quando `cameraPositionState` è stato inizializzato
                    // o potrebbe essere la stessa istanza se questo è il primo avvio dell'effetto con una posizione valida.
                    currentLocation?.let { newLocation ->
                        Log.d(LOCATION_DEBUG_TAG, "Map Camera Animate: New location $newLocation")

                        // Controlla se la posizione è effettivamente cambiata prima di animare
                        // per evitare animazioni non necessarie se `currentLocation` emette lo stesso valore.
                        // Questo controllo è opzionale e dipende da quanto spesso `currentLocation` potrebbe aggiornarsi
                        // con valori identici. Per `LatLng` è buona norma confrontare i valori.
                        val currentCameraTarget = cameraPositionState.position.target
                        if (currentCameraTarget.latitude != newLocation.latitude ||
                            currentCameraTarget.longitude != newLocation.longitude ||
                            cameraPositionState.position.zoom != DEFAULT_MAP_ZOOM
                        ) { // Controlla anche lo zoom se vuoi resettarlo

                            cameraPositionState.animate(
                                update = com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition(
                                    CameraPosition(
                                        newLocation,
                                        DEFAULT_MAP_ZOOM,
                                        0f,
                                        0f
                                    ) // tilt e bearing a 0
                                ),
                                durationMs = 1000 // Durata dell'animazione in millisecondi
                            )
                        } else {
                            Log.d(
                                LOCATION_DEBUG_TAG,
                                "Map Camera: Position $newLocation is already set. Skipping animation."
                            )
                        }
                    } ?: run {
                        // Questo blocco viene eseguito se `currentLocation` diventa `null`
                        // dopo essere stato non-null. Potrebbe non essere uno scenario previsto
                        // se la logica generale mantiene la mappa visibile solo con una posizione valida.
                        Log.d(
                            LOCATION_DEBUG_TAG,
                            "Map Camera: Current location became null after initial setup."
                        )
                    }
                }

                // Qui il resto del codice per GoogleMap, Marker, ecc.
                // Ad esempio:
                val mapProperties = MapProperties(
                    isMyLocationEnabled = true,
                    // Altre proprietà...
                )
                val uiSettings = remember {
                    MapUiSettings(
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = true
                        // Altre impostazioni UI...
                    )
                }

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    uiSettings = uiSettings,
                    onMapLoaded = {
                        Log.d(LOCATION_DEBUG_TAG, "GoogleMap: onMapLoaded CALLED")
                        // ... (logica aggiuntiva se necessaria quando la mappa è caricata)
                    }
                ) {
                    // Il Marker dovrebbe anche usare `initialMapLocation` o `currentLocation` in modo sicuro.
                    // Dato che siamo in questo blocco, `currentLocation` (e quindi `initialMapLocation`)
                    // non dovrebbe essere null.
                    Marker(
                        state = MarkerState(position = initialMapLocation),
                        title = "Sei qui",
                        snippet = "La tua posizione attuale"
                    )
                }
            }
        }
    }
}