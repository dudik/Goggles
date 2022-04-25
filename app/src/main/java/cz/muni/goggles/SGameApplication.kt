package cz.muni.goggles

import android.app.Application
import cz.muni.goggles.database.SGameDatabase
import cz.muni.goggles.database.SGameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class SGameApplication : Application(){

    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { SGameDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { SGameRepository(database.sGameDao()) }
}