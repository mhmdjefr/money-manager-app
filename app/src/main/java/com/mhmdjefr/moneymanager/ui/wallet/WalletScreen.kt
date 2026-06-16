package com.mhmdjefr.moneymanager.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.mhmdjefr.moneymanager.ui.dashboard.DashboardViewModel
import com.mhmdjefr.moneymanager.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(viewModel: DashboardViewModel) {
    val accounts by viewModel.accountList.collectAsState(initial = emptyList())
    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    val isVisible by viewModel.isBalanceVisible.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var inputName by remember { mutableStateOf("") }
    var inputInitialBalance by remember { mutableStateOf("") }
    var inputType by remember { mutableStateOf("REGULAR") }
    var inputInclude by remember { mutableStateOf(true) }

    val format = NumberFormat.getNumberInstance(Locale("id", "ID"))
    fun formatRp(amount: Double) = if (isVisible) "Rp ${format.format(amount)}" else "Rp ••••••••"

    var totalAssets = 0.0
    var totalLiabilities = 0.0

    accounts.forEach { account ->
        val income = transactions.filter { it.accountId == account.id && it.type == "INCOME" }.sumOf { it.amount }
        val expense = transactions.filter { it.accountId == account.id && it.type == "EXPENSE" }.sumOf { it.amount }
        val transferOut = transactions.filter { it.accountId == account.id && it.type == "TRANSFER" }.sumOf { it.amount }
        val transferIn = transactions.filter { it.targetAccountId == account.id && it.type == "TRANSFER" }.sumOf { it.amount }

        val currentBalance = account.initialBalance + income - expense - transferOut + transferIn
        if (account.includeInTotal) {
            if (account.type == "REGULAR") totalAssets += currentBalance
            else totalLiabilities += currentBalance
        }
    }
    val netWorth = totalAssets + totalLiabilities

    val mutableAccounts = remember(accounts) { accounts.toMutableStateList() }

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
                            TextButton(onClick = { viewModel.deleteAccount(selectedAccount!!); showDialog = false }) { Text("Delete", color = ExpenseRed) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallets", fontWeight = FontWeight.Bold) },
                actions = {
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Net Worth", color = Color.LightGray, fontSize = 14.sp)
                            Text(formatRp(netWorth), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { viewModel.toggleBalanceVisibility() }) {
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(mutableAccounts, key = { _, account -> account.id }) { index, account ->
                    var dragFromIndex by remember { mutableStateOf(-1) }
                    var accumulatedDragY by remember { mutableStateOf(0f) }

                    val income = transactions.filter { it.accountId == account.id && it.type == "INCOME" }.sumOf { it.amount }
                    val expense = transactions.filter { it.accountId == account.id && it.type == "EXPENSE" }.sumOf { it.amount }
                    val transferOut = transactions.filter { it.accountId == account.id && it.type == "TRANSFER" }.sumOf { it.amount }
                    val transferIn = transactions.filter { it.targetAccountId == account.id && it.type == "TRANSFER" }.sumOf { it.amount }
                    val currentBalance = account.initialBalance + income - expense - transferOut + transferIn

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        dragFromIndex = mutableAccounts.indexOf(account)
                                        accumulatedDragY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        accumulatedDragY += dragAmount.y

                                        // Ambang batas geser ~120 pixel per kartu
                                        if (accumulatedDragY > 120f && dragFromIndex < mutableAccounts.size - 1) {
                                            val targetIndex = dragFromIndex + 1
                                            val item = mutableAccounts.removeAt(dragFromIndex)
                                            mutableAccounts.add(targetIndex, item)
                                            dragFromIndex = targetIndex
                                            accumulatedDragY = 0f
                                        } else if (accumulatedDragY < -120f && dragFromIndex > 0) {
                                            val targetIndex = dragFromIndex - 1
                                            val item = mutableAccounts.removeAt(dragFromIndex)
                                            mutableAccounts.add(targetIndex, item)
                                            dragFromIndex = targetIndex
                                            accumulatedDragY = 0f
                                        }
                                    },
                                    onDragEnd = {
                                        viewModel.updateAccountsOrder(mutableAccounts)
                                        dragFromIndex = -1
                                    },
                                    onDragCancel = { dragFromIndex = -1 }
                                )
                            }
                            .clickable {
                                selectedAccount = account
                                inputName = account.name
                                inputInitialBalance = if (account.initialBalance < 0) (account.initialBalance * -1).toLong().toString() else account.initialBalance.toLong().toString()
                                inputType = account.type
                                inputInclude = account.includeInTotal
                                showDialog = true
                            },
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
            }
        }
    }
}

class RupiahVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.text.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val formattedText = try {
            val format = NumberFormat.getNumberInstance(Locale("id", "ID"))
            format.format(text.text.toLong())
        } catch (e: Exception) { text.text }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = formattedText.length
            override fun transformedToOriginal(offset: Int): Int = text.length
        }
        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}