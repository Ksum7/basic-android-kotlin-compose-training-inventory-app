package com.example.inventory.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.commonsware.cwac.saferoom.SQLCipherUtils
import com.commonsware.cwac.saferoom.SafeHelperFactory
import com.example.inventory.EncryptionUtil

@Database(entities = [Item::class], version = 1, exportSchema = false)
abstract class EncryptedInventoryDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var INSTANCE: EncryptedInventoryDatabase? = null

        private val encryptionUtil = EncryptionUtil()

        fun getDatabase(context: Context): EncryptedInventoryDatabase {
            return INSTANCE ?: synchronized(this) {
                var passphrase = encryptionUtil.getDatabasePassphrase()
                val state=  SQLCipherUtils.getDatabaseState(context,
                    "item_database"
                )

                if (state == SQLCipherUtils.State.UNENCRYPTED) {
                    SQLCipherUtils.encrypt(
                        context,
                        "item_database",
                        passphrase
                    )
                }
                val factory =
                    SafeHelperFactory(passphrase)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EncryptedInventoryDatabase::class.java,
                    "item_database"
                )
                    .openHelperFactory(factory)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}