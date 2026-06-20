package com.mhmdjefr.moneymanager.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhmdjefr.moneymanager.ui.theme.*

// Mapping string ke Material Icon yang lebih variatif
fun getAvatarIcon(name: String): ImageVector {
    return when (name) {
        "Person" -> Icons.Default.Person
        "Face" -> Icons.Default.Face
        "CrueltyFree" -> Icons.Default.CrueltyFree       // Kelinci / Hewan
        "Pets" -> Icons.Default.Pets                     // Kucing / Anjing
        "BugReport" -> Icons.Default.BugReport           // Kumbang / Serangga
        "SmartToy" -> Icons.Default.SmartToy             // Robot
        "Adb" -> Icons.Default.Adb                       // Alien / Android
        "Savings" -> Icons.Default.Savings               // Babi (Piggy Bank)
        "FlutterDash" -> Icons.Default.FlutterDash       // Burung
        "EmojiEmotions" -> Icons.Default.EmojiEmotions   // Smiley
        "Cyclone" -> Icons.Default.Cyclone               // Naga / Monster Air
        "ChildCare" -> Icons.Default.ChildCare           // Bayi
        "WbSunny" -> Icons.Default.WbSunny               // Matahari
        "AcUnit" -> Icons.Default.AcUnit                 // Salju / Es
        "LocalFlorist" -> Icons.Default.LocalFlorist     // Bunga / Tumbuhan
        else -> Icons.Default.Person
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("money_prefs", Context.MODE_PRIVATE) }

    var userName by remember { mutableStateOf(sharedPrefs.getString("user_name", "User") ?: "User") }
    var userAvatar by remember { mutableStateOf(sharedPrefs.getString("user_avatar", "Person") ?: "Person") }

    var inputName by remember { mutableStateOf(userName) }
    var selectedAvatar by remember { mutableStateOf(userAvatar) }
    var isEditing by remember { mutableStateOf(false) }

    // 15 Opsi Avatar
    val avatarList = listOf(
        "Person", "Face", "CrueltyFree", "Pets", "BugReport",
        "SmartToy", "Adb", "Savings", "FlutterDash", "EmojiEmotions",
        "Cyclone", "ChildCare", "WbSunny", "AcUnit", "LocalFlorist"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        },
        containerColor = LightBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Tampilan Lingkaran Avatar Utama
            Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(SoftBlue.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = getAvatarIcon(if (isEditing) selectedAvatar else userAvatar),
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = SoftBlue
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("Your Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))
                Text("Choose Avatar Icon:", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSecondary, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(12.dp))

                // Grid Pilihan Avatar (Tinggi ditambah biar bisa scroll kalau layar kecil)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                ) {
                    items(avatarList) { avatarName ->
                        val isSelected = selectedAvatar == avatarName
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) SoftBlue else CardWhite)
                                .border(1.dp, if (isSelected) Color.Transparent else TextSecondary.copy(alpha = 0.3f), CircleShape)
                                .clickable { selectedAvatar = avatarName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getAvatarIcon(avatarName),
                                contentDescription = null,
                                tint = if (isSelected) Color.White else TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        userName = inputName
                        userAvatar = selectedAvatar
                        sharedPrefs.edit()
                            .putString("user_name", inputName)
                            .putString("user_avatar", selectedAvatar)
                            .apply()
                        isEditing = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftBlue),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            } else {
                Text(userName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { isEditing = true; inputName = userName; selectedAvatar = userAvatar }, colors = ButtonDefaults.buttonColors(containerColor = SoftBlue.copy(alpha = 0.1f), contentColor = SoftBlue)) {
                    Text("Edit Profile")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        },
        containerColor = LightBackground
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Local Data Storage", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Your financial data is stored locally on this device using Room Database. We do not upload, share, or sell your personal transactions to any external servers.",
                        fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        },
        containerColor = LightBackground
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(48.dp))
            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)).background(SoftBlue), contentAlignment = Alignment.Center) {
                Text("MM", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Money Manager", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Version 1.0.0", fontSize = 14.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(32.dp))
            Text("Built with Kotlin & Jetpack Compose.", fontSize = 14.sp, color = TextSecondary)
        }
    }
}