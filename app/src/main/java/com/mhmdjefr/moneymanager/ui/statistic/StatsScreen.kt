package com.mhmdjefr.moneymanager.ui.statistic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
fun StatsScreen(viewModel: DashboardViewModel) {
    val transactions by viewModel.monthlyTransactions.collectAsState(initial = emptyList())
    val currentMonth by viewModel.currentMonth.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf("INCOME") }

    val filteredTransactions = transactions.filter { it.type == selectedTab }
    val totalAmount = filteredTransactions.sumOf { it.amount }

    val categoryTotals = filteredTransactions.groupBy { tx ->
        tx.note?.substringBefore("]")?.replace("[", "")?.trim()?.takeIf { it.isNotBlank() } ?: "Others"
    }.mapValues { entry ->
        entry.value.sumOf { it.amount }
    }.toList().sortedByDescending { it.second }

    val formatRp = NumberFormat.getNumberInstance(Locale("id", "ID"))
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