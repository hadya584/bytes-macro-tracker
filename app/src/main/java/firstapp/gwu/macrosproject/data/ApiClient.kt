package firstapp.gwu.macrosproject.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Response

data class FoodItem(
    val name: String? = null,
    val calories: Double? = null,
    val protein_g: Double? = null,
    val carbohydrates_total_g: Double? = null,
    val fat_total_g: Double? = null,
    val servingSize: Double? = null,
    val servingUnit: String? = null,
    val servings: Double? = 1.0
)

data class USDAFood(
    val description: String?,
    val foodNutrients: List<USDANutrient>?,
    val servingSize: Double?,      // ⬅️ new (USDA search response has this)
    val servingSizeUnit: String?   // ⬅️ new
)

data class USDANutrient(
    val nutrientName: String?,
    val value: Double?
)


data class USDAResponse(
    val foods: List<USDAFood>?
)

interface USDAApi {
    @GET("v1/foods/search")
    suspend fun searchFoods(
        @Query("query") query: String,
        @Query("api_key") apiKey: String
    ): Response<USDAResponse>
}

object ApiClient {
    private const val BASE_URL = "https://api.nal.usda.gov/fdc/"
    val instance: USDAApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(USDAApi::class.java)
    }
}
