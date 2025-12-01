package firstapp.gwu.macrosproject.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

//dataStoreInstance // single DataStore for the whole app
private val Context.dataStore: androidx.datastore.core.DataStore<Preferences> by preferencesDataStore(
    name = "user_prefs"
)

//goalsModel // simple data class to hold macro + calorie goals
data class Goals(
    val calories: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0
)

class UserPreferences(
    private val context: Context,
    private val userId: String? = null
) {
    //keySuffix // gives each user their own keys for goals + remember me
    private fun keySuffix(): String = userId ?: "default"
    //languageKey // global language setting shared across users on this device
    private val LANGUAGE = stringPreferencesKey("language")
    //goalKeys // per-user goal keys
    private val CAL_KEY = doublePreferencesKey("calories_${keySuffix()}")
    private val PRO_KEY = doublePreferencesKey("protein_${keySuffix()}")
    private val CARB_KEY = doublePreferencesKey("carbs_${keySuffix()}")
    private val FAT_KEY = doublePreferencesKey("fat_${keySuffix()}")
    //rememberMeKey // per-user remember credentials flag
    private val REMEMBER_KEY = booleanPreferencesKey("remember_global")
    //languageFlow //"en" or "es" from DataStore
    val languageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LANGUAGE] ?: "en"
    }
    //goalsFlow // emits the current goals for this user
    val goalsFlow: Flow<Goals> = context.dataStore.data.map { prefs ->
        Goals(
            calories = prefs[CAL_KEY] ?: 0.0,
            proteinG = prefs[PRO_KEY] ?: 0.0,
            carbsG   = prefs[CARB_KEY] ?: 0.0,
            fatG     = prefs[FAT_KEY] ?: 0.0
        )
    }
    //rememberMeFlow //true if this user wanted credentials remembered
    val rememberMeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[REMEMBER_KEY] ?: false
    }
    //setLanguage // saves the current language to DataStore
    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE] = lang
        }
    }
    //setGoals // saves all four goals for this user
    suspend fun setGoals(goals: Goals) {
        context.dataStore.edit { prefs ->
            prefs[CAL_KEY] = goals.calories
            prefs[PRO_KEY] = goals.proteinG
            prefs[CARB_KEY] = goals.carbsG
            prefs[FAT_KEY] = goals.fatG
        }
    }
    //setRememberMe // turns the remember-credentials flag on or off for this user
    suspend fun setRememberMe(remember: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[REMEMBER_KEY] = remember
        }
    }
}
