// ApiClient.kt
package firstapp.gwu.macrosproject.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.Response

data class FoodItem(
    val name: String?,
    val calories: Double?,
    val protein_g: Double?,
    val carbohydrates_total_g: Double?,
    val fat_total_g: Double?
)
data class FoodResponse(val items: List<FoodItem>)

interface CaloriesNinjaApi {
    @GET("v1/nutrition")
    suspend fun getFoodInfo(
        @Query("query") query: String,
        @Header("X-Api-Key") apiKey: String
    ): Response<FoodResponse>
}

object ApiClient {
    private const val BASE_URL = "https://api.calorieninjas.com/"
    val instance: CaloriesNinjaApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CaloriesNinjaApi::class.java)
    }
}
