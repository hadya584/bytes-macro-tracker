package firstapp.gwu.macrosproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import firstapp.gwu.macrosproject.ui.theme.MacrosProjectTheme
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.edit

import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        val app=FirebaseApp.getInstance()
        Log.d("FBINit", "Firebase initialized: ${app.name}")
        enableEdgeToEdge()
        setContent {
            MacrosProjectTheme {
                val context = LocalContext.current
                LoginApp(onLoginSuccess = {

                    val intent = Intent(context, DashboardActivity::class.java)
                    context.startActivity(intent)


                })
            }
        }
    }
}

@Composable
fun LoginApp(onLoginSuccess: ()-> Unit) {

    val context = LocalContext.current

    var password by remember { mutableStateOf("") }


    val prefs = remember { context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE) }


    var username by remember {
        mutableStateOf(prefs.getString("username", "") ?: "Enter your username")
    }

    //new variables
    var error by remember { mutableStateOf<String?>(null) }
    var loginRequested by remember { mutableStateOf(false) }
    var registerRequested by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        // verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Text(text = "WELCOME")
        Spacer(Modifier.height(70.dp))

        //username
        TextField(
            value = username,
            onValueChange = { newText -> username = newText },
            label = { Text("Enter your Email") },
            singleLine = true
        )
        Spacer(Modifier.height(70.dp))
        //password
        TextField(
            value = password,
            onValueChange = { newText ->
                password = newText
            },
            label = { Text("Enter your password") },
            visualTransformation = PasswordVisualTransformation()
        )
        //button
        Spacer(Modifier.height(70.dp))

        Button(
            onClick = {
                loginRequested = true
                prefs.edit { putString("username", username) }
            },
            enabled = checkUsernamePassword(username, password)
        )
        {
            Text("Login")
        }

        Spacer(Modifier.height(70.dp))

        Button(
            onClick = {
                registerRequested = true
            },
            Modifier.fillMaxWidth(),
            enabled = checkUsernamePassword(username, password)
        )
        {
            Text("Register")
        }

        //new loading indicator and error reporting
        if(isLoading) {
            CircularProgressIndicator()
        }
        error?.let{
            Spacer(Modifier.height(8.dp))
            Text(it, color=Color.Red)
        }


    }
    LaunchedEffect(loginRequested) {
        if(loginRequested) {
            isLoading=true
            try {
                AuthRepository.login(username, password)
                error = null
                onLoginSuccess()
            } catch(e: Exception) {
                error = e.message ?: "Login Failed"
            } finally {
                loginRequested = false
                isLoading = false
            }

        }
    }
    LaunchedEffect(registerRequested) {
        if(registerRequested) {
            isLoading=true
            try {
                AuthRepository.register(username, password)
                // Automatically log them in after registering
                AuthRepository.login(username, password)
                Toast.makeText(context, "Registration Successful",Toast.LENGTH_LONG).show()
                println("registration successful")
                error = null
                onLoginSuccess()
            } catch(e: Exception) {
                error = e.message ?: "Registration Failed"
            } finally {
                registerRequested = false
                isLoading = false
            }
        }
    }
}

fun checkUsernamePassword(username: String, password: String): Boolean {
    return username.isNotBlank() && password.isNotBlank()
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    val context = LocalContext.current
    MacrosProjectTheme {

    }
}