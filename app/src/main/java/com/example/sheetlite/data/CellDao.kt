package com.example.sheetlite.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CellDao {
    @Query("SELECT * FROM cells")
    fun getAllCells(): Flow<List<CellEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCell(cell: CellEntity)

    @Query("DELETE FROM cells WHERE row = :row AND col = :col")
    suspend fun deleteCell(row: Int, col: Int)

    @Query("DELETE FROM cells")
    suspend fun clearAll()
}
