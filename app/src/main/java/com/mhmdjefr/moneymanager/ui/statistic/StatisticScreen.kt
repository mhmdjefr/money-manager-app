package com.mhmdjefr.moneymanager.ui.statistic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhmdjefr.moneymanager.ui.dashboard.DashboardViewModel
import com.mhmdjefr.moneymanager.ui.dashboard.getCategoryIcon
import com.mhmdjefr.moneymanager.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticScreen(viewModel: DashboardViewModel) {
    val monthlyTransactions by viewModel.monthlyTransactions.collectAsState(initial = emptyList())
    val currentMonth by viewModel.currentMonth.collectAsState()

    var selectedTab by remember { mutableStateOf("EXPENSE") }
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
    val format = NumberFormat.getNumberInstance(Locale("id", "ID"))

    val filteredTx = monthlyTransactions.filter { it.type == selectedTab }
    val totalAmount = filteredTx.sumOf { it.amount }

    val categoryTotals = filteredTx.groupBy { tx ->
        tx.note?.substringBefore("]")?.replace("[", "")?.trim()?.takeIf { it.isNotEmpty() } ?: "Others"
    }.mapValues { it.value.sumOf { tx -> tx.amount } }
        .toList()
        .sortedByDescending { it.second }

    // Palet Warna Terpisah: Panas buat Expense, Dingin buat Income
    val expenseColors = listOf(
        Color(0xFFF26868), // Merah Utama (ExpenseRed)
        Color(0xFFE67E22), // Oranye
        Color(0xFFF2C94C), // Kuning
        Color(0xFFA569BD), // Ungu
        Color(0xFFD98880), // Merah Pudar
        Color(0xFFF39C12)  // Oranye Gelap
    )

    val incomeColors = listOf(
        Color(0xFF5ED5A8), // Hijau Utama (Income)
        Color(0xFF5D9FE6), // Biru (SoftBlue)
        Color(0xFF1ABC9C), // Cyan Terang
        Color(0xFF2E86C1), // Biru Gelap
        Color(0xFF7DCEA0), // Hijau Pudar
        Color(0xFF85C1E9)  // Biru Muda
    )

    // Tentukan palet yang aktif berdasarkan tab
    val activeColors = if (selectedTab == "EXPENSE") expenseColors else incomeColors

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        },
        containerColor = LightBackground
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp)) {

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

            // Tab Selector
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CardWhite).padding(4.dp)) {
                listOf("EXPENSE" to "Expense", "INCOME" to "Income").forEach { (typeKey, label) ->
                    val isSelected = selectedTab == typeKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) (if (typeKey == "EXPENSE") ExpenseRed else Color(0xFF5ED5A8)) else Color.Transparent)
                            .clickable { selectedTab = typeKey }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = label, color = if (isSelected) Color.White else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Donut Chart & List
            if (categoryTotals.isEmpty() || totalAmount == 0.0) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No transactions this month.", color = TextSecondary)
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(180.dp)) {
                        var startAngle = -90f
                        categoryTotals.forEachIndexed { index, (_, amount) ->
                            val sweepAngle = (amount / totalAmount).toFloat() * 360f
                            val sliceColor = activeColors[index % activeColors.size]

                            drawArc(
                                color = sliceColor,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 60f, cap = StrokeCap.Round)
                            )
                            startAngle += sweepAngle
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total ${if (selectedTab == "EXPENSE") "Expense" else "Income"}", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            text = "Rp ${format.format(totalAmount)}",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text("Category Breakdown", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.weight(1f)) {
                    itemsIndexed(categoryTotals) { index, (categoryName, amount) ->
                        val percentage = if (totalAmount > 0) (amount / totalAmount).toFloat() else 0f
                        val displayPercent = (percentage * 100).toInt()

                        val itemColor = activeColors[index % activeColors.size]

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(itemColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(getCategoryIcon(categoryName), contentDescription = null, tint = itemColor)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(categoryName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    Text("Rp ${format.format(amount)}", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LinearProgressIndicator(
                                        progress = { percentage },
                                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                                        color = itemColor,
                                        trackColor = LightBackground,
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("$displayPercent%", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(36.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}