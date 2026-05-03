package app.calsnap.android.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the user's Gemini API key, encrypted at rest with AndroidX Security
 * Crypto. We keep this OUT of DataStore on purpose: the EncryptedSharedPrefs
 * backing file is wrapped by a keystore-managed AES-GCM key, which gives
 * stronger protection for a credential-shaped value like an API key.
 */
@Singleton
class SecureKeyStore @Inject constructor(@ApplicationContext context: Context) {

    private val appContext = context.applicationContext
    private var prefs: SharedPreferences? = runCatching { encryptedPrefs(appContext) }
        .recoverCatching {
            appContext.deleteSharedPreferences(PREFS_FILE)
            encryptedPrefs(appContext)
        }
        .getOrNull()
    private var memoryKey: String? = null

    /** Null when the user has not entered a key yet. */
    fun getGeminiApiKey(): String? {
        val stored = runCatching { ensurePrefs()?.getString(KEY_GEMINI, null) }
            .getOrElse {
                recoverEncryptedPrefs()
                null
            }
        return (stored ?: memoryKey)?.takeIf { it.isNotBlank() }
    }

    fun setGeminiApiKey(apiKey: String?) {
        memoryKey = apiKey?.trim()?.takeIf { it.isNotBlank() }
        runCatching {
            ensurePrefs()?.edit()?.apply {
                if (apiKey.isNullOrBlank()) remove(KEY_GEMINI)
                else putString(KEY_GEMINI, apiKey.trim())
                apply()
            }
        }.onFailure {
            recoverEncryptedPrefs()
        }
    }

    fun hasGeminiKey(): Boolean = getGeminiApiKey() != null

    fun wipe() {
        memoryKey = null
        runCatching { ensurePrefs()?.edit()?.clear()?.apply() }
            .onFailure { recoverEncryptedPrefs() }
    }

    private companion object {
        const val PREFS_FILE = "calsnap_secure"
        const val KEY_GEMINI = "gemini_api_key"

        fun encryptedPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }

    private fun ensurePrefs(): SharedPreferences? {
        if (prefs == null) prefs = runCatching { encryptedPrefs(appContext) }.getOrNull()
        return prefs
    }

    private fun recoverEncryptedPrefs() {
        prefs = null
        runCatching { appContext.deleteSharedPreferences(PREFS_FILE) }
        prefs = runCatching { encryptedPrefs(appContext) }.getOrNull()
    }
}
