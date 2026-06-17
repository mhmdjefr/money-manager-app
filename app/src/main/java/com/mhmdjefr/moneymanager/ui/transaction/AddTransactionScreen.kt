package com.mhmdjefr.moneymanager.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mhmdjefr.moneymanager.data.local.AccountEntity
import com.mhmdjefr.moneymanager.data.local.CategoryEntity
import com.mhmdjefr.moneymanager.data.local.TransactionEntity
import com.mhmdjefr.moneymanager.ui.dashboard.getCategoryIcon
import com.mhmdjefr.moneymanager.ui.theme.*
import com.mhmdjefr.moneymanager.ui.wallet.RupiahVisualTransformation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(viewModel: AddTransactionViewModel, transactionId: Int = -1, onBackClick: () -> Unit) {
    val accounts by viewModel.accounts.collectAsState(initial = emptyList())
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val existingTransaction by if (transactionId != -1) viewModel.getTransactionById(transactionId).collectAsState(initial = null) else remember { mutableStateOf<TransactionEntity?>(null) }

    var type by remember { mutableStateOf("EXPENSE") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Calendar.getInstance().timeInMillis) }
    var selectedAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var targetAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    var showAccountDialog by remember { mutableStateOf(false) }
    var showTargetAccountDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

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

    // Custom Styling biar nggak kelihatan kaku
    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = CardWhite,
        unfocusedContainerColor = CardWhite,
        disabledContainerColor = CardWhite,
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
        disabledTextColor = TextPrimary,
        disabledLabelColor = TextSecondary,
        disabledTrailingIconColor = TextPrimary
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transactionId == -1) "Add Transaction" else "Edit Transaction", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        },
        containerColor = LightBackground
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp)) {

            // Transaction Type Selector
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CardWhite).padding(4.dp)) {
                listOf("EXPENSE" to "Expense", "INCOME" to "Income", "TRANSFER" to "Transfer").forEach { (typeKey, label) ->
                    val isSelected = type == typeKey
                    val indicatorColor = when (typeKey) {
                        "EXPENSE" -> ExpenseRed
                        "INCOME" -> SoftBlue
                        else -> TextSecondary
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) indicatorColor else Color.Transparent)
                            .clickable { type = typeKey; selectedCategory = null }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = label, color = if (isSelected) Color.White else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Amount Input
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                label = { Text("Amount (Rp)", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = RupiahVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = inputColors,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date Selector
            OutlinedTextField(
                value = dateFormat.format(date),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date", color = TextSecondary) },
                trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Select Date", tint = SoftBlue) },
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                enabled = false,
                shape = RoundedCornerShape(16.dp),
                colors = inputColors
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Account Selection
            OutlinedTextField(
                value = selectedAccount?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(if (type == "TRANSFER") "From Wallet" else "Wallet", color = TextSecondary) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable { showAccountDialog = true },
                enabled = false,
                shape = RoundedCornerShape(16.dp),
                colors = inputColors
            )

            // Target Account for Transfer
            if (type == "TRANSFER") {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = targetAccount?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("To Wallet", color = TextSecondary) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable { showTargetAccountDialog = true },
                    enabled = false,
                    shape = RoundedCornerShape(16.dp),
                    colors = inputColors
                )
            }

            // Category Selection (Hidden for Transfer)
            if (type != "TRANSFER") {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = selectedCategory?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category", color = TextSecondary) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable { showCategoryDialog = true },
                    enabled = false,
                    shape = RoundedCornerShape(16.dp),
                    colors = inputColors
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Notes (Optional)", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = inputColors
            )

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    val rawAmount = amount.replace(".", "").toDoubleOrNull() ?: 0.0
                    val isValid = rawAmount > 0 && selectedAccount != null && (type != "TRANSFER" || targetAccount != null) && (type == "TRANSFER" || selectedCategory != null)

                    if (isValid) {
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
                        viewModel.saveTransaction(newTransaction)
                        onBackClick()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SoftBlue),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Save Transaction", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date = it }
                    showDatePicker = false
                }) { Text("OK", color = SoftBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = TextSecondary) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Dialogs for Selections
    if (showAccountDialog) {
        SelectionDialog(title = "Select Wallet", items = accounts.map { it.name }, onDismiss = { showAccountDialog = false }) { selectedName ->
            selectedAccount = accounts.find { it.name == selectedName }
        }
    }
    if (showTargetAccountDialog) {
        SelectionDialog(title = "Select Destination Wallet", items = accounts.filter { it.id != selectedAccount?.id }.map { it.name }, onDismiss = { showTargetAccountDialog = false }) { selectedName ->
            targetAccount = accounts.find { it.name == selectedName }
        }
    }
    if (showCategoryDialog) {
        val validCategories = categories.filter { it.type == type }
        SelectionDialog(title = "Select Category", items = validCategories.map { it.name }, onDismiss = { showCategoryDialog = false }) { selectedName ->
            selectedCategory = validCategories.find { it.name == selectedName }
        }
    }
}

@Composable
fun SelectionDialog(title: String, items: List<String>, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(items) { item ->
                        Text(
                            text = item,
                            fontSize = 16.sp,
                            color = TextPrimary,
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(item); onDismiss() }.padding(vertical = 14.dp)
                        )
                    }
                }
            }
        }
    }
}