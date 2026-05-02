package app.calsnap.android.data.model

/** Matches PWA's `getMealType()` time-based buckets in assets/js/app.js. */
enum class MealType {
    BREAKFAST, LUNCH, SNACK, DINNER;

    companion object {
        fun forHour(hour: Int): MealType = when (hour) {
            in 6..10   -> BREAKFAST
            in 11..13  -> LUNCH
            in 14..17  -> SNACK
            else       -> DINNER
        }
    }
}
