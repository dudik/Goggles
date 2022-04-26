package cz.muni.goggles

import android.app.Application
import cz.muni.goggles.database.SGameDatabase
import cz.muni.goggles.database.SGameRepository

class SGameApplication : Application(){

    val database by lazy { SGameDatabase.getDatabase(this) }
    val repository by lazy { SGameRepository(database.sGameDao()) }
}