package cz.muni.goggles.classes

data class Game (
    val title: String,
    val coverHorizontal: String?,
    val slug: String,
    var price: Price?,
    val id: Int,
    val images: LogoImage?
    )

data class Price (
    val final: String,
    val base: String,
    val discount: String
    )