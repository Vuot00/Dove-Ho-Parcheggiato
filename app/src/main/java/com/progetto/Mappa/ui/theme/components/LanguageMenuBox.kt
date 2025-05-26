package com.progetto.Mappa.ui.theme.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun LanguageMenuBox(
    currentLang: String,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopStart
    ) {
        LanguageDropdownMenu(
            currentLang = currentLang,
            onLanguageSelected = onLanguageSelected
        )
    }
}
