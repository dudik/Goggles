package cz.muni.goggles

import cz.muni.goggles.classes.Game
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ProductIdApi {
    @GET("products/{ids}")
    fun getProductsByIds(
        @Path("ids")ids: String
    ): Call<Game>
}
