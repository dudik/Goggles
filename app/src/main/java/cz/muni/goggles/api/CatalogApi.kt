package cz.muni.goggles.api

import cz.muni.goggles.classes.Products
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface CatalogApi {
    @GET("catalog")
    fun getSearchByName(
        @Query("currencyCode") currencyCode: String,
        @Query("query") query: String?,
        @Query("price", encoded = true) priceRange: String,
        @Query("releaseStatuses") upcoming: String?,
        @Query("countryCode") countryCode: String = "SK",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 48,
        @Query("order") order: String = "desc:trending",
        @Query("productType") productType: String = "in:game,pack"
    ): Call<Products>
}
