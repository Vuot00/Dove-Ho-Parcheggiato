package com.progetto.Mappa.ui.theme.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.progetto.Mappa.R

// ######################################## MENU A TENDINA LINGUA ########################################
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDropdownMenu(
    currentLang: String,
    onLanguageSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // ######################################## LINGUE DISPONIBILI ########################################
    val languages = listOf(
        "it" to "🇮🇹 Italiano",
        "en" to "🇬🇧 English",
        "es" to "🇪🇸 Español"
    )

    // ######################################## BOTTONE VISUALE ########################################
    Box {
        Button(
            onClick = { expanded = true },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
            modifier = Modifier
                .padding(4.dp)
                .height(40.dp)
        ) {
            Icon(Icons.Default.Public, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.language_button), color = Color.White)
        }

        // ######################################## MENU A DISCESA ########################################
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            languages.forEach { (code, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onLanguageSelected(code)
                    },
                    trailingIcon = {
                        if (code == currentLang) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                )
            }
        }
    }
}
