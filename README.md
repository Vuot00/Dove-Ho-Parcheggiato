# 🚗 Dove ho Parcheggiato

"Dove ho Parcheggiato" è un'applicazione Android sviluppata in Kotlin che implementa la funzionalità di salvataggio parcheggio. L'app utilizza la posizione GPS del dispositivo per memorizzare dove hai parcheggiato e ti aiuta a ritrovare la tua auto con indicazioni dettagliate sulla mappa.

## ✨ Funzionalità Principali

- Salvataggio automatico posizione: Salva la posizione del parcheggio con un tap
- Visualizzazione su mappa interattiva: Utilizza Google Maps con temi chiaro/scuro
- Navigazione pedonale: Calcolo del percorso a piedi con visualizzazione su mappa
- Parcheggio a tempo: Imposta timer per parcheggi con scadenza
- Notifiche programmabili: Ricevi un avviso quando il parcheggio sta per scadere
- Supporto multilingua: Interfaccia in Italiano, English e Español
- Rotazione mappa con giroscopio: Orienta la mappa automaticamente (opzionale)
- Modalità immersiva: Esperienza fullscreen ottimizzata
- Tema scuro: Supporto automatico per dark mode di sistema
- Info window personalizzata: Interfaccia elegante per i marker sulla mappa

## 🛠️ Tecnologie Utilizzate

- Linguaggio: Kotlin
- UI Framework: Android Views + Jetpack Compose (componenti UI)
- Minimum SDK: Android 5.0 (API 21)
- Target SDK: Android 14 (API 34)

## Librerie Principali

- Google Maps Android API: Visualizzazione mappa e marker
- Google Directions API: Calcolo percorsi pedonali
- Room Database: Persistenza locale delle posizioni salvate
- Retrofit 2: Chiamate API per le direzioni
- Gson: Parsing JSON delle risposte API
- Fused Location Provider: Gestione GPS e localizzazione
- Material Design 3: Componenti UI moderni
- Jetpack Compose: Menu lingua e componenti UI reattivi

## Sensori

- GPS: Localizzazione precisa
- Rotation Vector Sensor: Orientamento automatico mappa con giroscopio

## Autori:
- D'Angelo Francesco
- Giurranna Antonio Pio
