package com.mhmdjefr.moneymanager.ui.wallet

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mhmdjefr.moneymanager.data.local.AccountEntity
import com.mhmdjefr.moneymanager.ui.common.ConfirmDeleteDialog
import com.mhmdjefr.moneymanager.ui.dashboard.DashboardViewModel
import com.mhmdjefr.moneymanager.ui.onboarding.OnboardingTooltip
import com.mhmdjefr.moneymanager.ui.onboarding.TutorialReplayButton
import com.mhmdjefr.moneymanager.ui.onboarding.rememberOnboardingState
import com.mhmdjefr.moneymanager.ui.theme.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.NumberFormat
import java.util.Locale

// State kecil untuk auto-scroll: dipakai bersama oleh semua WalletCard dan
// LazyColumn induk, supaya saat drag mendekati tepi viewport, list otomatis scroll.
private class AutoScrollState {
    var listBoundsTop by mutableStateOf(0f)
    var listBoundsBottom by mutableStateOf(0f)
    var pointerYOnScreen by mutableStateOf<Float?>(null)
    var isDragging by mutableStateOf(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(viewModel: DashboardViewModel) {
    val accounts by viewModel.accounts.collectAsState(initial = emptyList())
    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    val isVisible by viewModel.isBalanceVisible.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var inputInitialBalance by remember { mutableStateOf("") }
    var inputType by remember { mutableStateOf("REGULAR") }
    var inputInclude by remember { mutableStateOf(true) }

    val format = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
    fun formatRp(amount: Double) = if (isVisible) "Rp ${format.format(amount)}" else "Rp ••••••••"

    // Pre-compute balance semua akun sekali jalan (O(n)), bukan filter berulang
    // per akun (O(n x m)) yang sebelumnya dipanggil ulang juga di tiap WalletCard.
    val accountBalances = remember(accounts, transactions) {
        val incomeByAccount = transactions.filter { it.type == "INCOME" }.groupBy { it.accountId }
        val expenseByAccount = transactions.filter { it.type == "EXPENSE" }.groupBy { it.accountId }
        val transferOutByAccount = transactions.filter { it.type == "TRANSFER" }.groupBy { it.accountId }
        val transferInByAccount = transactions.filter { it.type == "TRANSFER" }.groupBy { it.targetAccountId }

        accounts.associate { account ->
            val income = incomeByAccount[account.id]?.sumOf { it.amount } ?: 0.0
            val expense = expenseByAccount[account.id]?.sumOf { it.amount } ?: 0.0
            val transferOut = transferOutByAccount[account.id]?.sumOf { it.amount } ?: 0.0
            val transferIn = transferInByAccount[account.id]?.sumOf { it.amount } ?: 0.0
            account.id to (account.initialBalance + income - expense - transferOut + transferIn)
        }
    }

    var totalAssets = 0.0
    var totalLiabilities = 0.0

    accounts.forEach { account ->
        val currentBalance = accountBalances[account.id] ?: account.initialBalance
        if (account.includeInTotal) {
            if (account.type == "REGULAR") totalAssets += currentBalance
            else totalLiabilities += currentBalance
        }
    }
    val netWorth = totalAssets + totalLiabilities

    val mutableAssets = remember(accounts) {
        accounts.filter { it.type == "REGULAR" }.sortedBy { it.orderIndex }.toMutableStateList()
    }
    val mutableLiabilities = remember(accounts) {
        accounts.filter { it.type == "LIABILITY" }.sortedBy { it.orderIndex }.toMutableStateList()
    }

    fun persistOrder() {
        val combined = mutableAssets + mutableLiabilities
        viewModel.updateAccountsOrder(combined)
    }

    val sectionTooltip = rememberOnboardingState("wallet_assets_liability")
    val dragTooltip = rememberOnboardingState("wallet_drag_reorder")

    val listState = rememberLazyListState()
    val autoScrollState = remember { AutoScrollState() }
    val density = LocalDensity.current

    // Loop auto-scroll HANYA dibuat selama isDragging true. LaunchedEffect akan
    // otomatis membatalkan coroutine lama dan membuat yang baru setiap kali key
    // (isDragging) berubah -- jadi saat drag berhenti, tidak ada loop polling
    // yang terus berjalan di background.
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

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(if (selectedAccount == null) "Add New Wallet" else "Edit Wallet", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = inputName, onValueChange = { inputName = it }, label = { Text("Wallet Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputInitialBalance, onValueChange = { if (it.all { char -> char.isDigit() }) inputInitialBalance = it },
                        label = { Text(if (inputType == "LIABILITY") "Current Debt (Rp)" else "Initial Balance (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = RupiahVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(LightBackground).padding(4.dp)) {
                        listOf("REGULAR" to "Asset", "LIABILITY" to "Liability").forEach { (typeKey, label) ->
                            val isSelected = inputType == typeKey
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (isSelected) SoftBlue else Color.Transparent).clickable { inputType = typeKey }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(text = label, color = if (isSelected) Color.White else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Include in Total Assets", fontSize = 14.sp)
                        Switch(checked = inputInclude, onCheckedChange = { inputInclude = it }, colors = SwitchDefaults.colors(checkedThumbColor = SoftBlue, checkedTrackColor = SoftBlue.copy(alpha = 0.5f)))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        if (selectedAccount != null) {
                            TextButton(onClick = { showDialog = false; showDeleteAccountDialog = true }) { Text("Delete", color = ExpenseRed) }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { showDialog = false }) { Text("Cancel", color = TextSecondary) }
                        Button(
                            onClick = {
                                if (inputName.isNotBlank()) {
                                    val id = selectedAccount?.id ?: 0
                                    val rawBalance = inputInitialBalance.replace(".", "").toDoubleOrNull() ?: 0.0
                                    val finalBalance = if (inputType == "LIABILITY" && rawBalance > 0) -rawBalance else rawBalance
                                    val order = selectedAccount?.orderIndex ?: (accounts.maxOfOrNull { it.orderIndex } ?: 0) + 1
                                    viewModel.saveAccount(id, inputName, finalBalance, inputType, inputInclude, order)
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

    if (showDeleteAccountDialog) {
        ConfirmDeleteDialog(
            message = "This wallet and its balance reference will be permanently deleted. Transactions linked to it won't be deleted automatically.",
            onConfirm = {
                selectedAccount?.let { viewModel.deleteAccount(it) }
                showDeleteAccountDialog = false
            },
            onDismiss = { showDeleteAccountDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallets", fontWeight = FontWeight.Bold) },
                actions = {
                    TutorialReplayButton(onClick = {
                        sectionTooltip.show()
                    })
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        selectedAccount = null; inputName = ""; inputInitialBalance = ""; inputType = "REGULAR"; inputInclude = true; showDialog = true
                    }) { Icon(Icons.Default.Add, contentDescription = "Add", tint = SoftBlue) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        },
        containerColor = LightBackground
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp)) {

            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("Net Worth", color = Color.LightGray, fontSize = 14.sp)
                            Text(formatRp(netWorth), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = { viewModel.toggleBalanceVisibility() },
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Icon(if (isVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, contentDescription = null, tint = Color.LightGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Assets", color = Color.LightGray, fontSize = 12.sp)
                            Text(formatRp(totalAssets), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Liabilities", color = Color.LightGray, fontSize = 12.sp)
                            Text(formatRp(totalLiabilities), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

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
                if (mutableAssets.isNotEmpty()) {
                    item {
                        Column {
                            SectionHeader(title = "Assets", icon = Icons.Default.TrendingUp, tint = SoftBlue)
                            OnboardingTooltip(
                                visible = sectionTooltip.isVisible,
                                message = "Assets and Liabilities are separated here so it is clear what you own versus what you owe.",
                                onDismiss = {
                                    sectionTooltip.dismiss()
                                    dragTooltip.show()
                                },
                                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                            )
                        }
                    }
                    itemsIndexed(mutableAssets, key = { _, account -> "asset_${account.id}" }) { index, account ->
                        Column {
                            WalletCard(
                                account = account,
                                currentBalance = accountBalances[account.id] ?: account.initialBalance,
                                formatRp = ::formatRp,
                                onClick = {
                                    selectedAccount = account
                                    inputName = account.name
                                    inputInitialBalance = if (account.initialBalance < 0) (account.initialBalance * -1).toLong().toString() else account.initialBalance.toLong().toString()
                                    inputType = account.type
                                    inputInclude = account.includeInTotal
                                    showDialog = true
                                },
                                onReorder = { from, to ->
                                    val item = mutableAssets.removeAt(from)
                                    mutableAssets.add(to, item)
                                },
                                onDragEnd = { persistOrder() },
                                list = mutableAssets,
                                autoScrollState = autoScrollState
                            )
                            if (index == 0) {
                                OnboardingTooltip(
                                    visible = dragTooltip.isVisible,
                                    message = "Long-press and drag up or down to reorder your wallets.",
                                    onDismiss = { dragTooltip.dismiss() },
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }

                if (mutableLiabilities.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(title = "Liability", icon = Icons.Default.TrendingDown, tint = ExpenseRed)
                    }
                    itemsIndexed(mutableLiabilities, key = { _, account -> "liability_${account.id}" }) { _, account ->
                        WalletCard(
                            account = account,
                            currentBalance = accountBalances[account.id] ?: account.initialBalance,
                            formatRp = ::formatRp,
                            onClick = {
                                selectedAccount = account
                                inputName = account.name
                                inputInitialBalance = if (account.initialBalance < 0) (account.initialBalance * -1).toLong().toString() else account.initialBalance.toLong().toString()
                                inputType = account.type
                                inputInclude = account.includeInTotal
                                showDialog = true
                            },
                            onReorder = { from, to ->
                                val item = mutableLiabilities.removeAt(from)
                                mutableLiabilities.add(to, item)
                            },
                            onDragEnd = { persistOrder() },
                            list = mutableLiabilities,
                            autoScrollState = autoScrollState
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WalletCard(
    account: AccountEntity,
    currentBalance: Double,
    formatRp: (Double) -> String,
    onClick: () -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,
    onDragEnd: () -> Unit,
    list: List<AccountEntity>,
    autoScrollState: Any
) {
    var dragFromIndex by remember { mutableStateOf(-1) }
    var accumulatedDragY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var cardPositionY by remember { mutableStateOf(0f) }

    val elevation by animateFloatAsState(
        targetValue = if (isDragging) 12f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "cardElevation"
    )
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "cardScale"
    )

    val scrollState = autoScrollState as AutoScrollState

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation = elevation.dp, shape = RoundedCornerShape(20.dp), clip = false)
            .onGloballyPositioned { coords ->
                cardPositionY = coords.positionInRoot().y
            }
            .pointerInput(account.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        dragFromIndex = list.indexOf(account)
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
            }
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(if (account.type == "LIABILITY") ExpenseRed.copy(alpha = 0.1f) else SoftBlue.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = if (account.type == "LIABILITY") ExpenseRed else SoftBlue)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (!account.includeInTotal) Text("Hidden", color = TextSecondary, fontSize = 12.sp)
            }
            Text(formatRp(currentBalance), color = if (currentBalance >= 0) TextPrimary else ExpenseRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Menu, contentDescription = "Drag indicator", tint = TextSecondary.copy(alpha = 0.5f))
        }
    }
}

class RupiahVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.text.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val formattedText = try {
            val format = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
            format.format(text.text.toLong())
        } catch (e: Exception) { text.text }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = formattedText.length
            override fun transformedToOriginal(offset: Int): Int = text.length
        }
        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
