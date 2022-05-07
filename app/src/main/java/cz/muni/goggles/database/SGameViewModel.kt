package cz.muni.goggles.database

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SGameViewModel(private val repository: SGameRepository) : ViewModel()
{

    fun insert(game: SGame) = viewModelScope.launch {
        repository.insert(game)
    }

    fun delete(game: SGame) = viewModelScope.launch {
        repository.delete(game)
    }

    fun getGame(name: String): SGame?
    {
        return repository.getGame(name)
    }

    fun getAll(): List<SGame>
    {
        return repository.getAll()
    }
}

class SGameViewModelFactory(private val repository: SGameRepository) : ViewModelProvider.Factory
{
    override fun <T : ViewModel> create(modelClass: Class<T>): T
    {
        if (modelClass.isAssignableFrom(SGameViewModel::class.java))
        {
            @Suppress("UNCHECKED_CAST") return SGameViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}