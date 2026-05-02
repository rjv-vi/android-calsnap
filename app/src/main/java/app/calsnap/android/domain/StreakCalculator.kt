package app.calsnap.android.domain

import java.time.LocalDate

/**
 * Day-streak with "one skip per week" auto-freeze, ported from the PWA's
 * streak() function in assets/js/state.js.
 *
 * Input: a predicate that answers "did the user log anything on this day?".
 * Output: integer streak length, plus the dates that were auto-frozen.
 */
object StreakCalculator {

    data class Result(val streak: Int, val frozenDates: Set<LocalDate>)

    fun calculate(
        today: LocalDate = LocalDate.now(),
        existingFreezes: Set<LocalDate> = emptySet(),
        hasLog: (LocalDate) -> Boolean,
    ): Result {
        val frozen = existingFreezes.toMutableSet()
        var usedFreezeThisWeek = frozen.any { it.isThisWeek(today) }
        var streak = 0
        var i = 0
        while (true) {
            val day = today.minusDays(i.toLong())
            if (hasLog(day)) { streak++; i++; continue }
            if (i == 0) { i++; continue }           // today not yet logged — keep looking
            if (frozen.contains(day))         { i++; continue }
            if (!usedFreezeThisWeek) {
                frozen.add(day); usedFreezeThisWeek = true; i++; continue
            }
            break
        }
        return Result(streak, frozen)
    }

    private fun LocalDate.isThisWeek(today: LocalDate): Boolean {
        val monday = today.minusDays((today.dayOfWeek.value - 1L))
        return !this.isBefore(monday) && !this.isAfter(today)
    }
}
