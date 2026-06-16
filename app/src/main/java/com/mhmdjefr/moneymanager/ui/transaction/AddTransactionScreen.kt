package com.mhmdjefr.moneymanager.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhmdjefr.moneymanager.data.local.AccountEntity
import com.mhmdjefr.moneymanager.ui.dashboard.getCategoryIcon
import com.mhmdjefr.moneymanager.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

data class CategoryItem(val name: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(viewModel: AddTransactionViewModel, transactionId: Int = -1, onBackClick: () -> Unit = {}) {
    val dbAccounts by viewModel.accountList.collectAsState(initial = emptyList())

    var selectedType by remember { mutableStateOf("Expense") }
    var nominal by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    var selectedCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var expandedCategory by remember { mutableStateOf(false) }

    var selectedFromAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var expandedFrom by remember { mutableStateOf(false) }
    var selectedToAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var expandedTo by remember { mutableStateOf(false) }

    var amountError by remember { mutableStateOf(false) }
    var walletError by remember { mutableStateOf(false) }
    var categoryError by remember { mutableStateOf(false) }

    val categories = when (selectedType) {
        "Income" -> listOf(CategoryItem("Salary", Icons.Default.Work), CategoryItem("Bonus", Icons.Default.CardGiftcard), CategoryItem("Gift", Icons.Default.VolunteerActivism), CategoryItem("Investment", Icons.Default.TrendingUp), CategoryItem("Freelance", Icons.Default.LaptopMac), CategoryItem("Others", Icons.Default.MoreHoriz))
        "Expense" -> listOf(CategoryItem("Food", Icons.Default.Fastfood), CategoryItem("Transport", Icons.Default.DirectionsCar), CategoryItem("Shopping", Icons.Default.ShoppingCart), CategoryItem("Bills", Icons.Default.Receipt), CategoryItem("Entertainment", Icons.Default.Movie), CategoryItem("Health", Icons.Default.LocalHospital), CategoryItem("Education", Icons.Default.School), CategoryItem("Installment", Icons.Default.CreditCard), CategoryItem("Others", Icons.Default.MoreHoriz))
        else -> emptyList()
    }

    LaunchedEffect(selectedType) {
        nominal = ""
        selectedCategory = null
        selectedFromAccount = null
        selectedToAccount = null
        amountError = false
        walletError = false
        categoryError = false
    }

    LaunchedEffect(transactionId, dbAccounts) {
        if (transactionId != -1 && dbAccounts.isNotEmpty()) {
            val tx = viewModel.getTransaction(transactionId)
            tx?.let {
                val mappedType = when (it.type) {
                    "INCOME" -> "Income"
                    "EXPENSE" -> "Expense"
                    else -> "Transfer"
                }
                selectedType = mappedType
                nominal = it.amount.toLong().toString()

                selectedFromAccount = dbAccounts.find { acc -> acc.id == it.accountId }

                val noteStr = it.note ?: ""
                if (mappedType == "Transfer") {
                    selectedToAccount = dbAccounts.find { acc -> acc.id == it.targetAccountId }
                    note = noteStr.substringAfter("|").trim()
                } else {
                    if (noteStr.startsWith("[")) {
                        val catName = noteStr.substringAfter("[").substringBefore("]")
                        note = noteStr.substringAfter("]").trim()
                        selectedCategory = CategoryItem(catName, getCategoryIcon(catName))
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transactionId != -1) "Edit Transaction" else "Add Transaction", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        },
        containerColor = LightBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CardWhite).padding(4.dp)
            ) {
                listOf("Income", "Expense", "Transfer").forEach { type ->
                    val isSelected = selectedType == type
                    val bgColor = if (isSelected) SoftBlue else Color.Transparent
                    val textColor = if (isSelected) Color.White else TextSecondary

                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(bgColor).clickable { selectedType = type }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(text = type, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = nominal,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) {
                        nominal = newValue
                        amountError = false
                    }
                },
                label = { Text("Amount (Rp)") },
                isError = amountError,
                supportingText = { if (amountError) Text("Amount is required!", color = ExpenseRed) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                visualTransformation = RupiahVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardWhite, unfocusedContainerColor = CardWhite, unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedType == "Transfer") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = expandedFrom, onExpandedChange = { expandedFrom = !expandedFrom }, modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedFromAccount?.name ?: "",
                            onValueChange = {}, readOnly = true, label = { Text("Source") },
                            isError = walletError,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrom) },
                            modifier = Modifier.menuAnchor(), shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardWhite, unfocusedContainerColor = CardWhite, unfocusedBorderColor = Color.Transparent)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedFrom, onDismissRequest = { expandedFrom = false },
                            modifier = Modifier.background(CardWhite)
                        ) {
                            dbAccounts.forEach { acc -> DropdownMenuItem(text = { Text(acc.name) }, onClick = { selectedFromAccount = acc; expandedFrom = false; walletError = false }) }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = expandedTo, onExpandedChange = { expandedTo = !expandedTo }, modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedToAccount?.name ?: "",
                            onValueChange = {}, readOnly = true, label = { Text("Destination") },
                            isError = walletError,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTo) },
                            modifier = Modifier.menuAnchor(), shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardWhite, unfocusedContainerColor = CardWhite, unfocusedBorderColor = Color.Transparent)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedTo, onDismissRequest = { expandedTo = false },
                            modifier = Modifier.background(CardWhite)
                        ) {
                            dbAccounts.forEach { acc -> DropdownMenuItem(text = { Text(acc.name) }, onClick = { selectedToAccount = acc; expandedTo = false; walletError = false }) }
                        }
                    }
                }
                if (walletError) {
                    Text("Source and destination wallet must be selected!", color = ExpenseRed, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                }
            } else {
                val walletLabel = if (selectedType == "Income") "Deposit to Wallet" else "Pay from Wallet"
                ExposedDropdownMenuBox(expanded = expandedFrom, onExpandedChange = { expandedFrom = !expandedFrom }) {
                    OutlinedTextField(
                        value = selectedFromAccount?.name ?: "",
                        onValueChange = {}, readOnly = true, label = { Text(walletLabel) },
                        isError = walletError,
                        supportingText = { if (walletError) Text("Wallet is required!", color = ExpenseRed) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrom) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardWhite, unfocusedContainerColor = CardWhite, unfocusedBorderColor = Color.Transparent)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedFrom, onDismissRequest = { expandedFrom = false },
                        modifier = Modifier.background(CardWhite)
                    ) {
                        dbAccounts.forEach { acc -> DropdownMenuItem(text = { Text(acc.name) }, onClick = { selectedFromAccount = acc; expandedFrom = false; walletError = false }) }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val categoryLabel = if (selectedType == "Income") "Income Category" else "Expense Category"
                ExposedDropdownMenuBox(expanded = expandedCategory, onExpandedChange = { expandedCategory = !expandedCategory }) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "",
                        onValueChange = {}, readOnly = true, label = { Text(categoryLabel) },
                        isError = categoryError,
                        supportingText = { if (categoryError) Text("Category is required!", color = ExpenseRed) },
                        leadingIcon = selectedCategory?.let { { Icon(it.icon, contentDescription = null, tint = SoftBlue) } },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardWhite, unfocusedContainerColor = CardWhite, unfocusedBorderColor = Color.Transparent)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategory, onDismissRequest = { expandedCategory = false },
                        modifier = Modifier.background(CardWhite)
                    ) {
                        categories.forEach { categoryItem ->
                            DropdownMenuItem(
                                leadingIcon = { Icon(categoryItem.icon, contentDescription = null, tint = SoftBlue) },
                                text = { Text(categoryItem.name) },
                                onClick = { selectedCategory = categoryItem; expandedCategory = false; categoryError = false }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardWhite, unfocusedContainerColor = CardWhite, unfocusedBorderColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val nominalValue = nominal.replace(".", "").toDoubleOrNull() ?: 0.0
                    val saveId = if (transactionId != -1) transactionId else null
                    val fromAccId = selectedFromAccount?.id ?: 0

                    var isValid = true

                    if (nominalValue <= 0) { amountError = true; isValid = false }
                    if (fromAccId == 0) { walletError = true; isValid = false }

                    if (selectedType == "Transfer") {
                        val toAccId = selectedToAccount?.id ?: 0
                        if (toAccId == 0 || fromAccId == toAccId) { walletError = true; isValid = false }

                        if (isValid) {
                            val transferNote = "${selectedFromAccount?.name} -> ${selectedToAccount?.name} | $note"
                            viewModel.saveTransaction(saveId, selectedType, nominalValue, "Transfer", transferNote, fromAccId, toAccId)
                            onBackClick()
                        }
                    } else {
                        val catName = selectedCategory?.name ?: ""
                        if (catName.isEmpty()) { categoryError = true; isValid = false }

                        if (isValid) {
                            viewModel.saveTransaction(saveId, selectedType, nominalValue, catName, note, fromAccId)
                            onBackClick()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SoftBlue)
            ) {
                Text(if (transactionId != -1) "Update Transaction" else "Save Transaction", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Nah ini dia fungsi formatting Rupiah yang kemarin kepotong
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