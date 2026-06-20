package com.mhmdjefr.moneymanager.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.mhmdjefr.moneymanager.ui.dashboard.DashboardViewModel
import com.mhmdjefr.moneymanager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: DashboardViewModel, onNavigate: (String) -> Unit) {
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/comma-separated-values")
    ) { uri ->
        uri?.let {
            viewModel.exportToCsv(context, it,
                onSuccess = { Toast.makeText(context, "Data successfully exported!", Toast.LENGTH_SHORT).show() },
                onError = { Toast.makeText(context, "Export failed!", Toast.LENGTH_SHORT).show() }
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importFromCsv(context, it,
                onSuccess = { Toast.makeText(context, "Data successfully imported!", Toast.LENGTH_SHORT).show() },
                onError = { Toast.makeText(context, "Import failed! Check format.", Toast.LENGTH_SHORT).show() }
            )
        }
    }

    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = LightBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(24.dp))

            // General Section
            Text("General", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingItem(icon = Icons.Default.Category, title = "Manage Categories", onClick = { onNavigate("manage_categories") })
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Account Section
            Text("Account", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingItem(icon = Icons.Default.Person, title = "My Profile", onClick = { onNavigate("profile") })
                    HorizontalDivider(color = LightBackground)
                    SettingItem(icon = Icons.Default.Lock, title = "Privacy", onClick = { onNavigate("privacy") })
                    HorizontalDivider(color = LightBackground)
                    SettingItem(icon = Icons.Default.Info, title = "About", onClick = { onNavigate("about") })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Data Management Section
            Text("Data Management", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingItem(
                        icon = Icons.Default.UploadFile,
                        title = "Export to CSV (Backup)",
                        onClick = { exportLauncher.launch("money_manager_backup.csv") }
                    )
                    HorizontalDivider(color = LightBackground)
                    SettingItem(
                        icon = Icons.Default.DownloadForOffline,
                        title = "Import from CSV (Restore)",
                        onClick = { importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv")) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Danger Zone Section (Reset Data)
            Text("Danger Zone", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showResetDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ExpenseRed.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = ExpenseRed)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Reset All Data", color = ExpenseRed, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // DIALOG KONFIRMASI RESET
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Data?", fontWeight = FontWeight.Bold) },
            text = { Text("This action will permanently delete all wallets and transaction history. This data cannot be recovered.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetApplicationData(context)
                        showResetDialog = false
                        Toast.makeText(context, "All data has been reset successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Yes, Delete All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardWhite
        )
    }
}

@Composable
fun SettingItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SoftBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = SoftBlue)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
    }
}