package cz.muni.goggles.logic

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import cz.muni.goggles.adapter.Adapter
import cz.muni.goggles.api.CatalogApi
import cz.muni.goggles.classes.Products
import cz.muni.goggles.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

private val retrofit: Retrofit = Retrofit.Builder().baseUrl("https://catalog.gog.com/v1/").addConverterFactory(MoshiConverterFactory.create()).build()

private val service: CatalogApi = retrofit.create(CatalogApi::class.java)

fun refresh(context: Context, binding: ActivityMainBinding, sharedPreferences: SharedPreferences, adapter: Adapter, query: String, upcoming: Boolean, pageNumber: Int = 1)
{
    service.getSearchByName(
        sharedPreferences.getString("currency", "EUR") ?: "EUR",
        if (query.isBlank()) null else "like:$query",
        "between%3A${binding.priceRangeSlider.values[0]}%2C${binding.priceRangeSlider.values[1]}",
        if (upcoming) "in:upcoming" else null,
        page = pageNumber).enqueue(object : Callback<Products>
    {
        override fun onResponse(call: Call<Products>, response: Response<Products>)
        {
            val responseBody = response.body()
            if (response.isSuccessful && responseBody != null)
            {
                println(responseBody.products)
                adapter.setItems(responseBody.products, pageNumber != 1)
            }
        }

        override fun onFailure(call: Call<Products>, t: Throwable)
        {
            println(t.message)
            Toast.makeText(context, "${t.message}", Toast.LENGTH_SHORT).show()
        }
    })
}