package app.calsnap.android.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * OpenFoodFacts v2 lookup by barcode. Returns null `product` when nothing
 * matches — we fall back to Gemini + user confirmation in that case.
 */
interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json?fields=code,product_name,product_name_ru,brands,quantity,nutriments,image_front_small_url,image_front_url")
    suspend fun fetchProduct(@Path("barcode") barcode: String): OffResponse

    companion object { const val BASE_URL = "https://world.openfoodfacts.org/" }
}

@Serializable
data class OffResponse(
    val code: String? = null,
    val status: Int = 0,
    val product: OffProduct? = null,
)

@Serializable
data class OffProduct(
    val code: String? = null,
    val product_name: String? = null,
    val product_name_ru: String? = null,
    val brands: String? = null,
    val quantity: String? = null,
    val image_front_small_url: String? = null,
    val image_front_url: String? = null,
    val nutriments: OffNutriments? = null,
)

@Serializable
data class OffNutriments(
    @SerialName("energy-kcal_100g")
    val energy_kcal_100g: Float? = null,
    @SerialName("proteins_100g")
    val proteins_100g: Float? = null,
    @SerialName("carbohydrates_100g")
    val carbohydrates_100g: Float? = null,
    @SerialName("fat_100g")
    val fat_100g: Float? = null,
)
