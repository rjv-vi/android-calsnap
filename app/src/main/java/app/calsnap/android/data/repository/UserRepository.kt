package app.calsnap.android.data.repository

import app.calsnap.android.data.model.UserProfile
import app.calsnap.android.data.preferences.UserPreferences
import app.calsnap.android.domain.BmrCalculator
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val prefs: UserPreferences,
) {
    val onboardingCompleted: Flow<Boolean> = prefs.onboardingCompleted
    val profile: Flow<UserProfile?>        = prefs.profile
    val darkTheme: Flow<Boolean?>          = prefs.darkTheme
    val language: Flow<String>             = prefs.language

    /**
     * Build a complete UserProfile from onboarding answers, calculating
     * calorie + macro goals via Mifflin-St Jeor (see BmrCalculator) and
     * persisting under the onboarding flag.
     */
    suspend fun completeOnboarding(
        name: String,
        dob: String,
        gender: UserProfile.Gender,
        heightCm: Float,
        weightKg: Float,
        activity: UserProfile.Activity,
        goal: UserProfile.Goal,
        preferences: Set<String>,
        allergies: String,
    ) {
        val age = BmrCalculator.ageFromDob(dob)
        val tdee = BmrCalculator.tdee(
            gender   = gender,
            weightKg = weightKg,
            heightCm = heightCm,
            ageYears = age,
            activity = activity,
        )
        val kcal = (tdee + goal.kcalDelta).coerceAtLeast(1200)
        val proteinG = (weightKg * 1.8f).toInt()
        val fatG     = ((kcal * 0.25f) / 9f).toInt()
        val carbsG   = ((kcal - proteinG * 4 - fatG * 9) / 4f).toInt()

        prefs.saveProfile(
            profile = UserProfile(
                name = name.trim(),
                dob = dob,
                gender = gender,
                heightCm = heightCm,
                weightKg = weightKg,
                activity = activity,
                goal = goal,
                preferences = preferences,
                allergies = allergies,
                kcalGoal = kcal,
                proteinGoal = proteinG,
                carbsGoal = carbsG,
                fatGoal = fatG,
            ),
            markOnboardingDone = true,
        )
    }

    suspend fun setDarkTheme(on: Boolean?) = prefs.setDarkTheme(on)
    suspend fun setLanguage(code: String)  = prefs.setLanguage(code)
    suspend fun wipe()                     = prefs.wipe()
}
