package firstapp.gwu.macrosproject

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import firstapp.gwu.macrosproject.data.ApiClient
import firstapp.gwu.macrosproject.data.FoodItem
import firstapp.gwu.macrosproject.data.LogRepository
import firstapp.gwu.macrosproject.data.UserPreferences
import firstapp.gwu.macrosproject.data.translate
import firstapp.gwu.macrosproject.ui.theme.MacrosProjectTheme
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import firstapp.gwu.macrosproject.ui.components.AppLogoHeader
import firstapp.gwu.macrosproject.ui.theme.BytesNavy
import firstapp.gwu.macrosproject.ui.theme.BytesMint
import firstapp.gwu.macrosproject.ui.theme.BytesPink
import firstapp.gwu.macrosproject.ui.theme.BytesPurple
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon

class FoodSearchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MacrosProjectTheme {
                FoodSearchScreen()
            }
        }
    }
}

@Composable
fun FoodSearchScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    //language from datastore
    //datastore + compose: https://developer.android.com/topic/libraries/architecture/datastore
    val prefsData = remember { UserPreferences(context) }
    val lang by prefsData.languageFlow.collectAsState(initial = "en")
    //usda api key injected at build time from local.properties (see app/build.gradle.kts)
    val apiKey = BuildConfig.USDA_API_KEY
    var query by remember { mutableStateOf("") }
    var foodList by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    //live search with debounce using launchedEffect + delay
    //launchedeffect: https://developer.android.com/jetpack/compose/side-effects#launchedeffect
    //delay: https://kotlinlang.org/docs/coroutines-basics.html#delays
    LaunchedEffect(query, apiKey) {
        errorMessage = null
        val trimmed = query.trim()
        if (apiKey.isBlank()) {
            foodList = emptyList()
            isLoading = false
            return@LaunchedEffect
        }
        if (trimmed.length < 3) {
            foodList = emptyList()
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        delay(400)
        try {
            val result = getMatchingFoods(trimmed, apiKey)
            foodList = result
            if (result.isEmpty()) {
                errorMessage = translate(
                    lang,
                    "No matches found",
                    "No se encontraron resultados"
                )
            }
        } catch (e: Exception) {
            val base = e.message ?: "Unknown error"
            errorMessage = translate(
                lang,
                "Failed: $base",
                "Error: $base"
            )
        } finally {
            isLoading = false
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFffe6ee))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //app logo at top
        AppLogoHeader()
        //back arrow + centered title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { activity?.finish() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = translate(lang, "Back", "Atrás"),
                    tint = BytesNavy
                )
            }
            Text(
                translate(lang, "SEARCH FOOD", "Buscar comida"),
                style = MaterialTheme.typography.titleLarge,
                color = BytesNavy,
                modifier = Modifier
                    .weight(1f)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
            //spacer to balance back arrow
            Spacer(modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(8.dp))
        //underlined textfield
        TextField(
            value = query,
            onValueChange = { query = it },
            label = {
                Text(
                    translate(lang, "Enter food...", "Ingresa el nombre del alimento"),
                    color = BytesNavy.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFffe6ee)),
            singleLine = true,
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
        errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                color = BytesPink,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(16.dp))
        if (isLoading) {
            //simple loading spinner
            CircularProgressIndicator(color = BytesNavy)
        } else {
            //search results list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                items(foodList) { food ->
                    FoodCard(
                        lang = lang,
                        food = food,
                        onLog = { servings ->
                            LogRepository.addFood(food, servings)
                            Toast.makeText(
                                context,
                                translate(
                                    lang,
                                    "Logged ${servings}x ${food.name ?: "food"}",
                                    "Registrado ${servings}x ${food.name ?: "alimento"}"
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}

//usda-backed search using retrofit api client
//retrofit basics: https://square.github.io/retrofit/
suspend fun getMatchingFoods(term: String, apiKey: String): List<FoodItem> {
    val query = term.trim()
    if (query.isEmpty()) return emptyList()
    val response = ApiClient.instance.searchFoods(query, apiKey)
    if (!response.isSuccessful) return emptyList()
    val foods = response.body()?.foods ?: emptyList()
    return foods.map { food ->
        val nutrients = food.foodNutrients.orEmpty()
        val calories = nutrients.find { it.nutrientName?.contains("Energy", true) == true }?.value
        val protein = nutrients.find { it.nutrientName?.contains("Protein", true) == true }?.value
        val carbs = nutrients.find { it.nutrientName?.contains("Carbohydrate", true) == true }?.value
        val fat = nutrients.find { it.nutrientName?.contains("Total lipid", true) == true }?.value
        FoodItem(
            name = food.description ?: "Unknown",
            calories = calories ?: 0.0,
            protein_g = protein ?: 0.0,
            carbohydrates_total_g = carbs ?: 0.0,
            fat_total_g = fat ?: 0.0,
            servingSize = food.servingSize,
            servingUnit = food.servingSizeUnit,
            servings = 1.0
        )
    }
}

@Composable
fun FoodCard(
    lang: String,
    food: FoodItem,
    onLog: (Double) -> Unit
) {
    var servingsText by remember { mutableStateOf("1.0") }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF1F4)
        ),
        border = BorderStroke(3.dp, BytesNavy),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            //food name
            Text(
                food.name ?: "Unknown",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = BytesNavy
            )
            //serving size if available
            food.servingSize?.let { size ->
                val unit = food.servingUnit ?: ""
                Spacer(Modifier.height(2.dp))
                Text(
                    translate(lang, "Serving:", "Porción:") + " $size $unit",
                    color = BytesNavy.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(6.dp))
            //one-line, color-coded macros
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Cal ${food.calories?.toInt() ?: 0}",
                    color = BytesNavy,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "P ${food.protein_g ?: 0.0}g",
                    color = BytesPurple,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "C ${food.carbohydrates_total_g ?: 0.0}g",
                    color = BytesMint,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "F ${food.fat_total_g ?: 0.0}g",
                    color = BytesPink,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                //servings textfield
                TextField(
                    value = servingsText,
                    onValueChange = { servingsText = it },
                    label = {
                        Text(
                            translate(lang, "Servings", "Porciones"),
                            color = BytesNavy.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
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
                Spacer(Modifier.width(8.dp))
                //circular "+" button to log servings
                //outlinedbutton docs: https://developer.android.com/reference/kotlin/androidx/compose/material3/OutlinedButton
                OutlinedButton(
                    onClick = {
                        val s = servingsText.toDoubleOrNull() ?: 1.0
                        onLog(s.coerceAtLeast(0.0))
                    },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    border = BorderStroke(2.dp, BytesNavy),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = BytesNavy
                    ),
                    modifier = Modifier.size(42.dp)
                ) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FoodSearchScreenPreview() {
    MacrosProjectTheme {
        FoodSearchScreen()
    }
}
