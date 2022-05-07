package cz.muni.goggles.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sGames_database")
data class SGame(
    @PrimaryKey
    @ColumnInfo(name = "gameSlug")
    val gameSlug: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "productId")
    val productId: Int,
    @ColumnInfo(name = "price")
    val price: Int,
    @ColumnInfo(name = "currency")
    val currency: String
)

