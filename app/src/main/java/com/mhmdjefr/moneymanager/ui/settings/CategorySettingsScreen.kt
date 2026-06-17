package com.mhmdjefr.moneymanager.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mhmdjefr.moneymanager.ui.dashboard.DashboardViewModel
import com.mhmdjefr.moneymanager.ui.dashboard.getCategoryIcon
import com.mhmdjefr.moneymanager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySettingsScreen(viewModel: DashboardViewModel, onBackClick: () -> Unit) {
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf("EXPENSE") }
    var showDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var selectedIconName by remember { mutableStateOf("AttachMoney") }

    // Filter kategori sesuai tab yang diklik
    val filteredCategories = categories.filter { it.type == selectedTab }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { showDialog = true; inputName = ""; selectedIconName = "AttachMoney" }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category", tint = SoftBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        },
        containerColor = LightBackground
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp)) {

            // Tab Selector
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CardWhite).padding(4.dp)) {
                listOf("EXPENSE" to "Expense", "INCOME" to "Income").forEach { (typeKey, label) ->
                    val isSelected = selectedTab == typeKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) (if (typeKey == "EXPENSE") ExpenseRed else SoftBlue) else Color.Transparent)
                            .clickable { selectedTab = typeKey }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = label, color = if (isSelected) Color.White else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // List Kategori Dinamis
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredCategories) { category ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(if (selectedTab == "EXPENSE") ExpenseRed.copy(alpha = 0.1f) else SoftBlue.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(getCategoryIcon(category.iconName), contentDescription = null, tint = if (selectedTab == "EXPENSE") ExpenseRed else SoftBlue)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(category.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { viewModel.deleteCategory(category) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ExpenseRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Add New Category", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = inputName, onValueChange = { inputName = it }, label = { Text("Category Name") }, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDialog = false }) { Text("Cancel", color = TextSecondary) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (inputName.isNotBlank()) {
                                    viewModel.saveCategory(inputName, selectedTab, selectedIconName)
                                    showDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftBlue)
                        ) { Text("Save") }
                    }
                }
            }
        }
    }
}