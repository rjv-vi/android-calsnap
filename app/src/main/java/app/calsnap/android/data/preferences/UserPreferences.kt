package app.calsnap.android.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.calsnap.android.data.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.prefsDataStore by preferencesDataStore(name = "calsnap_prefs")

data class ReminderConfig(
    val enabled: Boolean = false,
    val breakfastTime: String = "08:30",
    val lunchTime: String = "13:00",
    val dinnerTime: String = "19:00",
    val breakfastOn: Boolean = true,
    val lunchOn: Boolean = true,
    val dinnerOn: Boolean = true,
)

/**
 * Wraps DataStore<Preferences> into a typed API centred around
 * [UserProfile] + a few global flags (onboarding done, theme, language).
 *
 * We deliberately don't use proto-datastore: schema is trivial and the
 * added build complexity isn't worth it for v1.
 */
@Singleton
class UserPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val NAME        = stringPreferencesKey("user_name")
        val DOB         = stringPreferencesKey("user_dob")
        val GENDER      = stringPreferencesKey("user_gender")
        val HEIGHT_CM   = floatPreferencesKey("user_height_cm")
        val WEIGHT_KG   = floatPreferencesKey("user_weight_kg")
        val ACTIVITY    = stringPreferencesKey("user_activity")
        val GOAL        = stringPreferencesKey("user_goal")
        val PREFERENCES = stringPreferencesKey("user_preferences")  // comma-separated
        val ALLERGIES   = stringPreferencesKey("user_allergies")
        val KCAL_GOAL   = intPreferencesKey("user_kcal_goal")
        val PROTEIN_GOAL= intPreferencesKey("user_protein_goal")
        val CARBS_GOAL  = intPreferencesKey("user_carbs_goal")
        val FAT_GOAL    = intPreferencesKey("user_fat_goal")

        val DARK_THEME  = booleanPreferencesKey("dark_theme")
        val LANGUAGE    = stringPreferencesKey("language")
        val SOUND_ON    = booleanPreferencesKey("sound_on")
        val HAPTIC_ON   = booleanPreferencesKey("haptic_on")
        val GEMINI_MODEL= stringPreferencesKey("gemini_model")
        val REMINDERS_ON = booleanPreferencesKey("reminders_on")
        val REMINDER_BREAKFAST = stringPreferencesKey("reminder_breakfast")
        val REMINDER_LUNCH = stringPreferencesKey("reminder_lunch")
        val REMINDER_DINNER = stringPreferencesKey("reminder_dinner")
        val REMINDER_BREAKFAST_ON = booleanPreferencesKey("reminder_breakfast_on")
        val REMINDER_LUNCH_ON = booleanPreferencesKey("reminder_lunch_on")
        val REMINDER_DINNER_ON = booleanPreferencesKey("reminder_dinner_on")
    }

    private val data: Flow<Preferences> = context.prefsDataStore.data.catch { error ->
        if (error is CancellationException) throw error
        emit(emptyPreferences())
    }

    val onboardingCompleted: Flow<Boolean> =
        data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }

    val profile: Flow<UserProfile?> = data.map { prefs ->
        val name = prefs[Keys.NAME] ?: return@map null
        UserProfile(
            name        = name,
            dob         = prefs[Keys.DOB] ?: "",
            gender      = runCatching { UserProfile.Gender.valueOf(prefs[Keys.GENDER] ?: "MALE") }
                .getOrDefault(UserProfile.Gender.MALE),
            heightCm    = prefs[Keys.HEIGHT_CM] ?: 170f,
            weightKg    = prefs[Keys.WEIGHT_KG] ?: 70f,
            activity    = runCatching { UserProfile.Activity.valueOf(prefs[Keys.ACTIVITY] ?: "SEDENTARY") }
                .getOrDefault(UserProfile.Activity.SEDENTARY),
            goal        = runCatching { UserProfile.Goal.valueOf(prefs[Keys.GOAL] ?: "MAINTAIN") }
                .getOrDefault(UserProfile.Goal.MAINTAIN),
            preferences = (prefs[Keys.PREFERENCES] ?: "").split(',').filter { it.isNotBlank() }.toSet(),
            allergies   = prefs[Keys.ALLERGIES] ?: "",
            kcalGoal    = prefs[Keys.KCAL_GOAL] ?: 2000,
            proteinGoal = prefs[Keys.PROTEIN_GOAL] ?: 100,
            carbsGoal   = prefs[Keys.CARBS_GOAL] ?: 250,
            fatGoal     = prefs[Keys.FAT_GOAL] ?: 60,
        )
    }

    suspend fun saveProfile(profile: UserProfile, markOnboardingDone: Boolean = true) {
        context.prefsDataStore.edit { p ->
            p[Keys.NAME]         = profile.name
            p[Keys.DOB]          = profile.dob
            p[Keys.GENDER]       = profile.gender.name
            p[Keys.HEIGHT_CM]    = profile.heightCm
            p[Keys.WEIGHT_KG]    = profile.weightKg
            p[Keys.ACTIVITY]     = profile.activity.name
            p[Keys.GOAL]         = profile.goal.name
            p[Keys.PREFERENCES]  = profile.preferences.joinToString(",")
            p[Keys.ALLERGIES]    = profile.allergies
            p[Keys.KCAL_GOAL]    = profile.kcalGoal
            p[Keys.PROTEIN_GOAL] = profile.proteinGoal
            p[Keys.CARBS_GOAL]   = profile.carbsGoal
            p[Keys.FAT_GOAL]     = profile.fatGoal
            if (markOnboardingDone) p[Keys.ONBOARDING_COMPLETED] = true
        }
    }

    val darkTheme: Flow<Boolean?> =
        data.map { it[Keys.DARK_THEME] }

    val language: Flow<String> =
        data.map { it[Keys.LANGUAGE] ?: "ru" }

    val storedLanguage: Flow<String?> =
        data.map { it[Keys.LANGUAGE] }

    val soundOn: Flow<Boolean> =
        data.map { it[Keys.SOUND_ON] ?: false }

    val hapticOn: Flow<Boolean> =
        data.map { it[Keys.HAPTIC_ON] ?: false }

    val geminiModel: Flow<String> =
        data.map { it[Keys.GEMINI_MODEL] ?: "gemini-flash-lite-latest" }

    val reminderConfig: Flow<ReminderConfig> = data.map {
        ReminderConfig(
            enabled = it[Keys.REMINDERS_ON] ?: false,
            breakfastTime = it[Keys.REMINDER_BREAKFAST] ?: "08:30",
            lunchTime = it[Keys.REMINDER_LUNCH] ?: "13:00",
            dinnerTime = it[Keys.REMINDER_DINNER] ?: "19:00",
            breakfastOn = it[Keys.REMINDER_BREAKFAST_ON] ?: true,
            lunchOn = it[Keys.REMINDER_LUNCH_ON] ?: true,
            dinnerOn = it[Keys.REMINDER_DINNER_ON] ?: true,
        )
    }

    suspend fun seedAppearanceIfMissing(darkTheme: Boolean, language: String) = context.prefsDataStore.edit {
        if (!it.contains(Keys.DARK_THEME)) it[Keys.DARK_THEME] = darkTheme
        if (!it.contains(Keys.LANGUAGE)) it[Keys.LANGUAGE] = language
    }

    suspend fun setDarkTheme(on: Boolean?) = context.prefsDataStore.edit {
        it[Keys.DARK_THEME] = on ?: false
    }
    suspend fun setLanguage(code: String) = context.prefsDataStore.edit { it[Keys.LANGUAGE] = code }
    suspend fun setSoundOn(on: Boolean)   = context.prefsDataStore.edit { it[Keys.SOUND_ON]  = on }
    suspend fun setHapticOn(on: Boolean)  = context.prefsDataStore.edit { it[Keys.HAPTIC_ON] = on }
    suspend fun setGeminiModel(id: String)= context.prefsDataStore.edit { it[Keys.GEMINI_MODEL] = id }
    suspend fun setReminderConfig(config: ReminderConfig) = context.prefsDataStore.edit {
        it[Keys.REMINDERS_ON] = config.enabled
        it[Keys.REMINDER_BREAKFAST] = config.breakfastTime
        it[Keys.REMINDER_LUNCH] = config.lunchTime
        it[Keys.REMINDER_DINNER] = config.dinnerTime
        it[Keys.REMINDER_BREAKFAST_ON] = config.breakfastOn
        it[Keys.REMINDER_LUNCH_ON] = config.lunchOn
        it[Keys.REMINDER_DINNER_ON] = config.dinnerOn
    }

    suspend fun wipe() = context.prefsDataStore.edit { it.clear() }
}
