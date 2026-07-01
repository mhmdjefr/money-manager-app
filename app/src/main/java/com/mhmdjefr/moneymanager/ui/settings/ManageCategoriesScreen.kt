package com.mhmdjefr.moneymanager.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mhmdjefr.moneymanager.data.local.CategoryEntity
import com.mhmdjefr.moneymanager.ui.common.ConfirmDeleteDialog
import com.mhmdjefr.moneymanager.ui.theme.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

val categoryIconsMap = mapOf(
    "Category" to Icons.Default.Category,
    "Fastfood" to Icons.Default.Fastfood,
    "LocalCafe" to Icons.Default.LocalCafe,
    "ShoppingCart" to Icons.Default.ShoppingCart,
    "Receipt" to Icons.Default.Receipt,
    "DirectionsCar" to Icons.Default.DirectionsCar,
    "Flight" to Icons.Default.Flight,
    "Work" to Icons.Default.Work,
    "Home" to Icons.Default.Home,
    "Favorite" to Icons.Default.Favorite,
    "CardGiftcard" to Icons.Default.CardGiftcard,
    "TrendingUp" to Icons.Default.TrendingUp,
    "School" to Icons.Default.School,
    "Pets" to Icons.Default.Pets,
    "Place" to Icons.Default.Place,
    "Info" to Icons.Default.Info,
    "Movie" to Icons.Default.Movie,
    "LocalHospital" to Icons.Default.LocalHospital,
    "Hotel" to Icons.Default.Hotel,
    "Celebration" to Icons.Default.Celebration,
    "MoreHoriz" to Icons.Default.MoreHoriz,
    "LocalGroceryStore" to Icons.Default.LocalGroceryStore,
    "Subscriptions" to Icons.Default.Subscriptions,
    "Spa" to Icons.Default.Spa,
    "Shield" to Icons.Default.Shield,
    "VolunteerActivism" to Icons.Default.VolunteerActivism,
    "Wifi" to Icons.Default.Wifi,
    "LocalGasStation" to Icons.Default.LocalGasStation,
    "Build" to Icons.Default.Build,
    "AccountBalance" to Icons.Default.AccountBalance,
    "ChildCare" to Icons.Default.ChildCare,
    "LocalLaundryService" to Icons.Default.LocalLaundryService,
    "LocalParking" to Icons.Default.LocalParking,
    "Redeem" to Icons.Default.Redeem
)

fun getCategoryIcon(iconName: String?): ImageVector {
    return categoryIconsMap[iconName] ?: Icons.Default.Category
}

