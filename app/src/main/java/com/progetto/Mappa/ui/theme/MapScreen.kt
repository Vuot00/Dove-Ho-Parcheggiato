import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch


const val LOCATION_DEBUG_TAG = "LocationTracking"
private const val DEFAULT_MAP_ZOOM = 17f

@SuppressLint("MissingPermission")
@Composable
fun MapScreenWithLocation(isDarkMode: Boolean) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()

    fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            permissionGranted = granted
            showPermissionRationale = !granted
        }
    )

    LaunchedEffect(Unit) {
        permissionGranted = checkPermission()
        if (!permissionGranted) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val latest = locationResult.lastLocation
                if (latest != null) {
                    currentLocation = LatLng(latest.latitude, latest.longitude)
                    loading = false
                }
            }
        }
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build()

            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                context.mainLooper
            )

            // fallback timeout se la posizione non arriva
            coroutineScope.launch {
                kotlinx.coroutines.delay(10000)
                if (currentLocation == null) {
                    loading = false
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !permissionGranted -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("L'app ha bisogno del permesso di localizzazione.")
                    if (showPermissionRationale) {
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", context.packageName, null)
                            intent.data = uri
                            context.startActivity(intent)
                        }) {
                            Text("Apri Impostazioni")
                        }
                    } else {
                        Button(onClick = {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }) {
                            Text("Concedi permesso")
                        }
                    }
                }
            }

            loading -> {
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

            currentLocation != null -> {
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(currentLocation!!, DEFAULT_MAP_ZOOM)
                }

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = true),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false
                    )
                ) {
                    Marker(
                        state = MarkerState(position = currentLocation!!),
                        title = "Posizione Auto",
                        snippet = "La tua auto è qui"
                    )
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngZoom(currentLocation!!, DEFAULT_MAP_ZOOM),
                                durationMs = 1000
                            )
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(horizontal = 14.dp, vertical = 30.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(Icons.Default.GpsFixed, contentDescription = "Centra posizione")
                }

                Button(
                    onClick = {
                        Toast.makeText(context, "Posizione salvata!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(horizontal = 16.dp, vertical = 50.dp)
                        .fillMaxWidth()
                ) {
                    Text("Salva posizione")
                }



            }

            else -> {
                // caso raro: posizione nulla ma non in caricamento
                Text("Impossibile determinare la posizione.")
            }
        }
    }
}

