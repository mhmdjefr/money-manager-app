package com.mhmdjefr.moneymanager.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import com.mhmdjefr.moneymanager.R
import com.mhmdjefr.moneymanager.ui.theme.*

@Composable
fun SplashScreen(navController: NavController) {
    // Timer buat pindah halaman otomatis
    LaunchedEffect(key1 = true) {
        delay(2000L) // Tampil selama 2 detik (2000 ms)
        navController.navigate("dashboard") {
            // Hapus splash dari history biar kalau user mencet tombol 'Back', gak balik lagi ke sini
            popUpTo("splash") { inclusive = true }
        }
    }

    // UI Splash Screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White), // Ganti sesuai pilihan warna kamu, misal SoftBlue
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.splash_icon),
                contentDescription = "App Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(140.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Money Manager",
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "Developed by Jeffri",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}
