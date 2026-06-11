package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AmmoTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AmmoDao {
    @Query("SELECT * FROM ammo_transactions ORDER BY timestamp DESC, id DESC")
    fun getAllTransactions(): Flow<List<AmmoTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: AmmoTransaction): Long

    @Delete
    suspend fun deleteTransaction(transaction: AmmoTransaction)

    @Query("SELECT * FROM ammo_transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): AmmoTransaction?

    @Query("DELETE FROM ammo_transactions")
    suspend fun clearAll()
}
