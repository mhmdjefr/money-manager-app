package com.mhmdjefr.moneymanager.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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

fun getCategoryIcon(categoryName: String?): ImageVector {
    if (categoryName == null) return Icons.Default.Category
    val name = categoryName.lowercase()
    return when {
        name.contains("food") || name.contains("makan") || name.contains("minum") || name.contains("kopi") -> Icons.Default.Fastfood
        name.contains("transport") || name.contains("car") || name.contains("motor") || name.contains("bensin") -> Icons.Default.DirectionsCar
        name.contains("shop") || name.contains("belanja") || name.contains("gacha") -> Icons.Default.ShoppingCart
        name.contains("bill") || name.contains("tagihan") || name.contains("listrik") -> Icons.Default.Receipt
        name.contains("work") || name.contains("gaji") || name.contains("salary") -> Icons.Default.Work
        name.contains("gift") || name.contains("hadiah") || name.contains("bonus") -> Icons.Default.CardGiftcard
        name.contains("invest") || name.contains("saham") || name.contains("crypto") || name.contains("trending") -> Icons.Default.TrendingUp
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
    val accounts by viewModel.accounts.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()

    // 1. Ambil data yang udah difilter berdasarkan pencarian (Search Bar)
    val searchedTransactions by viewModel.filteredMonthlyTransactions.collectAsState(initial = emptyList())

    // 2. State untuk nyimpen filter dompet yang lagi aktif
    var selectedWalletId by remember { mutableStateOf<Int?>(null) }

    // 3. Terapin filter dompet ke data yang udah melewati Search Bar
    val finalFilteredTransactions = searchedTransactions.filter { tx ->
        selectedWalletId == null || tx.accountId == selectedWalletId || tx.targetAccountId == selectedWalletId
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("money_prefs", android.content.Context.MODE_PRIVATE) }
    val userName = sharedPrefs.getString("user_name", "User") ?: "User"
    val userAvatar = sharedPrefs.getString("user_avatar", "Person") ?: "Person"
    val format = NumberFormat.getNumberInstance(Locale("id", "ID"))
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
    val incomeColor = Color(0xFF5ED5A8)

    fun formatRp(amount: Double) = if (isBalanceVisible) "Rp ${format.format(amount)}" else "Rp ••••••••"

    val monthlyIncome = monthlyTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val monthlyExpense = monthlyTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    Scaffold(
        containerColor = LightBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {

            // --- HEADER SECTION ---
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Profil Greeting
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(SoftBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = com.mhmdjefr.moneymanager.ui.settings.getAvatarIcon(userAvatar),
                            contentDescription = null,
                            tint = SoftBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Welcome back,", color = TextSecondary, fontSize = 12.sp)
                        Text(userName, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Total Balance
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

                Spacer(modifier = Modifier.height(24.dp))

                // Summary Cards
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = incomeColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Income", color = Color.White, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
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
                            Text("Rp ${format.format(monthlyExpense)}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search transactions...", color = TextSecondary, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardWhite,
                        unfocusedContainerColor = CardWhite,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))
                Text("Recent Transactions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                // --- FILTER WALLET UI (LazyRow) ---
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Tombol "All"
                    item {
                        val isSelected = selectedWalletId == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) SoftBlue else CardWhite)
                                .border(1.dp, if (isSelected) SoftBlue else Color(0xFFE0E0E0), RoundedCornerShape(20.dp))
                                .clickable { selectedWalletId = null }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("All", color = if (isSelected) Color.White else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    // Tombol untuk masing-masing dompet
                    items(accounts) { acc ->
                        val isSelected = selectedWalletId == acc.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) SoftBlue else CardWhite)
                                .border(1.dp, if (isSelected) SoftBlue else Color(0xFFE0E0E0), RoundedCornerShape(20.dp))
                                .clickable { selectedWalletId = acc.id }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(acc.name, color = if (isSelected) Color.White else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- TRANSACTIONS LIST SECTION ---
            if (finalFilteredTransactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No transactions found."
                            else if (selectedWalletId != null) "No transactions for this wallet."
                            else "No transactions this month.",
                            color = TextSecondary
                        )
                    }
                }
            } else {
                // Logika Grouping Berdasarkan Tanggal
                val groupedTransactions = finalFilteredTransactions
                    .sortedByDescending { it.date }
                    .groupBy { tx ->
                        val txDate = java.util.Calendar.getInstance().apply { timeInMillis = tx.date }
                        val today = java.util.Calendar.getInstance()
                        val yesterday = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }

                        when {
                            txDate.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) && txDate.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR) -> "Today"
                            txDate.get(java.util.Calendar.YEAR) == yesterday.get(java.util.Calendar.YEAR) && txDate.get(java.util.Calendar.DAY_OF_YEAR) == yesterday.get(java.util.Calendar.DAY_OF_YEAR) -> "Yesterday"
                            else -> java.text.SimpleDateFormat("dd MMM yyyy", Locale.US).format(tx.date)
                        }
                    }

                // Render setiap kelompok tanggal beserta transaksinya
                groupedTransactions.forEach { (dateHeader, txList) ->
                    item {
                        Text(
                            text = dateHeader,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }

                    items(txList) { tx ->
                        val isIncome = tx.type == "INCOME"
                        val isTransfer = tx.type == "TRANSFER"

                        val txColor = if (isIncome) incomeColor else if (isTransfer) TextSecondary else ExpenseRed
                        val sign = if (isIncome) "+" else if (isTransfer) "" else "-"

                        val categoryName = if (isTransfer) "Transfer" else tx.note?.substringBefore("]")?.replace("[", "")?.trim()?.takeIf { it.isNotEmpty() } ?: "Others"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
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

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}