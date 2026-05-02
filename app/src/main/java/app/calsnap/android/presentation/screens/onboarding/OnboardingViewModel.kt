package app.calsnap.android.presentation.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.calsnap.android.data.model.UserProfile
import app.calsnap.android.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * v1 stub: holds the 5-step onboarding draft in memory and persists it via
 * UserRepository.completeOnboarding at the last step. Real implementation
 * plan is in TASKS.md § Onboarding.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    data class Draft(
        val step: Int = 1,
        val name: String = "",
        val gender: UserProfile.Gender = UserProfile.Gender.MALE,
        val dob: String = "",
        val heightCm: Float = 170f,
        val weightKg: Float = 70f,
        val activity: UserProfile.Activity = UserProfile.Activity.SEDENTARY,
        val goal: UserProfile.Goal = UserProfile.Goal.MAINTAIN,
        val preferences: Set<String> = emptySet(),
        val allergies: String = "",
    )

    private val _draft = MutableStateFlow(Draft())
    val draft: StateFlow<Draft> = _draft.asStateFlow()

    fun update(patch: (Draft) -> Draft) = _draft.update(patch)

    fun finish(onDone: () -> Unit) {
        val d = _draft.value
        viewModelScope.launch {
            userRepository.completeOnboarding(
                name        = d.name,
                dob         = d.dob,
                gender      = d.gender,
                heightCm    = d.heightCm,
                weightKg    = d.weightKg,
                activity    = d.activity,
                goal        = d.goal,
                preferences = d.preferences,
                allergies   = d.allergies,
            )
            onDone()
        }
    }
}
