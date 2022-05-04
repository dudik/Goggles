package cz.muni.goggles.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SGameDao {

    @Insert
    suspend fun insert(game: SGame)

    @Delete
    suspend fun delete(game: SGame)

    @Query("SELECT * from sGames_database WHERE gameSlug = :name")
    fun getGame(name: String): SGame?

    @Query("SELECT * from sGames_database")
    fun getAll(): List<SGame>

}