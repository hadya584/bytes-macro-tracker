package firstapp.gwu.macrosproject

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import firstapp.gwu.macrosproject.data.Goals
import firstapp.gwu.macrosproject.data.LogRepository
import firstapp.gwu.macrosproject.data.UserPreferences
import firstapp.gwu.macrosproject.data.translate
import firstapp.gwu.macrosproject.ui.components.AppLogoHeader
import firstapp.gwu.macrosproject.ui.theme.BytesMint
import firstapp.gwu.macrosproject.ui.theme.BytesNavy
import firstapp.gwu.macrosproject.ui.theme.BytesPink
import firstapp.gwu.macrosproject.ui.theme.MacrosProjectTheme
import kotlinx.coroutines.launch

class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MacrosProjectTheme {
                SignUpScreen(
                    onSignUpSuccess = {
                        //signUpSuccessNav // on success go to Settings to set goals
                        val context = this
                        val intent = Intent(context, Settings::class.java).apply {
                            putExtra("fromRegistration", true)
                        }
                        context.startActivity(intent)
                        finish()
                    },
                    onBackToLogin = {
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current

    //prefsForLang // use global language from DataStore
    val prefsData = remember { UserPreferences(context) }
    val lang by prefsData.languageFlow.collectAsState(initial = "en")

    //formState // email, password, confirm password
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    //uiState // errors + loading + sign up trigger
    var error by remember { mutableStateOf<String?>(null) }
    var signUpRequested by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    //passwordsMatch // requirement: do not allow sign up if they don't match
    val passwordsMatch = password == confirmPassword && password.isNotBlank()

    //formValid // both email/password validity + matching passwords
    val formValid = checkUsernamePassword(email, password) && passwordsMatch

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFffe6ee))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //logoTop // same logo as login
            AppLogoHeader()

            Spacer(Modifier.height(16.dp))

            //signUpCard // card container for sign up form
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
                    Spacer(Modifier.height(8.dp))

                    //titleText // makes it clear this is the sign up screen
                    Text(
                        text = translate(lang, "Create Account", "Crear cuenta"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = BytesNavy
                    )

                    Spacer(Modifier.height(20.dp))

                    //emailField // email for new account
                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        label = {
                            Text(
                                translate(lang, "Email", "Correo electrónico"),
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

                    //passwordField // first password input
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = {
                            Text(
                                translate(lang, "Password", "Contraseña"),
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

                    Spacer(Modifier.height(16.dp))

                    //confirmPasswordField // second password, must match
                    TextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = {
                            Text(
                                translate(lang, "Confirm Password", "Confirmar contraseña"),
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

                    //passwordMismatchHint // show a tiny message if they don't match
                    if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = translate(
                                lang,
                                "Passwords do not match",
                                "Las contraseñas no coinciden"
                            ),
                            color = BytesPink,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    //signUpButton // uses Firebase via AuthRepository.register
                    OutlinedButton(
                        onClick = { signUpRequested = true },
                        enabled = formValid,
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
                            translate(lang, "Sign Up", "Registrarse"),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    //backToLogin // secondary action to go back to login screen
                    TextButton(onClick = onBackToLogin) {
                        Text(
                            translate(lang, "Back to Login", "Volver a iniciar sesión"),
                            color = BytesNavy
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
                //languageToggle // same as login screen
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val scope = rememberCoroutineScope()
                    val prefs = remember { UserPreferences(context) }
                    val currentLang by prefs.languageFlow.collectAsState(initial = "en")

                    Text(
                        text = if (currentLang == "en") "English" else "Español",
                        color = BytesNavy,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(Modifier.width(12.dp))

                    Switch(
                        checked = currentLang == "es",
                        onCheckedChange = { isChecked ->
                            val newLang = if (isChecked) "es" else "en"
                            scope.launch { prefs.setLanguage(newLang) }
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
    //signUpSideEffect // handle actual Firebase sign up + login
    LaunchedEffect(signUpRequested) {
        if (signUpRequested) {
            isLoading = true
            try {
                //registerFirebase // create user using Firebase Auth via AuthRepository
                AuthRepository.register(email, password)
                //autoLoginFirebase // immediately log in new user
                AuthRepository.login(email, password)

                //initUserStuff // set up per-user things (goals + log repository)
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    LogRepository.initForUser(uid)
                    val userPrefs = UserPreferences(context, uid)
                    //startWithZeroGoals // let Settings handle real values
                    userPrefs.setGoals(Goals())
                }
                Toast.makeText(
                    context,
                    translate(lang, "Registration Successful", "Registro exitoso"),
                    Toast.LENGTH_LONG
                ).show()

                error = null
                onSignUpSuccess()
            } catch (e: Exception) {
                error = translate(
                    lang,
                    e.message ?: "Registration Failed",
                    "Error en el registro"
                )
            } finally {
                signUpRequested = false
                isLoading = false
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpPreview() {
    MacrosProjectTheme {
        SignUpScreen(
            onSignUpSuccess = {},
            onBackToLogin = {}
        )
    }
}
