package com.progetto.Mappa.ui.theme

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import com.progetto.Mappa.R

@Composable
fun MapScreen(isDarkMode: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF5F5F5))
    ) {
        Image(
            painter = painterResource(id = if (isDarkMode) R.drawable.map_dark else R.drawable.map_light),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Icon(
            painter = painterResource(id = R.drawable.ic_location_pin),
            contentDescription = null,
            tint = Color.Red,
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp)
        )

        Text(
            text = "Via Pietro Giuria",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 40.dp),
            fontSize = 16.sp
        )

        Button(
            onClick = { /* Azione */ },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp) // spostato più in alto
                /*.height(56.dp)           // più alto
                .width(250.dp)           // più largo*/
                .height(86.dp)
                .width(300.dp)
        ) {
            Text(text = "Segna il parcheggio", color = Color.White, fontSize = 25.sp)
        }
    }
}
