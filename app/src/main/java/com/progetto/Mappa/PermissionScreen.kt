package com.progetto.Mappa

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun RequestLocationPermissionScreen(
    showSettingsButton: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(id = R.string.permessi_localizzazione))
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onRequestPermission) {
                Text(text = stringResource(id = R.string.concedi_permesso))
            }

            if (showSettingsButton) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onOpenSettings) {
                    Text(text = stringResource(id = R.string.apri_impostazioni))
                }
            }
        }
    }
}

