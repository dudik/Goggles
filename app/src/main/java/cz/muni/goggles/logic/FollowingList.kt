package cz.muni.goggles.logic

import android.content.Context
import android.widget.Toast
import cz.muni.goggles.adapter.Adapter
import cz.muni.goggles.api.ProductIdApi
import cz.muni.goggles.classes.Game
import cz.muni.goggles.classes.Price
import cz.muni.goggles.database.SGame
import cz.muni.goggles.database.SGameDatabase
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException

private val retrofitGog: Retrofit = Retrofit.Builder().baseUrl("https://api.gog.com/").addConverterFactory(MoshiConverterFactory.create()).build()
private val productIdService: ProductIdApi = retrofitGog.create(ProductIdApi::class.java)

fun getFollowing(context: Context, adapter: Adapter)
{
    val games = SGameDatabase.getDatabase(context).sGameDao().getAll()
    val gamesList: MutableList<Game> = mutableListOf()

    for (game in games)
    {

        val client = OkHttpClient()
        val request = Request.Builder().url("https://www.gog.com/products/prices?ids=${game.productId}&countryCode=SK&currency=${game.currency}").build()

        client.newCall(request).enqueue(object : okhttp3.Callback
        {
            override fun onResponse(call: okhttp3.Call, responsePrice: okhttp3.Response)
            {
                if (responsePrice.isSuccessful)
                {
                    getFollowingAndSetPrice(context, adapter, game, gamesList, extractPriceString(game, responsePrice))
                }
            }

            override fun onFailure(call: okhttp3.Call, e: IOException)
            {
                Toast.makeText(context, "${e.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

private fun extractPriceString(game: SGame, responsePrice: okhttp3.Response): String
{
    var finalPrice: String
    val html = responsePrice.body()!!.string()
    var json = html.substring(html.indexOf("cardProduct: ") + "cardProduct: ".length)

    json = json.substring(0, json.indexOf("bonusWalletFunds"))
    json = json.trim().dropLast(7)
    json = json.substring(json.indexOf("\"finalPrice\":\""))
    finalPrice = (json.split(":\"")[1].toInt() / 100f).toString()
    finalPrice = convertCurrencyToSymbol(game.currency) + finalPrice

    return finalPrice
}

private fun getFollowingAndSetPrice(context: Context, adapter: Adapter, game: SGame, gamesList: MutableList<Game>, finalPrice: String)
{
    productIdService.getProductsByIds(game.productId.toString()).enqueue(object : Callback<Game>
        {
            override fun onResponse(call: Call<Game>, response: Response<Game>)
            {
                print(call.request().toString())
                val responseBody = response.body()
                if (response.isSuccessful && responseBody != null)
                {
                    responseBody.price = Price(finalPrice, "0", "0")
                    gamesList.add(responseBody)
                    adapter.setItems(gamesList)
                }
            }

            override fun onFailure(call: Call<Game>, t: Throwable)
            {
                Toast.makeText(context, "${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
}