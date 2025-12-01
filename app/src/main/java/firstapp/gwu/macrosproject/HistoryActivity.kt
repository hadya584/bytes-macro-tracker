package firstapp.gwu.macrosproject

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import firstapp.gwu.macrosproject.data.DailyLog
import firstapp.gwu.macrosproject.data.FoodItem
import firstapp.gwu.macrosproject.data.Goals
import firstapp.gwu.macrosproject.data.UserPreferences
import firstapp.gwu.macrosproject.data.translate
import firstapp.gwu.macrosproject.ui.components.AppLogoHeader
import firstapp.gwu.macrosproject.ui.theme.BytesMint
import firstapp.gwu.macrosproject.ui.theme.BytesNavy
import firstapp.gwu.macrosproject.ui.theme.BytesPink
import firstapp.gwu.macrosproject.ui.theme.BytesPurple
import firstapp.gwu.macrosproject.ui.theme.MacrosProjectTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //pull date for this history screen from the Intent (sent by weekly/monthly history)
        //localDate docs: https://developer.android.com/reference/java/time/LocalDate
        val dateStr = intent.getStringExtra("date")
        val initialDate = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: LocalDate.now()

        setContent {
            MacrosProjectTheme {
                HistoryScreen(selectedDate = initialDate)
            }
        }
    }
}

