package cz.muni.goggles.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SGame::class], version = 1, exportSchema = false)
abstract class SGameDatabase : RoomDatabase() {

    abstract fun sGameDao(): SGameDao

    companion object {

        @Volatile
        private var INSTANCE: SGameDatabase? = null

        fun getDatabase(context: Context): SGameDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SGameDatabase::class.java,
                    "sGames_database"
                ).allowMainThreadQueries()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}