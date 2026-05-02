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

    val soundOn: Flow<Boolean> =
        data.map { it[Keys.SOUND_ON] ?: true }

    val hapticOn: Flow<Boolean> =
        data.map { it[Keys.HAPTIC_ON] ?: true }

    val geminiModel: Flow<String> =
        data.map { it[Keys.GEMINI_MODEL] ?: "gemini-2.0-flash-lite" }

    suspend fun setDarkTheme(on: Boolean?) = context.prefsDataStore.edit {
        if (on == null) it.remove(Keys.DARK_THEME) else it[Keys.DARK_THEME] = on
    }
    suspend fun setLanguage(code: String) = context.prefsDataStore.edit { it[Keys.LANGUAGE] = code }
    suspend fun setSoundOn(on: Boolean)   = context.prefsDataStore.edit { it[Keys.SOUND_ON]  = on }
    suspend fun setHapticOn(on: Boolean)  = context.prefsDataStore.edit { it[Keys.HAPTIC_ON] = on }
    suspend fun setGeminiModel(id: String)= context.prefsDataStore.edit { it[Keys.GEMINI_MODEL] = id }

    suspend fun wipe() = context.prefsDataStore.edit { it.clear() }
}
