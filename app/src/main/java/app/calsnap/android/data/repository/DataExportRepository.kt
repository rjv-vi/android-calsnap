package app.calsnap.android.data.repository

import app.calsnap.android.data.database.dao.FoodLogDao
import app.calsnap.android.data.database.dao.WaterDao
import app.calsnap.android.data.database.dao.WeightDao
import app.calsnap.android.data.database.entity.FoodLogEntity
import app.calsnap.android.data.database.entity.WaterEntity
import app.calsnap.android.data.database.entity.WeightEntity
import app.calsnap.android.data.model.UserProfile
import app.calsnap.android.data.preferences.SecureKeyStore
import app.calsnap.android.data.preferences.UserPreferences
import app.calsnap.android.domain.BmrCalculator
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataExportRepository @Inject constructor(
    private val foodLogDao: FoodLogDao,
    private val waterDao: WaterDao,
    private val weightDao: WeightDao,
    private val prefs: UserPreferences,
    private val keyStore: SecureKeyStore,
    private val userRepository: UserRepository,
    private val foodLogRepository: FoodLogRepository,
    private val waterRepository: WaterRepository,
    private val weightRepository: WeightRepository,
) {
    private val json = Json { prettyPrint = true }
    private val zone = ZoneId.systemDefault()

    suspend fun exportJsonString(): String {
        val profile = prefs.profile.first()
        val foods = foodLogDao.listAll()
        val weights = weightDao.listAll()
        val water = waterDao.listAll()
        val model = prefs.geminiModel.first()
        val darkTheme = prefs.darkTheme.first()
        val soundOn = prefs.soundOn.first()
        val hapticOn = prefs.hapticOn.first()
        return json.encodeToString(
            buildJsonObject {
                put("version", 1)
                put("exported", Instant.now().toString())
                put("user", profile?.toPwaUserString() ?: "null")
                put("log", json.encodeToString(JsonArray(foods.map { it.toPwaFoodJson() })))
                put("wts", json.encodeToString(JsonArray(weights.map { it.toPwaWeightJson() })))
                put("key", keyStore.getGeminiApiKey().orEmpty())
                put("model", model)
                put("theme", when (darkTheme) {
                    true -> "dark"
                    false -> "light"
                    null -> ""
                })
                put("cal", profile?.kcalGoal?.toString().orEmpty())
                put("hfx", if (hapticOn) "1" else "0")
                put("sfx", if (soundOn) "1" else "0")
                put("notif", "0")
                put("notifCfg", "")
                put("water", water.toPwaWaterObject())
            },
        )
    }

    suspend fun exportCsvString(): String {
        val rows = mutableListOf(
            listOf("date", "time", "food", "portion", "kcal", "protein", "carbs", "fats"),
        )
        foodLogDao.listAll().forEach { item ->
            rows += listOf(
                item.dateString(),
                item.timeString(),
                item.foodName,
                item.portion.orEmpty(),
                item.calories.toString(),
                item.protein.roundString(),
                item.carbs.roundString(),
                item.fat.roundString(),
            )
        }
        rows.add(emptyList())
        rows += listOf("date", "time", "drink", "ml", "", "", "", "")
        waterDao.listAll().forEach { item ->
            rows += listOf(item.dateString(), item.timeString(), "Water", item.milliliters.toString(), "", "", "", "")
        }
        return "\uFEFF" + rows.joinToString("\n") { row -> row.joinToString(",") { it.csvEscape() } }
    }

    suspend fun resetAll() {
        foodLogRepository.wipe()
        waterRepository.wipe()
        weightRepository.wipe()
        userRepository.wipe()
        keyStore.wipe()
    }

    private fun UserProfile.toPwaUserString(): String = json.encodeToString(
        buildJsonObject {
            put("name", name)
            put("dob", dob)
            put("age", BmrCalculator.ageFromDob(dob))
            put("gen", if (gender == UserProfile.Gender.MALE) "m" else "f")
            put("h", heightCm)
            put("w", weightKg)
            put("goal", goal.pwaName())
            put("act", activity.multiplier)
            put("kcal", kcalGoal)
            put("pr", proteinGoal)
            put("ft", fatGoal)
            put("cb", carbsGoal)
            put("prefs", JsonArray(preferences.map { JsonPrimitive(it) }))
            put("allerg", allergies)
        },
    )

    private fun FoodLogEntity.toPwaFoodJson(): JsonObject = buildJsonObject {
        put("food", foodName)
        put("portion", portion.orEmpty())
        put("kcal", calories)
        put("prot", protein)
        put("fat", fat)
        put("carb", carbs)
        put("time", timeString())
        put("date", dateString())
        ingredients?.takeIf { it.isNotBlank() }?.let { raw ->
            put("ingr", JsonArray(raw.split(',').map { it.trim() }.filter { it.isNotBlank() }.map { JsonPrimitive(it) }))
        }
        imagePath?.let { put("img", it) }
        put("meal", mealType.name.lowercase())
        put("servings", servings)
        put("fav", favourite)
    }

    private fun WeightEntity.toPwaWeightJson(): JsonObject = buildJsonObject {
        put("v", weightKg)
        put("d", dateString())
        put("t", timeString())
    }

    private fun List<WaterEntity>.toPwaWaterObject(): JsonObject = buildJsonObject {
        groupBy { it.dateString() }.forEach { (date, entries) ->
            put(
                "water_$date",
                json.encodeToString(
                    buildJsonArray {
                        entries.sortedBy { it.loggedAt }.forEach {
                            add(
                                buildJsonObject {
                                    put("id", "water")
                                    put("ml", it.milliliters)
                                    put("t", it.timeString())
                                },
                            )
                        }
                    },
                ),
            )
        }
    }

    private fun UserProfile.Goal.pwaName(): String = when (this) {
        UserProfile.Goal.LOSE -> "lose"
        UserProfile.Goal.MAINTAIN -> "maintain"
        UserProfile.Goal.GAIN -> "gain"
    }

    private fun FoodLogEntity.dateString(): String = loggedAt.dateString()
    private fun FoodLogEntity.timeString(): String = loggedAt.timeString()
    private fun WaterEntity.dateString(): String = loggedAt.dateString()
    private fun WaterEntity.timeString(): String = loggedAt.timeString()
    private fun WeightEntity.dateString(): String = loggedAt.dateString()
    private fun WeightEntity.timeString(): String = loggedAt.timeString()

    private fun Long.dateString(): String = Instant.ofEpochMilli(this).atZone(zone).toLocalDate().toString()
    private fun Long.timeString(): String = timeFormatter.format(Instant.ofEpochMilli(this).atZone(zone))
    private fun Float.roundString(): String = Math.round(this).toString()
    private fun String.csvEscape(): String {
        val cleaned = replace("\r", " ").replace("\n", " ")
        return if (cleaned.any { it == ',' || it == '"' }) "\"${cleaned.replace("\"", "\"\"")}\"" else cleaned
    }

    private companion object {
        val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
