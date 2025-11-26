package firstapp.gwu.macrosproject.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")
object UserPrefsKeys {
    val LANGUAGE = stringPreferencesKey("language")             // "en" or "es"
    val GOAL_CAL = doublePreferencesKey("goal_calories")
    val GOAL_PRO = doublePreferencesKey("goal_protein_g")
    val GOAL_CAR = doublePreferencesKey("goal_carbs_g")
    val GOAL_FAT = doublePreferencesKey("goal_fat_g")
    val REMEMBER_ME = booleanPreferencesKey("remember_me")      // mirrors login switch
}
data class Goals(
    val calories: Double = 2000.0,
    val proteinG: Double = 120.0,
    val carbsG: Double = 200.0,
    val fatG: Double = 70.0
)
class UserPreferences(private val context: Context) {
    val languageFlow: Flow<String> =
        context.dataStore.data.map { it[UserPrefsKeys.LANGUAGE] ?: "en" }
    val rememberMeFlow: Flow<Boolean> =
        context.dataStore.data.map { it[UserPrefsKeys.REMEMBER_ME] ?: false }
    val goalsFlow: Flow<Goals> =
        context.dataStore.data.map {
            Goals(
                calories = it[UserPrefsKeys.GOAL_CAL] ?: 2000.0,
                proteinG = it[UserPrefsKeys.GOAL_PRO] ?: 120.0,
                carbsG = it[UserPrefsKeys.GOAL_CAR] ?: 200.0,
                fatG = it[UserPrefsKeys.GOAL_FAT] ?: 70.0
            )
        }

    suspend fun setLanguage(code: String) {
        context.dataStore.edit { it[UserPrefsKeys.LANGUAGE] = code }
    }
    suspend fun setRememberMe(enabled: Boolean) {
        context.dataStore.edit { it[UserPrefsKeys.REMEMBER_ME] = enabled }
    }
    suspend fun setGoals(g: Goals) {
        context.dataStore.edit {
            it[UserPrefsKeys.GOAL_CAL] = g.calories
            it[UserPrefsKeys.GOAL_PRO] = g.proteinG
            it[UserPrefsKeys.GOAL_CAR] = g.carbsG
            it[UserPrefsKeys.GOAL_FAT] = g.fatG
        }
    }
}
