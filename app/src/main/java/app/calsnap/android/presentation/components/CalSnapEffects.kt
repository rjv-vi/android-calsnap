package app.calsnap.android.presentation.components

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import app.calsnap.android.R

enum class CalSnapSoundEffect(val resId: Int, val volume: Float) {
    Welcome(R.raw.welcome, 0.70f),
    Splash(R.raw.splash, 0.50f),
    TabSwitch(R.raw.tab_switch, 0.40f),
    SheetOpen(R.raw.sheet_open, 0.45f),
    SheetClose(R.raw.sheet_close, 0.35f),
    DrumTick(R.raw.drum_tick, 0.22f),
    DrumConfirm(R.raw.drum_confirm, 0.55f),
    ObNext(R.raw.ob_next, 0.50f),
    ObFinish(R.raw.ob_finish, 0.75f),
    AddFood(R.raw.add_food, 0.65f),
    ScanSuccess(R.raw.scan_success, 0.65f),
    ButtonTap(R.raw.btn_tap, 0.30f),
    Toggle(R.raw.toggle, 0.35f),
    Save(R.raw.save, 0.55f),
    Error(R.raw.error, 0.50f),
    AiSend(R.raw.ai_send, 0.40f),
    AiReply(R.raw.ai_reply, 0.45f),
    Select(R.raw.select, 0.40f),
    CardTap(R.raw.card_tap, 0.30f),
    Delete(R.raw.delete, 0.45f),
    WaterAdd(R.raw.water_add, 0.50f),
    WaterUndo(R.raw.water_undo, 0.35f),
    WeightLog(R.raw.weight_log, 0.55f),
    Back(R.raw.back, 0.30f),
    Copy(R.raw.copy, 0.35f),
    NotificationSave(R.raw.notif_save, 0.55f),
    Install(R.raw.install, 0.65f),
    StreakUp(R.raw.streak_up, 0.65f),
    GoalReached(R.raw.goal_reached, 0.75f),
    WaterGoal(R.raw.water_goal, 0.60f),
    PhotoSnap(R.raw.photo_snap, 0.40f),
    AiError(R.raw.ai_error, 0.45f),
    BarcodeScan(R.raw.barcode_scan, 0.50f),
    OnboardSkip(R.raw.onboard_skip, 0.30f),
    ExportDone(R.raw.export_done, 0.50f),
    ImportDone(R.raw.import_done, 0.60f),
    ResetConfirm(R.raw.reset_confirm, 0.50f),
    NotificationRing(R.raw.notif_ring, 0.65f),
}

enum class CalSnapHapticEffect {
    Light,
    Medium,
    Heavy,
    Success,
    Error,
    Tick,
    Double,
}

interface CalSnapSoundPlayer {
    fun setEnabled(enabled: Boolean)
    fun play(effect: CalSnapSoundEffect)
}

interface CalSnapHapticPlayer {
    fun setEnabled(enabled: Boolean)
    fun play(effect: CalSnapHapticEffect)
}

private object NoOpSoundPlayer : CalSnapSoundPlayer {
    override fun setEnabled(enabled: Boolean) = Unit
    override fun play(effect: CalSnapSoundEffect) = Unit
}

private object NoOpHapticPlayer : CalSnapHapticPlayer {
    override fun setEnabled(enabled: Boolean) = Unit
    override fun play(effect: CalSnapHapticEffect) = Unit
}

class CalSnapEffects(
    val sound: CalSnapSoundPlayer,
    val haptic: CalSnapHapticPlayer,
)

val LocalCalSnapEffects = staticCompositionLocalOf {
    CalSnapEffects(NoOpSoundPlayer, NoOpHapticPlayer)
}

@Composable
fun CalSnapEffectsProvider(
    soundOn: Boolean,
    hapticOn: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val sound = remember(context) { AndroidCalSnapSoundPlayer(context) }
    val haptic = remember(context) { AndroidCalSnapHapticPlayer(context) }

    LaunchedEffect(soundOn) { sound.setEnabled(soundOn) }
    LaunchedEffect(hapticOn) { haptic.setEnabled(hapticOn) }
    DisposableEffect(sound) {
        onDispose { sound.release() }
    }

    CompositionLocalProvider(
        LocalCalSnapEffects provides CalSnapEffects(sound, haptic),
        content = content,
    )
}

private class AndroidCalSnapSoundPlayer(context: Context) : CalSnapSoundPlayer {
    private val appContext = context.applicationContext
    private val pool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val soundIds = CalSnapSoundEffect.entries.associateWith { pool.load(appContext, it.resId, 1) }
    private var enabled = false

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun play(effect: CalSnapSoundEffect) {
        if (!enabled) return
        val id = soundIds[effect] ?: return
        pool.play(id, effect.volume, effect.volume, 1, 0, 1f)
    }

    fun release() {
        pool.release()
    }
}

private class AndroidCalSnapHapticPlayer(context: Context) : CalSnapHapticPlayer {
    private val vibrator: Vibrator? = context
        .getSystemService(VibratorManager::class.java)
        ?.defaultVibrator
    private var enabled = false

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun play(effect: CalSnapHapticEffect) {
        if (!enabled) return
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        when (effect) {
            CalSnapHapticEffect.Light -> vibrator.vibratePredefined(VibrationEffect.EFFECT_CLICK)
            CalSnapHapticEffect.Medium -> vibrator.vibrateOnce(24, 140)
            CalSnapHapticEffect.Heavy -> vibrator.vibratePredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            CalSnapHapticEffect.Success -> vibrator.vibratePattern(longArrayOf(0, 18, 44, 28), intArrayOf(0, 120, 0, 190))
            CalSnapHapticEffect.Error -> vibrator.vibratePattern(longArrayOf(0, 26, 56, 26, 56, 34), intArrayOf(0, 210, 0, 210, 0, 230))
            CalSnapHapticEffect.Tick -> vibrator.vibratePredefined(VibrationEffect.EFFECT_TICK)
            CalSnapHapticEffect.Double -> vibrator.vibratePredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
        }
    }

    private fun Vibrator.vibratePredefined(effectId: Int) {
        vibrate(VibrationEffect.createPredefined(effectId))
    }

    private fun Vibrator.vibrateOnce(durationMs: Long, amplitude: Int) {
        vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
    }

    private fun Vibrator.vibratePattern(pattern: LongArray, amplitudes: IntArray) {
        vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
    }
}
