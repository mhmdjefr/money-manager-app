package com.mhmdjefr.moneymanager.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.material.icons.filled.*

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, onNavigateToEdit: (Int) -> Unit) {
    val totalBalance by viewModel.totalBalance.collectAsState(initial = 0.0)
    val monthlyTransactions by viewModel.monthlyTransactions.collectAsState(initial = emptyList())
    val currentMonth by viewModel.currentMonth.collectAsState()
    val isVisible by viewModel.isBalanceVisible.collectAsState()

    val format = NumberFormat.getNumberInstance(Locale("id", "ID"))
    fun formatRpHidden(amount: Double) = if (isVisible) "Rp ${format.format(amount)}" else "Rp ••••••••"
    fun formatRp(amount: Double) = "Rp ${format.format(amount)}"

    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)

    val totalIncome = monthlyTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = monthlyTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    Scaffold(containerColor = LightBackground) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp)) {

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Dashboard", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Total Balance", color = TextSecondary, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatRpHidden(totalBalance), color = TextPrimary, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.toggleBalanceVisibility() }) {
                    Icon(if (isVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, contentDescription = "Toggle Visibility", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.previousMonth() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Previous") }
                Text(text = monthFormat.format(currentMonth.time), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                IconButton(onClick = { viewModel.nextMonth() }) { Icon(Icons.Default.ArrowForward, contentDescription = "Next") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF5ED5A8))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Income", color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(formatRp(totalIncome), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF26868))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Expense", color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(formatRp(totalExpense), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Recent Transactions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(monthlyTransactions) { transaction ->
                    val isIncome = transaction.type == "INCOME"
                    val isTransfer = transaction.type == "TRANSFER"
                    val amountColor = when {
                        isIncome -> SoftBlue
                        isTransfer -> TextSecondary
                        else -> ExpenseRed
                    }
                    val amountPrefix = when {
                        isIncome -> "+"
                        isTransfer -> ""
                        else -> "-"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigateToEdit(transaction.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(amountColor.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val iconName = if (isTransfer) "Transfer" else transaction.note?.substringBefore("]")?.replace("[", "") ?: "Others"
                                Icon(getCategoryIcon(iconName), contentDescription = null, tint = amountColor)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val title = if (isTransfer) "Transfer" else transaction.note?.substringBefore("]")?.replace("[", "") ?: "Unknown"
                                val subtitle = if (isTransfer) transaction.note ?: "" else transaction.note?.substringAfter("]")?.trim() ?: ""

                                Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                if (subtitle.isNotEmpty()) {
                                    Text(subtitle, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                                }
                            }
                            Text("$amountPrefix Rp ${format.format(transaction.amount)}", color = amountColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

fun getCategoryIcon(categoryName: String): ImageVector {
    return when (categoryName) {
        "Salary" -> Icons.Default.Work
        "Food" -> Icons.Default.Fastfood
        "Transport" -> Icons.Default.DirectionsCar
        "Shopping" -> Icons.Default.ShoppingCart
        "Bills" -> Icons.Default.Receipt
        "Transfer" -> Icons.Default.SwapHoriz
        else -> Icons.Default.AttachMoney
    }
}