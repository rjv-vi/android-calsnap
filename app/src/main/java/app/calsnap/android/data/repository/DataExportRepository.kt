package app.calsnap.android.data.repository

import app.calsnap.android.data.database.dao.FoodLogDao
import app.calsnap.android.data.database.dao.WaterDao
import app.calsnap.android.data.database.dao.WeightDao
import app.calsnap.android.data.database.entity.FoodLogEntity
import app.calsnap.android.data.database.entity.WaterEntity
import app.calsnap.android.data.database.entity.WeightEntity
import app.calsnap.android.data.model.UserProfile
import app.calsnap.android.data.preferences.ReminderConfig
import app.calsnap.android.data.preferences.SecureKeyStore
import app.calsnap.android.data.preferences.UserPreferences
import app.calsnap.android.domain.BmrCalculator
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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
        val reminders = prefs.reminderConfig.first()
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
                put("notif", if (reminders.enabled) "1" else "0")
                put("notifCfg", json.encodeToString(reminders.toPwaNotifConfig()))
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

    suspend fun importJsonString(raw: String) {
        val root = json.parseToJsonElement(raw).jsonObject
        root["key"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let(keyStore::setGeminiApiKey)
        root["model"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let { prefs.setGeminiModel(it) }
        root["theme"]?.jsonPrimitive?.contentOrNull?.let { theme ->
            when (theme) {
                "dark" -> prefs.setDarkTheme(true)
                "light" -> prefs.setDarkTheme(false)
            }
        }
        root["sfx"]?.jsonPrimitive?.contentOrNull?.let { prefs.setSoundOn(it != "0") }
        root["hfx"]?.jsonPrimitive?.contentOrNull?.let { prefs.setHapticOn(it != "0") }
        val notifEnabled = root["notif"]?.jsonPrimitive?.contentOrNull?.let { it != "0" && it != "false" }
        parseReminderConfig(root["notifCfg"], notifEnabled)?.let {
            prefs.setReminderConfig(it)
        } ?: notifEnabled?.let {
            prefs.setReminderConfig(prefs.reminderConfig.first().copy(enabled = it))
        }
        root["user"]?.jsonPrimitive?.contentOrNull?.takeIf { it != "null" && it.isNotBlank() }?.let { userRaw ->
            val user = json.parseToJsonElement(userRaw).jsonObject
            val weight = user.floatValue("w", 70f)
            val height = user.floatValue("h", 170f)
            val goal = when (user.stringValue("goal")) {
                "lose" -> UserProfile.Goal.LOSE
                "gain" -> UserProfile.Goal.GAIN
                else -> UserProfile.Goal.MAINTAIN
            }
            prefs.saveProfile(
                UserProfile(
                    name = user.stringValue("name", "User"),
                    dob = user.stringValue("dob", ""),
                    gender = if (user.stringValue("gen") == "f") UserProfile.Gender.FEMALE else UserProfile.Gender.MALE,
                    heightCm = height,
                    weightKg = weight,
                    activity = UserProfile.Activity.entries.minBy {
                        kotlin.math.abs(it.multiplier - user.floatValue("act", UserProfile.Activity.SEDENTARY.multiplier))
                    },
                    goal = goal,
                    preferences = user["prefs"]?.jsonArray.orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull }.toSet(),
                    allergies = user.stringValue("allerg"),
                    kcalGoal = user.intValue("kcal", 2000),
                    proteinGoal = user.intValue("pr", (weight * 1.8f).toInt()),
                    carbsGoal = user.intValue("cb", 250),
                    fatGoal = user.intValue("ft", 60),
                ),
            )
        }
        root["log"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let { logRaw ->
            json.parseToJsonElement(logRaw).jsonArray.forEach { element ->
                val item = element.jsonObject
                foodLogDao.insert(
                    FoodLogEntity(
                        loggedAt = loggedAtFromPwa(item.stringValue("date"), item.stringValue("time")),
                        foodName = item.stringValue("food", "Food"),
                        portion = item.stringValue("portion").takeIf { it.isNotBlank() },
                        calories = item.intValue("kcal", 0),
                        protein = item.floatValue("prot", 0f),
                        fat = item.floatValue("fat", 0f),
                        carbs = item.floatValue("carb", 0f),
                        mealType = runCatching { app.calsnap.android.data.model.MealType.valueOf(item.stringValue("meal").uppercase()) }
                            .getOrDefault(app.calsnap.android.data.model.MealType.SNACK),
                        servings = item.floatValue("servings", 1f),
                        imagePath = item.stringValue("img").takeIf { it.isNotBlank() },
                        ingredients = item["ingr"]?.jsonArray.orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull }.joinToString(", ").takeIf { it.isNotBlank() },
                        favourite = item["fav"]?.jsonPrimitive?.contentOrNull == "true",
                    ),
                )
            }
        }
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

    private fun ReminderConfig.toPwaNotifConfig(): JsonObject = buildJsonObject {
        put("breakfast", breakfastTime)
        put("lunch", lunchTime)
        put("dinner", dinnerTime)
        put("breakfast_on", breakfastOn)
        put("lunch_on", lunchOn)
        put("dinner_on", dinnerOn)
    }

    private fun parseReminderConfig(element: JsonElement?, enabled: Boolean?): ReminderConfig? {
        element ?: return null
        return runCatching {
            val cfg = runCatching { element.jsonObject }.getOrElse {
                json.parseToJsonElement(element.jsonPrimitive.contentOrNull.orEmpty()).jsonObject
            }
            ReminderConfig(
                enabled = enabled ?: false,
                breakfastTime = cfg.stringValue("breakfast", "08:30"),
                lunchTime = cfg.stringValue("lunch", "13:00"),
                dinnerTime = cfg.stringValue("dinner", "19:00"),
                breakfastOn = cfg.boolValue("breakfast_on", cfg.boolValue("breakfastOn", true)),
                lunchOn = cfg.boolValue("lunch_on", cfg.boolValue("lunchOn", true)),
                dinnerOn = cfg.boolValue("dinner_on", cfg.boolValue("dinnerOn", true)),
            )
        }.getOrNull()
    }

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

    private fun JsonObject.stringValue(key: String, default: String = ""): String =
        this[key]?.jsonPrimitive?.contentOrNull ?: default

    private fun JsonObject.floatValue(key: String, default: Float = 0f): Float =
        this[key]?.jsonPrimitive?.floatOrNull ?: this[key]?.jsonPrimitive?.contentOrNull?.toFloatOrNull() ?: default

    private fun JsonObject.intValue(key: String, default: Int = 0): Int =
        this[key]?.jsonPrimitive?.intOrNull ?: this[key]?.jsonPrimitive?.contentOrNull?.toFloatOrNull()?.toInt() ?: default

    private fun JsonObject.boolValue(key: String, default: Boolean = false): Boolean =
        this[key]?.jsonPrimitive?.contentOrNull?.let { it == "true" || it == "1" } ?: default

    private fun loggedAtFromPwa(date: String, time: String): Long =
        runCatching {
            LocalDate.parse(date)
                .atTime(LocalTime.parse(time.ifBlank { "12:00" }))
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        }.getOrDefault(System.currentTimeMillis())

    private companion object {
        val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
