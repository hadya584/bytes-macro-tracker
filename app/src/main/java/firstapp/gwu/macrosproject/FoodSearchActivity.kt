package firstapp.gwu.macrosproject

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
import com.google.android.gms.common.wrappers.Wrappers.packageManager
import firstapp.gwu.macrosproject.data.ApiClient
import firstapp.gwu.macrosproject.data.FoodItem
import android.content.Context
import android.content.pm.PackageManager
import firstapp.gwu.macrosproject.ui.theme.MacrosProjectTheme
import kotlinx.coroutines.launch

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

fun Context.readCaloriesNinjaKey(): String {
    val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    return ai.metaData?.getString("CALORIE_NINJA_KEY", "") ?: ""
}

@Composable
fun FoodSearchScreen() {
    val context = LocalContext.current
    val apiKey = remember { context.readCaloriesNinjaKey() }

    var query by remember { mutableStateOf("") }
    var foodList by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Search Food", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        TextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Enter food name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (apiKey.isBlank()) {
                    Toast.makeText(context, "API key missing", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (query.isNotBlank()) {
                    isLoading = true
                    scope.launch {
                        try {
                            foodList = getMatchingFoods(query.trim(), apiKey)
                            if (foodList.isEmpty()) {
                                Toast.makeText(context, "No matches found", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            enabled = !isLoading
        ) { Text("Search") }

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(foodList) { food -> FoodCard(food) }
            }
        }
    }
}

/**
 * Tries a few query variants (exact, plural, singular) and returns
 * items whose name CONTAINS the original search term (case-insensitive).
 */
suspend fun getMatchingFoods(term: String, apiKey: String): List<FoodItem> {
    //generate variants for search
    val variants = buildList {
        add(term)
        if (!term.endsWith("s", ignoreCase = true)) add("${term}s")
        if (term.endsWith("es", ignoreCase = true)) add(term.dropLast(2))
        if (term.endsWith("s", ignoreCase = true)) add(term.dropLast(1))
    }.distinct()
    //call api for each word containing the search term
    val all = mutableListOf<FoodItem>()
    for (q in variants) {
        val resp = ApiClient.instance.getFoodInfo(q, apiKey)
        if (resp.isSuccessful) {
            resp.body()?.items?.let { all += it }
        }
    }
    //de-dup by name
    val deduped = all
        .filter { it.name != null }
        .distinctBy { it.name!!.lowercase() }
    val filtered = deduped.filter { it.name!!.contains(term, ignoreCase = true) }
    //if the filter is too strict it goes back to dedup
    return if (filtered.isNotEmpty()) filtered else deduped
}


@Composable
fun FoodCard(food: FoodItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(food.name ?: "Unknown", style = MaterialTheme.typography.titleMedium)
            Text("Calories: ${food.calories ?: 0}")
            Text("Protein: ${food.protein_g ?: 0} g")
            Text("Carbs: ${food.carbohydrates_total_g ?: 0} g")
            Text("Fat: ${food.fat_total_g ?: 0} g")
        }
    }
}
