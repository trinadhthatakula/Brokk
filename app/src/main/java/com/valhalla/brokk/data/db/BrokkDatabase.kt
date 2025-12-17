package com.valhalla.brokk.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HistoryEntity::class], version = 1, exportSchema = false)
abstract class BrokkDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}