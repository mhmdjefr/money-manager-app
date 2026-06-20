package com.mhmdjefr.moneymanager.ui.transaction

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhmdjefr.moneymanager.data.local.AccountEntity
import com.mhmdjefr.moneymanager.data.local.CategoryEntity
import com.mhmdjefr.moneymanager.data.local.TransactionEntity
import com.mhmdjefr.moneymanager.ui.common.ConfirmDeleteDialog
import com.mhmdjefr.moneymanager.ui.settings.getCategoryIcon
import com.mhmdjefr.moneymanager.ui.theme.*
import com.mhmdjefr.moneymanager.ui.wallet.RupiahVisualTransformation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(viewModel: AddTransactionViewModel, transactionId: Int = -1, onBackClick: () -> Unit) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState(initial = emptyList())
    val categories by viewModel.categories.collectAsState(initial = emptyList())

    var existingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    LaunchedEffect(transactionId) {
        if (transactionId != -1) {
            viewModel.getTransactionById(transactionId).collect { tx ->
                existingTransaction = tx
            }
        }
    }

    var type by remember { mutableStateOf("EXPENSE") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Calendar.getInstance().timeInMillis) }

    var selectedAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var targetAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

    // Pre-fill data saat Edit Mode
    LaunchedEffect(existingTransaction, accounts, categories) {
        existingTransaction?.let { tx ->
            type = tx.type
            amount = tx.amount.toLong().toString()
            date = tx.date
            selectedAccount = accounts.find { it.id == tx.accountId }

            if (tx.type == "TRANSFER") {
                targetAccount = accounts.find { it.id == tx.targetAccountId }
                note = tx.note ?: ""
            } else {
                val parsedCategoryName = tx.note?.substringBefore("]")?.replace("[", "") ?: ""
                selectedCategory = categories.find { it.name == parsedCategoryName }
                note = tx.note?.substringAfter("]")?.trim() ?: ""
            }
        }
    }

    // Auto-select item pertama biar user gak usah ngeklik kalau udah sesuai
    LaunchedEffect(accounts, type) {
        if (selectedAccount == null && accounts.isNotEmpty() && transactionId == -1) {
            selectedAccount = accounts.first()
        }
        if (targetAccount == null && accounts.size > 1 && type == "TRANSFER" && transactionId == -1) {
            targetAccount = accounts.last()
        }
    }

    val currentCategories = categories.filter { it.type == type }
    LaunchedEffect(currentCategories, type) {
        if (transactionId == -1 && currentCategories.isNotEmpty()) {
            selectedCategory = currentCategories.first()
        }
    }

    val baseColor = when (type) {
        "EXPENSE" -> ExpenseRed
        "INCOME" -> Color(0xFF5ED5A8)
        else -> SoftBlue
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transactionId == -1) "Add Transaction" else "Edit Transaction", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (transactionId != -1 && existingTransaction != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        },
        containerColor = LightBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. TABS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardWhite)
                    .padding(4.dp)
            ) {
                listOf("EXPENSE" to "Expense", "INCOME" to "Income", "TRANSFER" to "Transfer").forEach { (typeKey, label) ->
                    val isSelected = type == typeKey
                    val indicatorColor = when (typeKey) {
                        "EXPENSE" -> ExpenseRed
                        "INCOME" -> Color(0xFF5ED5A8)
                        else -> SoftBlue
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) indicatorColor else Color.Transparent)
                            .clickable { type = typeKey }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = label, color = if (isSelected) Color.White else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. AMOUNT INPUT & DATE CHIP
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                    placeholder = { Text("0", fontSize = 32.sp, color = TextSecondary) },
                    leadingIcon = { Text("Rp", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(start = 16.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = RupiahVisualTransformation(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = baseColor),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                    )
                )

                // Date Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardWhite)
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = baseColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(dateFormat.format(date), fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. WALLET CHIPS
            Text(if (type == "TRANSFER") "From Wallet" else "Wallet", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(accounts) { acc ->
                    val isSelected = selectedAccount?.id == acc.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) baseColor else CardWhite)
                            .border(1.dp, if (isSelected) baseColor else Color(0xFFE0E0E0), RoundedCornerShape(20.dp))
                            .clickable { selectedAccount = acc }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = acc.name, color = if (isSelected) Color.White else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (type == "TRANSFER") {
                Spacer(modifier = Modifier.height(16.dp))
                Text("To Wallet", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts.filter { it.id != selectedAccount?.id }) { acc ->
                        val isSelected = targetAccount?.id == acc.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) SoftBlue else CardWhite)
                                .border(1.dp, if (isSelected) SoftBlue else Color(0xFFE0E0E0), RoundedCornerShape(20.dp))
                                .clickable { targetAccount = acc }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = acc.name, color = if (isSelected) Color.White else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. CATEGORY GRID (Hanya untuk Expense & Income)
            if (type != "TRANSFER") {
                Text("Category", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(modifier = Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.weight(1f) // Biar sisa ruang diisi sama grid, dan tombol save tetep di bawah
                ) {
                    items(currentCategories) { cat ->
                        val isSelected = selectedCategory?.id == cat.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { selectedCategory = cat }.padding(bottom = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) baseColor else CardWhite)
                                    .border(2.dp, if (isSelected) baseColor else Color.Transparent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = getCategoryIcon(cat.iconName), contentDescription = cat.name, tint = if (isSelected) Color.White else TextSecondary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = cat.name, fontSize = 12.sp, color = if (isSelected) TextPrimary else TextSecondary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f)) // Pengisi ruang kosong buat Transfer
            }

            // 5. NOTES & SAVE BUTTON
            Column(modifier = Modifier.fillMaxWidth().background(CardWhite).padding(24.dp)) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Add notes (Optional)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = baseColor, unfocusedBorderColor = Color(0xFFE0E0E0))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val rawAmount = amount.replace(".", "").toDoubleOrNull() ?: 0.0

                        val errorMessage = when {
                            rawAmount <= 0 -> "Please enter an amount"
                            selectedAccount == null -> "Please select a wallet"
                            type == "TRANSFER" && targetAccount == null -> "Please select a destination wallet"
                            type != "TRANSFER" && selectedCategory == null -> "Please select a category"
                            else -> null
                        }

                        if (errorMessage != null) {
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                        } else if (!isSaving) {
                            isSaving = true
                            val finalNote = if (type == "TRANSFER") note else "[${selectedCategory?.name}] $note"
                            val finalTargetId = if (type == "TRANSFER") targetAccount?.id else null

                            val newTransaction = TransactionEntity(
                                id = if (transactionId != -1) transactionId else 0,
                                accountId = selectedAccount!!.id,
                                amount = rawAmount,
                                type = type,
                                date = date,
                                note = finalNote.trim(),
                                targetAccountId = finalTargetId
                            )
                            viewModel.saveTransaction(newTransaction) {
                                isSaving = false
                                Toast.makeText(context, "Transaction Saved!", Toast.LENGTH_SHORT).show()
                                onBackClick()
                            }
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = baseColor, disabledContainerColor = baseColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(if (transactionId == -1) "Save Transaction" else "Update Transaction", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date = it }
                    showDatePicker = false
                }) { Text("OK", color = baseColor) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = TextSecondary) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            message = "This transaction will be permanently deleted. This action cannot be undone.",
            onConfirm = {
                existingTransaction?.let { viewModel.deleteTransaction(it) }
                onBackClick()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}