package com.example.financetracker

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    fun getAll(): LiveData<List<Transaction>>

    @Query("""
        SELECT * FROM transactions
        WHERE dateMillis >= :startMillis AND dateMillis < :endMillis
        ORDER BY dateMillis DESC
    """)
    fun getByMonth(startMillis: Long, endMillis: Long): LiveData<List<Transaction>>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}