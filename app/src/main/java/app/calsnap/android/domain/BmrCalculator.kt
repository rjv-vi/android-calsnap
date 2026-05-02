package app.calsnap.android.domain

import app.calsnap.android.data.model.UserProfile
import java.time.LocalDate

/**
 * Mifflin-St Jeor BMR + activity-based TDEE.
 *
 * Ported from the PWA's onFin() in assets/js/state.js:
 *   bmr = 10*w + 6.25*h − 5*age + (+5 male / −161 female)
 *   tdee = bmr * activityMultiplier
 */
object BmrCalculator {

    fun bmr(
        gender: UserProfile.Gender,
        weightKg: Float,
        heightCm: Float,
        ageYears: Int,
    ): Float {
        val base = 10f * weightKg + 6.25f * heightCm - 5f * ageYears
        return base + if (gender == UserProfile.Gender.MALE) 5f else -161f
    }

    fun tdee(
        gender: UserProfile.Gender,
        weightKg: Float,
        heightCm: Float,
        ageYears: Int,
        activity: UserProfile.Activity,
    ): Int = (bmr(gender, weightKg, heightCm, ageYears) * activity.multiplier).toInt()

    /** Parse an ISO yyyy-MM-dd date-of-birth string to today's integer age. */
    fun ageFromDob(dob: String, today: LocalDate = LocalDate.now()): Int {
        if (dob.isBlank()) return 0
        return runCatching {
            val parsed = LocalDate.parse(dob)
            var years = today.year - parsed.year
            if (today.dayOfYear < parsed.dayOfYear) years--
            years.coerceAtLeast(0)
        }.getOrDefault(0)
    }
}
