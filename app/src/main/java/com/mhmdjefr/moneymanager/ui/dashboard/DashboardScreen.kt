package com.mhmdjefr.moneymanager.ui.dashboard

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhmdjefr.moneymanager.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

// Helper function biar icon dinamis sesuai nama category
fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "Work" -> Icons.Default.Work
        "CardGiftcard" -> Icons.Default.CardGiftcard
        "TrendingUp" -> Icons.Default.TrendingUp
        "Fastfood" -> Icons.Default.Fastfood
        "DirectionsCar" -> Icons.Default.DirectionsCar
        "ShoppingCart" -> Icons.Default.ShoppingCart
        "Receipt" -> Icons.Default.Receipt
        else -> Icons.Default.Category
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel, onNavigateToEdit: (Int) -> Unit) {
    val currentMonth by viewModel.currentMonth.collectAsState()
    val isBalanceVisible by viewModel.isBalanceVisible.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState(initial = 0.0)
    val monthlyTransactions by viewModel.monthlyTransactions.collectAsState(initial = emptyList())

    val format = NumberFormat.getNumberInstance(Locale("id", "ID"))
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)

    // Warna hijau konsisten buat Income
    val incomeColor = Color(0xFF5ED5A8)

    // Fungsi format ini SEKARANG CUMA DIPAKAI BUAT TOTAL BALANCE
    fun formatRp(amount: Double) = if (isBalanceVisible) "Rp ${format.format(amount)}" else "Rp ••••••••"

    val monthlyIncome = monthlyTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val monthlyExpense = monthlyTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    Scaffold(
        containerColor = LightBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header - Total Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Balance", color = TextSecondary, fontSize = 14.sp)
                    Text(formatRp(totalBalance), color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { viewModel.toggleBalanceVisibility() }) {
                    Icon(
                        imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Balance",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Month Navigator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Prev") }
                Text(text = monthFormat.format(currentMonth.time), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                IconButton(onClick = { viewModel.nextMonth() }) { Icon(Icons.Default.ArrowForward, contentDescription = "Next") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Cards (Income & Expense)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = incomeColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Income", color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        // Nggak pakai formatRp lagi, langsung tembak angka aslinya
                        Text("Rp ${format.format(monthlyIncome)}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ExpenseRed)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Expense", color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        // Nggak pakai formatRp lagi, langsung tembak angka aslinya
                        Text("Rp ${format.format(monthlyExpense)}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Recent Transactions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            // Transactions List
            if (monthlyTransactions.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No transactions this month.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(monthlyTransactions.sortedByDescending { it.date }) { tx ->
                        val isIncome = tx.type == "INCOME"
                        val isTransfer = tx.type == "TRANSFER"

                        val txColor = if (isIncome) incomeColor else if (isTransfer) TextSecondary else ExpenseRed
                        val sign = if (isIncome) "+" else if (isTransfer) "" else "-"

                        val categoryName = if (isTransfer) "Transfer" else tx.note?.substringBefore("]")?.replace("[", "")?.trim()?.takeIf { it.isNotEmpty() } ?: "Others"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToEdit(tx.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardWhite)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(txColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(if (isTransfer) Icons.Default.SyncAlt else getCategoryIcon(categoryName), contentDescription = null, tint = txColor)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(categoryName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    if (!tx.note.isNullOrBlank() && !isTransfer) {
                                        val realNote = tx.note.substringAfter("]").trim()
                                        if (realNote.isNotEmpty()) {
                                            Text(realNote, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                                        }
                                    } else if (isTransfer) {
                                        Text(tx.note ?: "", color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                                    }
                                }
                                Text(
                                    text = "$sign Rp ${format.format(tx.amount)}",
                                    color = txColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}