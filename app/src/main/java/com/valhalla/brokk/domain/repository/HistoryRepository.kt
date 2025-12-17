package com.valhalla.brokk.domain.repository

import com.valhalla.brokk.domain.model.HistoryRecord
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getHistory(): Flow<List<HistoryRecord>>
    suspend fun addRecord(record: HistoryRecord)
    suspend fun clearHistory()
}