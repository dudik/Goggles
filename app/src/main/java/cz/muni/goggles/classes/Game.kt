package cz.muni.goggles.classes

data class Game (
    val title: String,
    val coverHorizontal: String,
    val slug: String,
    val price: Price,
    val id: Int
    )

data class Price (
    val final: String,
    val base: String,
    val discount: String
    )