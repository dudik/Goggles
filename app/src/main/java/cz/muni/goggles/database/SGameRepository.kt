package cz.muni.goggles.database

import androidx.annotation.WorkerThread

class SGameRepository(private val sGameDao: SGameDao) {

    @WorkerThread
    suspend fun insert(game: SGame) {
        sGameDao.insert(game)
    }

    @WorkerThread
    suspend fun delete(game: SGame) {
        sGameDao.delete(game)
    }

    fun getGame(name: String): SGame? {
       return sGameDao.getGame(name)
    }

    fun getAll(): List<SGame> {
        return sGameDao.getAll()
    }

    fun getAllIds(): List<Int> {
        return sGameDao.getAllIds()
    }

}