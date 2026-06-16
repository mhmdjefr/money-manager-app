package com.mhmdjefr.moneymanager.ui.statistic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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

val ChartColors = listOf(
    Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6),
    Color(0xFFFFD54F), Color(0xFFBA68C8), Color(0xFFFF8A65),
    Color(0xFF4DB6AC), Color(0xFFA1887F), Color(0xFF90A4AE)
)

@Composable
fun StatisticScreen(viewModel: DashboardViewModel) {
    val monthlyTransactions by viewModel.monthlyTransactions.collectAsState(initial = emptyList())
    val currentMonthCal by viewModel.currentMonth.collectAsState()
    val monthTitle = SimpleDateFormat("MMMM yyyy", Locale("en", "US")).format(currentMonthCal.time)

    // State buat milih mau liat chart Pengeluaran atau Pemasukan
    var selectedType by remember { mutableStateOf("Expense") }

    // Filter data sesuai tipe yang dipilih (Expense/Income)
    val filteredTransactions = monthlyTransactions.filter { it.type.equals(selectedType, ignoreCase = true) }
    val totalAmount = filteredTransactions.sumOf { it.amount }

    fun extractCategory(note: String?): String {
        val noteStr = note ?: ""
        return if (noteStr.startsWith("[")) {
            noteStr.substringAfter("[").substringBefore("]")
        } else {
            "Others"
        }
    }

    // Grouping berdasarkan kategori
    val groupedByCategory = filteredTransactions.groupBy { extractCategory(it.note) }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    val format = NumberFormat.getNumberInstance(Locale("id", "ID"))

    Scaffold(containerColor = LightBackground) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Statistics", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

            // --- 1. Navigator Bulan (Sama kayak Dashboard) ---
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", tint = TextPrimary)
                }
                Text(monthTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = TextPrimary)
                }
            }

            // --- 2. Toggle Pilihan (Income / Expense) ---
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardWhite)
                    .padding(4.dp)
            ) {
                listOf("Income", "Expense").forEach { type ->
                    val isSelected = selectedType == type
                    val bgColor = if (isSelected) SoftBlue else Color.Transparent
                    val textColor = if (isSelected) Color.White else TextSecondary

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(bgColor)
                            .clickable { selectedType = type }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = type, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (totalAmount == 0.0) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No $selectedType recorded this month.", color = TextSecondary)
                }
            } else {
                // --- 3. Donut Chart Dinamis ---
                Box(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(200.dp)) {
                        var startAngle = -90f
                        groupedByCategory.forEachIndexed { index, (_, amount) ->
                            val sweepAngle = (amount.toFloat() / totalAmount.toFloat()) * 360f
                            val color = ChartColors[index % ChartColors.size]

                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 50f, cap = StrokeCap.Butt)
                            )
                            startAngle += sweepAngle
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total $selectedType", fontSize = 12.sp, color = TextSecondary)
                        val mainColor = if (selectedType == "Income") IncomeGreen else ExpenseRed
                        Text("Rp ${format.format(totalAmount)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = mainColor)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text("Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(16.dp))

                // --- 4. List Kategori ---
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(groupedByCategory.size) { index ->
                        val (category, amount) = groupedByCategory[index]
                        val color = ChartColors[index % ChartColors.size]
                        val percentage = (amount / totalAmount) * 100

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(color))
                            Spacer(modifier = Modifier.width(12.dp))

                            Icon(getCategoryIcon(category), contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(category, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Rp ${format.format(amount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(String.format(Locale.US, "%.1f%%", percentage), fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}