@Composable
fun HistoryScreen(selectedDate: LocalDate) {
    val context = LocalContext.current
    val activity = context as? Activity
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid
    if (uid == null) {
        //no user logged in
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFffe6ee)),
            contentAlignment = Alignment.Center
        ) {
            Text("No user logged in", color = BytesNavy)
        }
        return
    }
    //per-user preferences
    val prefs = remember(uid) { UserPreferences(context, uid) }
    val lang by prefs.languageFlow.collectAsState(initial = "en")
    val goals by prefs.goalsFlow.collectAsState(initial = Goals())
    //state for the single DailyLog we load
    var dailyLog by remember { mutableStateOf<DailyLog?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val dateKey = selectedDate.toString()
    val dateLabel = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    LaunchedEffect(uid, dateKey) {
        isLoading = true
        db.collection("users")
            .document(uid)
            .collection("logs")
            .document(dateKey)
            .get()
            .addOnSuccessListener { snap ->
                dailyLog = snap.toObject(DailyLog::class.java)
                isLoading = false
            }
            .addOnFailureListener { e ->
                errorMessage = e.message
                isLoading = false
            }
    }
    //unwrap the list of FoodItem entries for this day
    val items: List<FoodItem> = dailyLog?.items.orEmpty()
    //sum up total macros for the day
    //https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sum-of.html
    val calories = items.sumOf { it.calories ?: 0.0 }
    val protein = items.sumOf { it.protein_g ?: 0.0 }
    val carbs = items.sumOf { it.carbohydrates_total_g ?: 0.0 }
    val fat = items.sumOf { it.fat_total_g ?: 0.0 }
    //flags to check if we went over our daily goals
    val proteinOver = goals.proteinG > 0 && protein > goals.proteinG
    val carbsOver = goals.carbsG > 0 && carbs > goals.carbsG
    val fatOver = goals.fatG > 0 && fat > goals.fatG
    val calOver = goals.calories > 0 && calories > goals.calories
    //helper to compute progress as a 0f..something value for the macro rings
    //allow values over 1f so rings can show "over goal" visually
    fun pct(current: Double, goal: Double) =
        if (goal <= 0.0) 0f else (current / goal).toFloat()
    val calProg = pct(calories, goals.calories)
    val pProg = pct(protein, goals.proteinG)
    val cProg = pct(carbs, goals.carbsG)
    val fProg = pct(fat, goals.fatG)
    Scaffold(containerColor = Color(0xFFffe6ee)) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(Color(0xFFffe6ee))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //app logo
            AppLogoHeader()
            //back arrow plus centered title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { activity?.finish() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = translate(lang, "Back", "Atrás"),
                        tint = BytesNavy
                    )
                }
                Text(
                    text = translate(lang, "LOG HISTORY", "Historia"),
                    style = MaterialTheme.typography.titleLarge,
                    color = BytesNavy,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
                //spacer to balance back arrow
                Spacer(modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(2.dp))
            //shows which day we are looking at
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = BytesNavy.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(10.dp))
            //loading spinner while we wait for Firestore
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BytesNavy)
                }
                return@Column
            }
            //ff we have no entries for this day, let the user know
            if (items.isEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = translate(
                        lang,
                        "No foods logged for this day.",
                        "No hay alimentos registrados para este día."
                    ),
                    color = BytesNavy
                )
                return@Column
            }
            //macro rings row, same as the DailyLog
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(110.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MacroRing(
                    label = translate(lang, "Protein", "Proteína"),
                    progress = pProg,
                    centerText = "${protein.toInt()}g / ${goals.proteinG.toInt()}g",
                    color = BytesPurple,
                    overLimit = proteinOver,
                    sizeDp = 85,
                    strokeDp = 14,
                    showPercentOnly = true
                )
                MacroRing(
                    label = translate(lang, "Carbs", "Carbohidratos"),
                    progress = cProg,
                    centerText = "${carbs.toInt()}g / ${goals.carbsG.toInt()}g",
                    color = BytesMint,
                    overLimit = carbsOver,
                    sizeDp = 85,
                    strokeDp = 14,
                    showPercentOnly = true
                )
                MacroRing(
                    label = translate(lang, "Fat", "Grasa"),
                    progress = fProg,
                    centerText = "${fat.toInt()}g / ${goals.fatG.toInt()}g",
                    color = BytesPink,
                    overLimit = fatOver,
                    sizeDp = 85,
                    strokeDp = 14,
                    showPercentOnly = true
                )
                MacroRing(
                    label = translate(lang, "Calories", "Calorías"),
                    progress = calProg,
                    centerText = "${calories.toInt()} / ${goals.calories.toInt()}",
                    color = BytesNavy,
                    overLimit = calOver,
                    sizeDp = 85,
                    strokeDp = 14,
                    showPercentOnly = true
                )
            }
            Spacer(Modifier.height(2.dp))
            //text labels under the rings for quick numbers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Protein",
                        color = BytesPurple,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${protein.toInt()}g / ${goals.proteinG.toInt()}g",
                        color = BytesPurple,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Carbs",
                        color = BytesMint,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${carbs.toInt()}g / ${goals.carbsG.toInt()}g",
                        color = BytesMint,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Fat",
                        color = BytesPink,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${fat.toInt()}g / ${goals.fatG.toInt()}g",
                        color = BytesPink,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Calories",
                        color = BytesNavy,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${calories.toInt()} / ${goals.calories.toInt()}",
                        color = BytesNavy,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            //scrollable list of all foods that were logged on this day
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { food ->
                    HistoryFoodRow(lang = lang, food = food)
                }
            }
            //show any Firestore error at the bottom
            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = translate(lang, "Error:", "Error:") + " $it",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun HistoryFoodRow(
    lang: String,
    food: FoodItem
) {
    //if servings somehow comes back null, treat it as 1.0
    val servings = food.servings ?: 1.0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF1F4)
        ),
        border = BorderStroke(3.dp, BytesNavy),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            //food name
            Text(
                food.name ?: translate(lang, "Unknown", "Desconocido"),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = BytesNavy
            )
            //base serving size line
            food.servingSize?.let { size ->
                val unit = food.servingUnit ?: ""
                Spacer(Modifier.height(2.dp))
                Text(
                    translate(lang, "Base serving:", "Porción base:") + " $size $unit",
                    color = BytesNavy.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            //how many servings were logged for this entry
            Text(
                translate(lang, "Servings:", "Porciones:") + " $servings",
                color = BytesNavy,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(6.dp))
            //one-line macros row like "Cal 120  P 10g  C 20g  F 3g"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Cal ${(food.calories ?: 0.0).toInt()}",
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
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    MacrosProjectTheme {
        HistoryScreen(selectedDate = LocalDate.now())
    }
}