private class AutoScrollState {
    var listBoundsTop by mutableStateOf(0f)
    var listBoundsBottom by mutableStateOf(0f)
    var pointerYOnScreen by mutableStateOf<Float?>(null)
    var isDragging by mutableStateOf(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(viewModel: ManageCategoriesViewModel, onBackClick: () -> Unit) {
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val allBudgets by viewModel.allBudgets.collectAsState(initial = emptyList())
    var selectedTab by remember { mutableStateOf("EXPENSE") }

    var showDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryForBudget by remember { mutableStateOf<CategoryEntity?>(null) }
    var inputBudgetAmount by remember { mutableStateOf("") }
    var inputName by remember { mutableStateOf("") }
    var selectedIconName by remember { mutableStateOf("Category") }

    val baseColor = if (selectedTab == "EXPENSE") ExpenseRed else Color(0xFF5ED5A8)

    val mutableCategories = remember(categories, selectedTab) {
        categories.filter { it.type == selectedTab }.sortedBy { it.orderIndex }.toMutableStateList()
    }

    val listState = rememberLazyListState()
    val autoScrollState = remember { AutoScrollState() }
    val density = LocalDensity.current

    LaunchedEffect(autoScrollState.isDragging) {
        if (!autoScrollState.isDragging) return@LaunchedEffect

        while (isActive && autoScrollState.isDragging) {
            val pointerY = autoScrollState.pointerYOnScreen
            if (pointerY != null) {
                val edgeThreshold = with(density) { 80.dp.toPx() }
                val topEdge = autoScrollState.listBoundsTop + edgeThreshold
                val bottomEdge = autoScrollState.listBoundsBottom - edgeThreshold

                when {
                    pointerY < topEdge -> {
                        val distance = (topEdge - pointerY).coerceAtMost(edgeThreshold)
                        val speed = (distance / edgeThreshold) * 18f
                        listState.scrollBy(-speed)
                    }
                    pointerY > bottomEdge -> {
                        val distance = (pointerY - bottomEdge).coerceAtMost(edgeThreshold)
                        val speed = (distance / edgeThreshold) * 18f
                        listState.scrollBy(speed)
                    }
                }
            }
            delay(16L)
        }
    }

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
                    selectedIconName = "Category"
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

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Hold and drag to reorder",
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (mutableCategories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No categories yet.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInRoot()
                            autoScrollState.listBoundsTop = pos.y
                            autoScrollState.listBoundsBottom = pos.y + coords.size.height
                        }
                ) {
                    itemsIndexed(mutableCategories, key = { _, cat -> cat.id }) { _, cat ->
                        CategoryCard(
                            category = cat,
                            baseColor = baseColor,
                            hasBudget = allBudgets.any { it.categoryId == cat.id },
                            list = mutableCategories,
                            autoScrollState = autoScrollState,
                            onReorder = { from, to ->
                                val item = mutableCategories.removeAt(from)
                                mutableCategories.add(to, item)
                            },
                            onDragEnd = { viewModel.updateCategoriesOrder(mutableCategories) },
                            onSetBudget = {
                                categoryForBudget = cat
                                inputBudgetAmount = ""
                            },
                            onEdit = {
                                editingCategory = cat
                                inputName = cat.name
                                selectedIconName = cat.iconName
                                showDialog = true
                            },
                            onDelete = { categoryToDelete = cat }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingCategory == null) "Add Category" else "Edit Category", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select Icon", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categoryIconsMap.keys.toList()) { iconName ->
                            val isSelected = selectedIconName == iconName
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) baseColor.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(2.dp, if (isSelected) baseColor else Color.Transparent, CircleShape)
                                    .clickable { selectedIconName = iconName },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = categoryIconsMap[iconName]!!,
                                    contentDescription = null,
                                    tint = if (isSelected) baseColor else TextSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isNotBlank()) {
                            val newCat = CategoryEntity(
                                id = editingCategory?.id ?: 0,
                                name = inputName.trim(),
                                type = selectedTab,
                                iconName = selectedIconName,
                                orderIndex = editingCategory?.orderIndex ?: 0
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

    categoryToDelete?.let { cat ->
        ConfirmDeleteDialog(
            message = "Category \"${cat.name}\" will be permanently deleted. Transactions using this category won't be deleted, but the category label may no longer display correctly.",
            onConfirm = {
                viewModel.deleteCategory(cat)
                categoryToDelete = null
            },
            onDismiss = { categoryToDelete = null }
        )
    }

    categoryForBudget?.let { cat ->
        Dialog(onDismissRequest = { categoryForBudget = null }) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Set Budget", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Monthly limit for \"${cat.name}\"", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = inputBudgetAmount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) inputBudgetAmount = it },
                        label = { Text("Monthly Limit (Rp)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        visualTransformation = com.mhmdjefr.moneymanager.ui.wallet.RupiahVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            viewModel.deleteBudget(cat.id)
                            categoryForBudget = null
                        }) { Text("Remove Budget", color = ExpenseRed) }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { categoryForBudget = null }) { Text("Cancel", color = TextSecondary) }
                        Button(
                            onClick = {
                                val amount = inputBudgetAmount.toDoubleOrNull()
                                if (amount != null && amount > 0) {
                                    viewModel.saveBudget(cat.id, amount)
                                    categoryForBudget = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D))
                        ) { Text("Save") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: CategoryEntity,
    baseColor: Color,
    hasBudget: Boolean,
    list: List<CategoryEntity>,
    autoScrollState: Any,
    onReorder: (from: Int, to: Int) -> Unit,
    onDragEnd: () -> Unit,
    onSetBudget: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var dragFromIndex by remember { mutableStateOf(-1) }
    var accumulatedDragY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var cardPositionY by remember { mutableStateOf(0f) }

    val elevation by animateFloatAsState(
        targetValue = if (isDragging) 12f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "categoryCardElevation"
    )
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "categoryCardScale"
    )

    val scrollState = autoScrollState as AutoScrollState

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation = elevation.dp, shape = RoundedCornerShape(16.dp), clip = false)
            .onGloballyPositioned { coords ->
                cardPositionY = coords.positionInRoot().y
            }
            .pointerInput(category.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        dragFromIndex = list.indexOf(category)
                        accumulatedDragY = 0f
                        isDragging = true
                        scrollState.isDragging = true
                        scrollState.pointerYOnScreen = cardPositionY + offset.y
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDragY += dragAmount.y
                        scrollState.pointerYOnScreen = cardPositionY + change.position.y

                        if (accumulatedDragY > 120f && dragFromIndex < list.size - 1) {
                            val targetIndex = dragFromIndex + 1
                            onReorder(dragFromIndex, targetIndex)
                            dragFromIndex = targetIndex
                            accumulatedDragY = 0f
                        } else if (accumulatedDragY < -120f && dragFromIndex > 0) {
                            val targetIndex = dragFromIndex - 1
                            onReorder(dragFromIndex, targetIndex)
                            dragFromIndex = targetIndex
                            accumulatedDragY = 0f
                        }
                    },
                    onDragEnd = {
                        onDragEnd()
                        dragFromIndex = -1
                        isDragging = false
                        scrollState.isDragging = false
                        scrollState.pointerYOnScreen = null
                    },
                    onDragCancel = {
                        dragFromIndex = -1
                        isDragging = false
                        scrollState.isDragging = false
                        scrollState.pointerYOnScreen = null
                    }
                )
            },
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
                Icon(getCategoryIcon(category.iconName), contentDescription = null, tint = baseColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(category.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

            if (category.type == "EXPENSE") {
                IconButton(onClick = onSetBudget) {
                    if (hasBudget) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFB74D)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Savings, contentDescription = "Budget set", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Icon(Icons.Default.Savings, contentDescription = "Set Budget", tint = TextSecondary.copy(alpha = 0.4f))
                    }
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SoftBlue)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed)
            }

            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.Menu, contentDescription = "Drag indicator", tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        }
    }
}
