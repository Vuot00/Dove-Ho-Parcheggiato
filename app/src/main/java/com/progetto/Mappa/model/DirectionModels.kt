package com.progetto.Mappa.model // Assicurati che questo sia il package corretto per il tuo progetto

import com.google.gson.annotations.SerializedName // Importa questo se usi Gson

// Classe principale che rappresenta l'intera risposta JSON
data class DirectionsResponse(
    @SerializedName("geocoded_waypoints")
    val geocodedWaypoints: List<GeocodedWaypoint>?,
    val routes: List<Route>,
    val status: String,
    @SerializedName("error_message")
    val errorMessage: String? = null // Utile se lo status non è "OK"
)

// Rappresenta un singolo percorso tra l'origine e la destinazione
data class Route(
    val bounds: Bounds?,
    val copyrights: String?,
    val legs: List<Leg>,
    @SerializedName("overview_polyline")
    val overviewPolyline: OverviewPolyline, // La polyline generale per l'intero percorso
    val summary: String?,
    val warnings: List<String>?,
    @SerializedName("waypoint_order")
    val waypointOrder: List<Int>?
)

// Informazioni geografiche sui limiti del percorso
data class Bounds(
    val northeast: LatLngLiteral,
    val southwest: LatLngLiteral
)

// Rappresenta una coordinata geografica
data class LatLngLiteral(
    val lat: Double,
    val lng: Double
)

// Una "gamba" del percorso (se ci sono tappe intermedie, ci saranno più legs)
// Per un percorso semplice origine->destinazione, solitamente c'è una sola leg.
data class Leg(
    val distance: TextValueObject?,
    val duration: TextValueObject?,
    @SerializedName("duration_in_traffic")
    val durationInTraffic: TextValueObject?,
    @SerializedName("end_address")
    val endAddress: String?,
    @SerializedName("end_location")
    val endLocation: LatLngLiteral?,
    @SerializedName("start_address")
    val startAddress: String?,
    @SerializedName("start_location")
    val startLocation: LatLngLiteral?,
    val steps: List<Step>, // Sequenza di istruzioni/passaggi per questa leg
    @SerializedName("traffic_speed_entry")
    val trafficSpeedEntry: List<Any>?, // Puoi definire meglio se ti serve
    @SerializedName("via_waypoint")
    val viaWaypoint: List<Any>? // Puoi definire meglio se ti serve
)

// Un singolo passaggio o istruzione nel percorso
data class Step(
    val distance: TextValueObject,
    val duration: TextValueObject,
    @SerializedName("end_location")
    val endLocation: LatLngLiteral,
    @SerializedName("html_instructions")
    val htmlInstructions: String, // Istruzioni di navigazione (possono contenere HTML)
    val polyline: OverviewPolyline, // Polyline per questo specifico step
    @SerializedName("start_location")
    val startLocation: LatLngLiteral,
    @SerializedName("travel_mode")
    val travelMode: String, // Es. "WALKING"
    val maneuver: String? = null // Es. "turn-left"
)

// Oggetto comune per rappresentare testo e valore (es. per distanza e durata)
data class TextValueObject(
    val text: String, // Rappresentazione testuale (es. "10 km", "5 mins")
    val value: Int    // Valore numerico (es. 10000 per metri, 300 per secondi)
)

// Rappresenta la polyline codificata
data class OverviewPolyline(
    val points: String // La stringa della polyline codificata
)

// Informazioni aggiuntive sui waypoint
data class GeocodedWaypoint(
    @SerializedName("geocoder_status")
    val geocoderStatus: String?,
    @SerializedName("place_id")
    val placeId: String?,
    val types: List<String>?
)

