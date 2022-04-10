package cz.muni.goggles

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

// catalog?limit=48&order=desc:trending&productType=in:game,pack&page=1&countryCode=SK&locale=en-US&currencyCode=EUR
interface Api {
    @GET("catalog?limit=48&order=desc:trending&productType=in:game,pack&page=1&countryCode=SK&locale=en-US&currencyCode=EUR")
    fun getSearchByName(
        @Query("query") query: String,
    ): Call<Products>
}