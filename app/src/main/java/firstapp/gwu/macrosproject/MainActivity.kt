package firstapp.gwu.macrosproject

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import firstapp.gwu.macrosproject.data.LogRepository
import firstapp.gwu.macrosproject.data.UserPreferences
import firstapp.gwu.macrosproject.data.translate
import firstapp.gwu.macrosproject.ui.components.AppLogoHeader
import firstapp.gwu.macrosproject.ui.theme.BytesMint
import firstapp.gwu.macrosproject.ui.theme.BytesNavy
import firstapp.gwu.macrosproject.ui.theme.BytesPink
import firstapp.gwu.macrosproject.ui.theme.MacrosProjectTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //firebaseinit // initialize firebase for the whole app
        FirebaseApp.initializeApp(this)
        val app = FirebaseApp.getInstance()
        Log.d("FBInit", "Firebase initialized: ${app.name}")
        enableEdgeToEdge()
        setContent {
            MacrosProjectTheme {
                val context = LocalContext.current
                //loginapproot // show login screen and navigate to dashboard on success
                LoginApp(
                    onLoginSuccess = {
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        if (uid != null) {
                            //logrepoinit // set up logrepository for this user
                            LogRepository.initForUser(uid)
                        }
                        val intent = Intent(context, DashboardActivity::class.java)
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun LoginApp(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current

    //prefsforlanguage // read language + rememberme from datastore (global)
    val prefsData = remember { UserPreferences(context) }
    val lang by prefsData.languageFlow.collectAsState(initial = "en")
    val rememberMe by prefsData.rememberMeFlow.collectAsState(initial = false)

    //sharedprefs // plain sharedpreferences for remembered username
    val prefs = remember { context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE) }

    //usernamestate // only seed from prefs when rememberme is on
    var username by remember(rememberMe) {
        mutableStateOf(
            if (rememberMe) {
                prefs.getString("username", "") ?: ""
            } else {
                ""
            }
        )
    }

    //passwordstate // only seed from prefs when rememberme is on
    var password by remember(rememberMe) {
        mutableStateOf(
            if (rememberMe) {
                prefs.getString("password", "") ?: ""
            } else {
                ""
            }
        )
    }


    //uistate // error text + loading + login trigger
    var error by remember { mutableStateOf<String?>(null) }
    var loginRequested by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFffe6ee)) // background matches dashboard
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //logo
            AppLogoHeader()
            Spacer(Modifier.height(16.dp))
            //logincard // card container for fields + buttons
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF1F4)
                ),
                border = BorderStroke(3.dp, BytesNavy),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = translate(lang, "Log In", "Acceder"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = BytesNavy
                    )
                    Spacer(Modifier.height(20.dp))
                    //emailfield // user can enter email to log in
                    TextField(
                        value = username,
                        onValueChange = { newText -> username = newText },
                        label = {
                            Text(
                                translate(lang, "Enter your Email", "Ingresa tu correo"),
                                color = BytesNavy.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        singleLine = true,
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
                    Spacer(Modifier.height(16.dp))
                    //passwordfield // user can enter password to log in
                    TextField(
                        value = password,
                        onValueChange = { newText -> password = newText },
                        label = {
                            Text(
                                translate(lang, "Enter your password", "Ingresa tu contraseña"),
                                color = BytesNavy.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
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
                    Spacer(Modifier.height(24.dp))
                    //loginbutton // uses firebase to log the user in
                    OutlinedButton(
                        onClick = {
                            loginRequested = true
                            //remembercredentials // save or clear email + password based on rememberme
                            if (rememberMe) {
                                prefs.edit {
                                    putString("username", username)
                                    putString("password", password)
                                }
                            } else {
                                prefs.edit {
                                    remove("username")
                                    remove("password")
                                }
                            }
                        },
                        enabled = checkUsernamePassword(username, password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(40.dp),
                        border = BorderStroke(3.dp, BytesNavy),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFFFF1F4),
                            contentColor = BytesNavy
                        )
                    ) {
                        Text(
                            translate(lang, "Login", "Iniciar sesión"),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    //signupnavbutton // takes user to separate signupactivity
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(context, SignUpActivity::class.java)
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(40.dp),
                        border = BorderStroke(3.dp, BytesNavy),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFFFF1F4),
                            contentColor = BytesNavy
                        )
                    ) {
                        Text(
                            translate(lang, "Sign Up", "Crear cuenta"),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    if (isLoading) {
                        Spacer(Modifier.height(16.dp))
                        CircularProgressIndicator(color = BytesNavy)
                    }
                    error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = BytesPink)
                    }
                }
                Spacer(Modifier.height(20.dp))
                //languagetoggle // same language switch row as before
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val scope = rememberCoroutineScope()
                    Text(
                        text = if (lang == "en") "English" else "Español",
                        color = BytesNavy,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = lang == "es",
                        onCheckedChange = { isChecked ->
                            val newLang = if (isChecked) "es" else "en"
                            scope.launch { prefsData.setLanguage(newLang) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BytesNavy,
                            checkedTrackColor = BytesMint.copy(alpha = 0.5f),
                            uncheckedThumbColor = BytesPink,
                            uncheckedTrackColor = Color(0xFFFFF1F4)
                        )
                    )
                }
            }
        }
    }
    //loginsideeffect // triggers firebase login when loginrequested flips to true
    LaunchedEffect(loginRequested) {
        if (loginRequested) {
            isLoading = true
            try {
                AuthRepository.login(username, password)
                error = null
                onLoginSuccess()
            } catch (e: Exception) {
                error = translate(
                    lang,
                    e.message ?: "Login Failed",
                    "Error al iniciar sesión"
                )
            } finally {
                loginRequested = false
                isLoading = false
            }
        }
    }
}

//loginvalidation // basic check for non-blank + min lengths
fun checkUsernamePassword(username: String, password: String): Boolean {
    val noBlanks = username.isNotBlank() && password.isNotBlank()
    return noBlanks && username.length >= 5 && password.length >= 8
}

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    MacrosProjectTheme {
        LoginApp(onLoginSuccess = {})
    }
}
