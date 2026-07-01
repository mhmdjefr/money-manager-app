package com.mhmdjefr.moneymanager.ui.statistic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhmdjefr.moneymanager.ui.dashboard.DashboardViewModel
import com.mhmdjefr.moneymanager.ui.settings.getCategoryIcon
import com.mhmdjefr.moneymanager.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun StatsScreen(viewModel: DashboardViewModel, onNavigateToCategories: () -> Unit = {}) {
    val rawTransactions by viewModel.monthlyTransactions.collectAsState(initial = emptyList())
    val rawPreviousMonthTransactions by viewModel.previousMonthTransactions.collectAsState(initial = emptyList())
    val currentMonth by viewModel.currentMonth.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState(initial = emptyList())
    val accounts by viewModel.accounts.collectAsState(initial = emptyList())
    val budgetProgressList by viewModel.budgetProgressList.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf("INCOME") }
    var selectedWalletId by remember { mutableStateOf<Int?>(null) }

    // Reset filter wallet setiap kali bulan aktif berubah
    LaunchedEffect(currentMonth) {
        selectedWalletId = null
    }

    val transactions = rawTransactions.filter { selectedWalletId == null || it.accountId == selectedWalletId }
    val previousMonthTransactions = rawPreviousMonthTransactions.filter { selectedWalletId == null || it.accountId == selectedWalletId }

    val filteredTransactions = remember(transactions, selectedTab) {
        transactions.filter { it.type == selectedTab }
    }
    val totalAmount = remember(filteredTransactions) { filteredTransactions.sumOf { it.amount } }

    // Perbandingan dengan bulan sebelumnya
    val previousTotalAmount = previousMonthTransactions.filter { it.type == selectedTab }.sumOf { it.amount }
    val diffAmount = totalAmount - previousTotalAmount
    val diffPercentage = if (previousTotalAmount > 0) (diffAmount / previousTotalAmount) * 100 else null

    val categoryTotals = remember(filteredTransactions) {
        filteredTransactions.groupBy { tx ->
            tx.note?.substringBefore("]")?.replace("[", "")?.trim()?.takeIf { it.isNotBlank() } ?: "Others"
        }.mapValues { entry ->
            entry.value.sumOf { it.amount }
        }.toList().sortedByDescending { it.second }
    }

    val formatRp = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)

    val baseColor = if (selectedTab == "INCOME") Color(0xFF5ED5A8) else ExpenseRed

    // Palet warna pelangi yang vibrant dan kontras tinggi
    val poolColors = listOf(
        baseColor,
        Color(0xFF5A92E6), // Biru Terang
        Color(0xFFFFB74D), // Kuning Amber
        Color(0xFFBA68C8), // Ungu
        Color(0xFFFF7043), // Oranye
        Color(0xFF4DB6AC), // Tosca
        Color(0xFFF06292), // Pink
        Color(0xFF95A5A6), // Abu-abu
        Color(0xFF2ECC71)  // Hijau Emerald
    )

    // SOLUSI DINAMIS: Membuat map warna otomatis berdasarkan kategori yang aktif saat itu
    val categoryColorMap = remember(categoryTotals, selectedTab) {
        categoryTotals.mapIndexed { index, pair ->
            pair.first to poolColors[index % poolColors.size]
        }.toMap()
    }

    // ANIMASI STATE UNTUK DONUT CHART
    val donutAnimationProgress = remember { Animatable(0f) }
    LaunchedEffect(categoryTotals, selectedTab, currentMonth) {
        donutAnimationProgress.snapTo(0f)
        donutAnimationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }

    Scaffold(
        containerColor = LightBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Statistics", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(24.dp))

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

                // --- FILTER WALLET ---
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        val isSelected = selectedWalletId == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) baseColor else CardWhite)
                                .border(1.dp, if (isSelected) baseColor else Color(0xFFE0E0E0), RoundedCornerShape(20.dp))
                                .clickable { selectedWalletId = null }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("All", color = if (isSelected) Color.White else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    items(accounts) { acc ->
                        val isSelected = selectedWalletId == acc.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) baseColor else CardWhite)
                                .border(1.dp, if (isSelected) baseColor else Color(0xFFE0E0E0), RoundedCornerShape(20.dp))
                                .clickable { selectedWalletId = acc.id }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(acc.name, color = if (isSelected) Color.White else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- BUDGET STATUS ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Budget Status", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (budgetProgressList.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onNavigateToCategories() }
                                    .background(Color(0xFFFFB74D).copy(alpha = 0.1f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFFFB74D).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Savings, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Set your first budget", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Track spending limits per category", color = TextSecondary, fontSize = 11.sp)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                            }
                        } else {
                            budgetProgressList.forEach { budget ->
                                val progressColor = when {
                                    budget.percentage >= 1f -> ExpenseRed
                                    budget.percentage >= 0.8f -> Color(0xFFFFB74D)
                                    else -> Color(0xFF5ED5A8)
                                }
                                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(budget.categoryName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text(
                                            "Rp ${formatRp.format(budget.spentAmount)} / Rp ${formatRp.format(budget.limitAmount)}",
                                            color = TextSecondary, fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { budget.percentage.coerceAtMost(1f) },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = progressColor,
                                        trackColor = LightBackground
                                    )
                                    if (budget.isOverBudget) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Over budget!", color = ExpenseRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

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

                Spacer(modifier = Modifier.height(32.dp))
            }

            if (totalAmount == 0.0) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No $selectedTab this month.", color = TextSecondary, fontSize = 16.sp)
                    }
                }
            } else {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(200.dp)) {
                            var startAngle = -90f
                            categoryTotals.forEachIndexed { index, pair ->
                                val targetSweep = (pair.second / totalAmount).toFloat() * 360f
                                // Kalikan dengan animasi biar muter pelan-pelan
                                val sweepAngle = targetSweep * donutAnimationProgress.value

                                val color = categoryColorMap[pair.first] ?: baseColor

                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 45f, cap = StrokeCap.Round),
                                    size = Size(size.width, size.height),
                                    topLeft = Offset(0f, 0f)
                                )
                                startAngle += targetSweep
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (selectedTab == "INCOME") "Total Income" else "Total Expense", color = TextSecondary, fontSize = 12.sp)
                            Text("Rp ${formatRp.format(totalAmount)}", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Badge perbandingan dengan bulan sebelumnya
                    if (diffPercentage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val isIncrease = diffAmount >= 0
                        // Untuk EXPENSE, kenaikan itu "buruk" (merah); untuk INCOME, kenaikan itu "baik" (hijau)
                        val trendColor = if (selectedTab == "EXPENSE") {
                            if (isIncrease) ExpenseRed else Color(0xFF5ED5A8)
                        } else {
                            if (isIncrease) Color(0xFF5ED5A8) else ExpenseRed
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(trendColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isIncrease) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = trendColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${kotlin.math.abs(diffPercentage).toInt()}% • ${if (isIncrease) "+" else "-"}Rp ${formatRp.format(kotlin.math.abs(diffAmount))} vs last month",
                                        color = trendColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Category Breakdown", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                itemsIndexed(categoryTotals) { index, pair ->
                    val (category, amount) = pair
                    val percentage = (amount / totalAmount).toFloat()
                    val color = categoryColorMap[category] ?: baseColor
                    val categoryEntity = allCategories.find { it.name == category }

                    // ANIMASI STATE UNTUK PROGRESS BAR
                    val animatedProgress by animateFloatAsState(
                        targetValue = percentage,
                        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                        label = "BarAnimation"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(getCategoryIcon(categoryEntity?.iconName), contentDescription = null, tint = color)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(category, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("Rp ${formatRp.format(amount)}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                LinearProgressIndicator(
                                    progress = { animatedProgress }, // Pakai nilai yang udah dianimasiin
                                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = color,
                                    trackColor = CardWhite,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("• ${(percentage * 100).toInt()}%", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}