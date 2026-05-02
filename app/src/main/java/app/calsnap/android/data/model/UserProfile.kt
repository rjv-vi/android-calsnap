package app.calsnap.android.data.model

/**
 * User profile. Persisted in DataStore (single-row preferences style; we
 * don't need Room for this). Mirrors the PWA's `U` state object in
 * `assets/js/state.js`.
 */
data class UserProfile(
    val name: String,
    val dob: String,         // ISO yyyy-MM-dd
    val gender: Gender,
    val heightCm: Float,
    val weightKg: Float,
    val activity: Activity,
    val goal: Goal,
    val preferences: Set<String> = emptySet(),
    val allergies: String = "",
    val kcalGoal: Int,
    val proteinGoal: Int,
    val carbsGoal: Int,
    val fatGoal: Int,
) {
    enum class Gender { MALE, FEMALE }

    enum class Activity(val multiplier: Float) {
        SEDENTARY(1.2f),
        LIGHT(1.375f),
        MODERATE(1.55f),
        ACTIVE(1.725f),
    }

    enum class Goal(val kcalDelta: Int) {
        LOSE(-500),
        MAINTAIN(0),
        GAIN(+300),
    }
}
