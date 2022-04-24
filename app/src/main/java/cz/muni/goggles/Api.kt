package cz.muni.goggles

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface Api {
    @GET("catalog")
    fun getSearchByName(
        @Query("currencyCode") currencyCode: String,
        @Query("query") query: String?,
        @Query("price", encoded = true) priceRange: String,
        @Query("releaseStatuses") upcoming: String?,
        @Query("countryCode") countryCode: String = "SK"
    ): Call<Products>
}
