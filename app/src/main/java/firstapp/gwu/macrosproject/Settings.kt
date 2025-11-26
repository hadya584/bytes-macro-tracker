package firstapp.gwu.macrosproject

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import firstapp.gwu.macrosproject.data.Goals
import firstapp.gwu.macrosproject.data.UserPreferences
import firstapp.gwu.macrosproject.ui.theme.MacrosProjectTheme
import kotlinx.coroutines.launch

class Settings : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MacrosProjectTheme {
                SettingsScreen()
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    val scope = rememberCoroutineScope()

    // Observe current values from DataStore
    val currentLang by prefs.languageFlow.collectAsState(initial = "en")
    val rememberMe by prefs.rememberMeFlow.collectAsState(initial = false)
    val goals by prefs.goalsFlow.collectAsState(initial = Goals())

    // Editable UI state
    var lang by remember { mutableStateOf(currentLang) }
    var remember by remember { mutableStateOf(rememberMe) }
    var calTxt by remember { mutableStateOf(goals.calories.toString()) }
    var proTxt by remember { mutableStateOf(goals.proteinG.toString()) }
    var carbTxt by remember { mutableStateOf(goals.carbsG.toString()) }
    var fatTxt by remember { mutableStateOf(goals.fatG.toString()) }

    // Keep UI in sync if flows update underneath
    LaunchedEffect(currentLang, rememberMe, goals) {
        lang = currentLang
        remember = rememberMe
        calTxt = goals.calories.toString()
        proTxt = goals.proteinG.toString()
        carbTxt = goals.carbsG.toString()
        fatTxt = goals.fatG.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)

        // Language
        Text("Language")
        LanguagePicker(selected = lang, onSelect = { lang = it })

        // Remember credentials (shared with login)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Remember credentials", modifier = Modifier.weight(1f))
            Switch(checked = remember, onCheckedChange = { remember = it })
        }

        Text("Macro & Calorie Goals")
        NumberField("Calories", calTxt) { calTxt = it }
        NumberField("Protein (g)", proTxt) { proTxt = it }
        NumberField("Carbs (g)", carbTxt) { carbTxt = it }
        NumberField("Fat (g)", fatTxt) { fatTxt = it }

        Spacer(Modifier.height(8.dp))

        Button(onClick = {
            scope.launch {
                // Save language + apply app locales
                prefs.setLanguage(lang)
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(lang)
                )

                // Save remember-me
                prefs.setRememberMe(remember)

                // Save goals (ignore invalid input and keep previous)
                val newGoals = Goals(
                    calories = calTxt.toDoubleOrNull() ?: goals.calories,
                    proteinG = proTxt.toDoubleOrNull() ?: goals.proteinG,
                    carbsG   = carbTxt.toDoubleOrNull() ?: goals.carbsG,
                    fatG     = fatTxt.toDoubleOrNull() ?: goals.fatG
                )
                prefs.setGoals(newGoals)

                Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Save")
        }
    }
}

@Composable
private fun LanguagePicker(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterChip(
            selected = selected == "en",
            onClick = { onSelect("en") },
            label = { Text("English") }
        )
        FilterChip(
            selected = selected == "es",
            onClick = { onSelect("es") },
            label = { Text("Español") }
        )
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    TextField(
        value = value,
        onValueChange = { input ->
            // allow digits and one decimal point
            val cleaned = buildString {
                var dotSeen = false
                input.forEach { c ->
                    if (c.isDigit()) append(c)
                    else if (c == '.' && !dotSeen) { append('.'); dotSeen = true }
                }
            }
            onChange(if (cleaned.isEmpty()) value else cleaned)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
