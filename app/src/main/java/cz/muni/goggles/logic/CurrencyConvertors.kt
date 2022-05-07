package cz.muni.goggles.logic

fun convertCurrencyToSymbol(symbol: String): String
{
    if (symbol == "USD")
    {
        return "$"
    }
    if (symbol == "EUR")
    {
        return "€"
    }
    return ""
}

fun convertSymbolToCurrency(symbol: Char): String
{
    if (symbol == '$')
    {
        return "USD"
    }
    if (symbol == '€')
    {
        return "EUR"
    }
    return ""
}