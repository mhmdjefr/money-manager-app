package com.mhmdjefr.moneymanager.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhmdjefr.moneymanager.data.local.CategoryEntity
import com.mhmdjefr.moneymanager.ui.dashboard.getCategoryIcon
import com.mhmdjefr.moneymanager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(viewModel: ManageCategoriesViewModel, onBackClick: () -> Unit) {
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    var selectedTab by remember { mutableStateOf("EXPENSE") }

    // Dialog State
    var showDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var inputName by remember { mutableStateOf("") }

    val baseColor = if (selectedTab == "EXPENSE") ExpenseRed else Color(0xFF5ED5A8)
    val currentCategories = categories.filter { it.type == selectedTab }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCategory = null
                    inputName = ""
                    showDialog = true
                },
                containerColor = baseColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        },
        containerColor = LightBackground
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp)) {

            // TABS
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CardWhite).padding(4.dp)
            ) {
                listOf("EXPENSE" to "Expense", "INCOME" to "Income").forEach { (typeKey, label) ->
                    val isSelected = selectedTab == typeKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) baseColor else Color.Transparent)
                            .clickable { selectedTab = typeKey }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = label, color = if (isSelected) Color.White else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // LIST KATEGORI
            if (currentCategories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No categories yet.", color = TextSecondary)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(currentCategories) { cat ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardWhite)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(baseColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(getCategoryIcon(cat.name), contentDescription = null, tint = baseColor)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(cat.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

                                // Action Buttons
                                IconButton(onClick = {
                                    editingCategory = cat
                                    inputName = cat.name
                                    showDialog = true
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SoftBlue) }

                                IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG ADD/EDIT KATEGORI
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingCategory == null) "Add Category" else "Edit Category", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isNotBlank()) {
                            val newCat = CategoryEntity(
                                id = editingCategory?.id ?: 0,
                                name = inputName.trim(),
                                type = selectedTab,
                                iconName = editingCategory?.iconName ?: "Category" // <-- Tambahin baris ini bro
                            )
                            viewModel.saveCategory(newCat)
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = baseColor)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = CardWhite
        )
    }
}