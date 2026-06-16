package com.mhmdjefr.moneymanager

import android.app.Application
import com.mhmdjefr.moneymanager.data.local.AppDatabase
import com.mhmdjefr.moneymanager.data.repository.MoneyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MoneyApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { MoneyRepository(database.moneyDao()) }
}