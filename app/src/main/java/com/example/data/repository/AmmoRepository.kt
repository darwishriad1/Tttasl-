package com.example.data.repository

import com.example.data.local.AmmoDao
import com.example.data.model.AmmoTransaction
import com.example.data.model.AmmoType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class AmmoRepository(private val ammoDao: AmmoDao) {

    val allTransactions: Flow<List<AmmoTransaction>> = ammoDao.getAllTransactions()

    suspend fun insert(transaction: AmmoTransaction): Long {
        return ammoDao.insertTransaction(transaction)
    }

    suspend fun delete(transaction: AmmoTransaction) {
        ammoDao.deleteTransaction(transaction)
    }

    suspend fun clearAll() {
        ammoDao.clearAll()
    }

    // Database starts completely empty per user request. Seeding is disabled.
    suspend fun seedInitialDataIfNeeded() {
        // No-op: Data is added manually by the user
    }
}
