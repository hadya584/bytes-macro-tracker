package firstapp.gwu.macrosproject

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import firstapp.gwu.macrosproject.data.Goals
import firstapp.gwu.macrosproject.data.UserPreferences
import firstapp.gwu.macrosproject.data.translate
import firstapp.gwu.macrosproject.ui.components.AppLogoHeader
import firstapp.gwu.macrosproject.ui.theme.BytesMint
import firstapp.gwu.macrosproject.ui.theme.BytesNavy
import firstapp.gwu.macrosproject.ui.theme.BytesPink
import firstapp.gwu.macrosproject.ui.theme.MacrosProjectTheme
import kotlinx.coroutines.launch

class Settings : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //fromregistrationflag // indicates if user came here right after signing up
        val fromRegistration = intent.getBooleanExtra("fromRegistration", false)
        setContent {
            MacrosProjectTheme {
                SettingsScreen(fromRegistration = fromRegistration)
            }
        }
    }
}

@Composable
fun SettingsScreen(fromRegistration: Boolean = false) {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    if (uid == null) {
        Text("No user logged in")
        return
    }

    //userprefs // per-user prefs for goals
    val userPrefs = remember(uid) { UserPreferences(context, uid) }
    //globalprefs // global prefs for language + rememberme
    val globalPrefs = remember { UserPreferences(context) }

    val goals by userPrefs.goalsFlow.collectAsState(initial = Goals())
    val scope = rememberCoroutineScope()
    val activity = context as? Activity

    val currentLang by globalPrefs.languageFlow.collectAsState(initial = "en")
    val rememberMe by globalPrefs.rememberMeFlow.collectAsState(initial = false)

    var lang by remember { mutableStateOf(currentLang) }
    var remember by remember { mutableStateOf(rememberMe) }

    var calTxt by remember {
        mutableStateOf(goals.calories.takeIf { it > 0 }?.toInt()?.toString() ?: "")
    }
    var proTxt by remember {
        mutableStateOf(goals.proteinG.takeIf { it > 0 }?.toInt()?.toString() ?: "")
    }
    var carbTxt by remember {
        mutableStateOf(goals.carbsG.takeIf { it > 0 }?.toInt()?.toString() ?: "")
    }
    var fatTxt by remember {
        mutableStateOf(goals.fatG.takeIf { it > 0 }?.toInt()?.toString() ?: "")
    }

    LaunchedEffect(currentLang, goals, rememberMe) {
        lang = currentLang
        remember = rememberMe
        calTxt = goals.calories.takeIf { it > 0 }?.toInt()?.toString() ?: ""
        proTxt = goals.proteinG.takeIf { it > 0 }?.toInt()?.toString() ?: ""
        carbTxt = goals.carbsG.takeIf { it > 0 }?.toInt()?.toString() ?: ""
        fatTxt = goals.fatG.takeIf { it > 0 }?.toInt()?.toString() ?: ""
    }

    val goalsValid by remember(calTxt, proTxt, carbTxt, fatTxt) {
        mutableStateOf(
            listOf(calTxt, proTxt, carbTxt, fatTxt).all { txt ->
                txt.isNotBlank() && txt.toIntOrNull()?.let { it > 0 } == true
            }
        )
    }

    //blockbackiffromregistration
    BackHandler(enabled = !goalsValid && fromRegistration) {
        Toast.makeText(
            context,
            translate(
                lang,
                "Please fill in all your goals before leaving Settings",
                "Por favor completa todas tus metas antes de salir de Configuración"
            ),
            Toast.LENGTH_SHORT
        ).show()
    }

    Scaffold(containerColor = Color(0xFFffe6ee)) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(Color(0xFFffe6ee))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //logo
            AppLogoHeader()
            Spacer(Modifier.height(4.dp))
            //title
            Text(
                text = translate(lang, "SETTINGS", "Configuración"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = BytesNavy
            )
            //subtitle
            Text(
                text = translate(
                    lang,
                    "Set your daily calorie and macro goals",
                    "Configura tus metas diarias de calorías y macros"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = BytesNavy.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(6.dp))
            //maincard – slightly tighter padding + spacing
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F4)),
                border = BorderStroke(3.dp, BytesNavy),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    //languagetoggle + rememberswitch in one compact column
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (lang == "en") "English" else "Español",
                                color = BytesNavy,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = translate(
                                    lang,
                                    "Language",
                                    "Idioma"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = BytesNavy.copy(alpha = 0.7f)
                            )
                        }

                        Switch(
                            checked = lang == "es",
                            onCheckedChange = { isChecked ->
                                lang = if (isChecked) "es" else "en"
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BytesNavy,
                                checkedTrackColor = BytesMint.copy(alpha = 0.5f),
                                uncheckedThumbColor = BytesPink,
                                uncheckedTrackColor = Color(0xFFFFF1F4)
                            )
                        )
                    }

                    //remembercredentialsrow
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = translate(
                                lang,
                                "Remember credentials",
                                "Recordar credenciales"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = BytesNavy
                        )
                        Switch(
                            checked = remember,
                            onCheckedChange = { remember = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BytesNavy,
                                checkedTrackColor = BytesMint.copy(alpha = 0.5f),
                                uncheckedThumbColor = BytesPink,
                                uncheckedTrackColor = Color(0xFFFFF1F4)
                            )
                        )
                    }

                    Divider(color = BytesNavy.copy(alpha = 0.2f))

                    Text(
                        text = translate(lang, "Macro & Calorie Goals", "Metas de macros y calorías"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = BytesNavy,
                        fontWeight = FontWeight.SemiBold
                    )

                    //macrofields – minimal spacing
                    MacroNumberField(
                        label = translate(lang, "Calories", "Calorías"),
                        value = calTxt,
                        onChange = { calTxt = it }
                    )
                    MacroNumberField(
                        label = translate(lang, "Protein (g)", "Proteína (g)"),
                        value = proTxt,
                        onChange = { proTxt = it }
                    )
                    MacroNumberField(
                        label = translate(lang, "Carbs (g)", "Carbohidratos (g)"),
                        value = carbTxt,
                        onChange = { carbTxt = it }
                    )
                    MacroNumberField(
                        label = translate(lang, "Fat (g)", "Grasa (g)"),
                        value = fatTxt,
                        onChange = { fatTxt = it }
                    )

                    if (!goalsValid) {
                        Text(
                            text = translate(
                                lang,
                                "Please fill in all goals",
                                "Por favor ingrese todos los objetivos"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = BytesPink
                        )
                    }

                    //backandsaverow – compact
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    //savegoalsbeforeleaving
                                    val newGoals = Goals(
                                        calories = calTxt.toIntOrNull()?.toDouble()
                                            ?: goals.calories,
                                        proteinG = proTxt.toIntOrNull()?.toDouble()
                                            ?: goals.proteinG,
                                        carbsG = carbTxt.toIntOrNull()?.toDouble()
                                            ?: goals.carbsG,
                                        fatG = fatTxt.toIntOrNull()?.toDouble()
                                            ?: goals.fatG
                                    )
                                    userPrefs.setGoals(newGoals)
                                    globalPrefs.setLanguage(lang)
                                    globalPrefs.setRememberMe(remember)

                                    if (fromRegistration) {
                                        val dashIntent = Intent(
                                            context,
                                            DashboardActivity::class.java
                                        ).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        }
                                        context.startActivity(dashIntent)
                                    } else {
                                        activity?.finish()
                                    }
                                }
                            },
                            enabled = goalsValid
                        ) {
                            Text(
                                text = translate(lang, "Dashboard", "Atrás"),
                                color = if (goalsValid) BytesNavy else BytesNavy.copy(alpha = 0.3f)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    globalPrefs.setLanguage(lang)
                                    globalPrefs.setRememberMe(remember)
                                    val newGoals = Goals(
                                        calories = calTxt.toIntOrNull()?.toDouble()
                                            ?: goals.calories,
                                        proteinG = proTxt.toIntOrNull()?.toDouble()
                                            ?: goals.proteinG,
                                        carbsG = carbTxt.toIntOrNull()?.toDouble()
                                            ?: goals.carbsG,
                                        fatG = fatTxt.toIntOrNull()?.toDouble()
                                            ?: goals.fatG
                                    )
                                    userPrefs.setGoals(newGoals)

                                    Toast.makeText(
                                        context,
                                        translate(
                                            lang,
                                            "Settings saved",
                                            "Configuración guardada"
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            enabled = goalsValid,
                            border = BorderStroke(2.dp, BytesNavy),
                            shape = RoundedCornerShape(30.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFFFF1F4),
                                contentColor = BytesNavy
                            ),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text(
                                text = translate(lang, "Save", "Guardar"),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.1.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            //logoutbutton – still visible at bottom
            OutlinedButton(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(context, MainActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                },
                border = BorderStroke(3.dp, BytesPink),
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFFFE6EE),
                    contentColor = BytesNavy
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Text(
                    text = translate(lang, "Logout", "Cerrar sesión"),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun MacroNumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    //numerictextfield // underlined number-only field
    //ref: https://developer.android.com/jetpack/compose/text-fields
    TextField(
        value = value,
        onValueChange = { input ->
            val cleaned = input.filter { it.isDigit() }
            onChange(cleaned)
        },
        label = {
            Text(
                label,
                color = BytesNavy.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFffe6ee)),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
            color = BytesNavy
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedTextColor = BytesNavy,
            unfocusedTextColor = BytesNavy,
            cursorColor = BytesNavy,
            focusedLabelColor = BytesNavy,
            unfocusedLabelColor = BytesNavy.copy(alpha = 0.6f),
            focusedIndicatorColor = BytesNavy,
            unfocusedIndicatorColor = BytesNavy.copy(alpha = 0.3f)
        )
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    MacrosProjectTheme {
        SettingsScreen(fromRegistration = false)
    }
}
