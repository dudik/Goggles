package cz.muni.goggles

data class Game (
    val title: String,
    val coverHorizontal: String,
    val slug: String,
    val price: Price
    )

data class Price (
    val final: String,
    val base: String,
    val discount: String
    )