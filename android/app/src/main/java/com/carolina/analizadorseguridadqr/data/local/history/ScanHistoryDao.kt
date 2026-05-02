package com.carolina.analizadorseguridadqr.data.local.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Query(
        """
        SELECT * FROM scan_history
        ORDER BY scannedAt DESC
        LIMIT :limit
        """
    )
    fun observeRecentScans(limit: Int = 100): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(scan: ScanHistoryEntity)

    @Transaction
    suspend fun insertAndKeepOnlyLast(
        scan: ScanHistoryEntity,
        maxRows: Int,
    ) {
        insert(scan)
        keepOnlyLast(maxRows)
    }

    @Query("DELETE FROM scan_history")
    suspend fun clearAll()

    @Query(
        """
        DELETE FROM scan_history
        WHERE id NOT IN (
            SELECT id FROM scan_history
            ORDER BY scannedAt DESC
            LIMIT :maxRows
        )
        """
    )
    suspend fun keepOnlyLast(maxRows: Int)
}
