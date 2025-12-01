package firstapp.gwu.macrosproject

import android.app.Activity
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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
import firstapp.gwu.macrosproject.data.FoodItem
import firstapp.gwu.macrosproject.data.Goals
import firstapp.gwu.macrosproject.data.LogRepository
import firstapp.gwu.macrosproject.data.UserPreferences
import firstapp.gwu.macrosproject.data.translate
import firstapp.gwu.macrosproject.ui.components.AppLogoHeader
import firstapp.gwu.macrosproject.ui.theme.BytesMint
import firstapp.gwu.macrosproject.ui.theme.BytesNavy
import firstapp.gwu.macrosproject.ui.theme.BytesPink
import firstapp.gwu.macrosproject.ui.theme.BytesPurple
import firstapp.gwu.macrosproject.ui.theme.MacrosProjectTheme
import java.util.Date

class DailyLogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MacrosProjectTheme {
                DailyLogScreen()
            }
        }
    }
}

@Composable
fun DailyLogScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    //per-user prefs
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    if (uid == null) {
        Text("No user logged in")
        return
    }
    val prefs = remember(uid) { UserPreferences(context, uid) }
    val lang by prefs.languageFlow.collectAsState(initial = "en")
    val goals by prefs.goalsFlow.collectAsState(initial = Goals())
    //live list of foods for today
    val loggedFoods by LogRepository.loggedFoods.collectAsState()
    //totals for the day
    val calories = loggedFoods.sumOf { it.calories ?: 0.0 }
    val protein  = loggedFoods.sumOf { it.protein_g ?: 0.0 }
    val carbs    = loggedFoods.sumOf { it.carbohydrates_total_g ?: 0.0 }
    val fat      = loggedFoods.sumOf { it.fat_total_g ?: 0.0 }
    //check if over goals
    val proteinOver = goals.proteinG > 0 && protein  > goals.proteinG
    val carbsOver   = goals.carbsG   > 0 && carbs    > goals.carbsG
    val fatOver     = goals.fatG     > 0 && fat      > goals.fatG
    val calOver     = goals.calories > 0 && calories > goals.calories
    //helper for progress ratio
    fun pct(current: Double, goal: Double) =
        if (goal <= 0.0) 0f else (current / goal).toFloat()

    val calProg = pct(calories, goals.calories)
    val pProg   = pct(protein,  goals.proteinG)
    val cProg   = pct(carbs,    goals.carbsG)
    val fProg   = pct(fat,      goals.fatG)
    //date label compatible with minsdk 24
    //dateformat docs: https://developer.android.com/reference/android/text/format/DateFormat
    val todayLabel = remember {
        DateFormat.format("EEEE, MMM d", Date()).toString()
    }

    Scaffold(containerColor = Color(0xFFffe6ee)) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(Color(0xFFffe6ee))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //logo
            AppLogoHeader()
            //back arrow + centered title
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { activity?.finish() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = translate(lang, "Back", "Atrás"),
                        tint = BytesNavy
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    translate(lang, "TODAY'S LOG", "Registro de hoy"),
                    style = MaterialTheme.typography.titleLarge,
                    color = BytesNavy,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.width(48.dp)) //balances arrow width
            }
            Spacer(Modifier.height(0.dp))
            Text(
                text = todayLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = BytesNavy.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(6.dp))
            //single row of macro rings for today
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
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
            if (loggedFoods.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    translate(
                        lang,
                        "No foods logged yet today.",
                        "Todavía no hay alimentos registrados hoy."
                    ),
                    color = BytesNavy
                )
            } else {
                Spacer(Modifier.height(6.dp))
                //scrollable list of logged foods
                //lazycolumn docs: https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyColumn
                LazyColumn(
                    modifier = Modifier
                        .weight(1.8f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(loggedFoods) { index, item ->
                        LoggedFoodRow(
                            lang = lang,
                            food = item,
                            onUpdateServings = { newServings ->
                                LogRepository.updateFoodServings(index, newServings)
                            },
                            onDelete = {
                                LogRepository.removeFood(index)
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            //clear all logged foods for today
            OutlinedButton(
                onClick = { LogRepository.clearToday() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 8.dp)
                    .height(52.dp),
                border = BorderStroke(3.dp, BytesPink),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFFFF1F4),
                    contentColor = BytesPink
                )
            ) {
                Text(
                    translate(
                        lang,
                        "Clear Today's Log",
                        "Borrar el registro de hoy"
                    ),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}




@Composable
private fun LoggedFoodRow(
    lang: String,
    food: FoodItem,
    onUpdateServings: (Double) -> Unit,
    onDelete: () -> Unit
) {
    var servingsText by remember { mutableStateOf((food.servings ?: 1.0).toString()) }
    val currentServings = food.servings ?: 1.0
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
                food.name ?: translate(lang, "Unknown", "Desconocido"),
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
                    translate(lang, "Serving Size:", "Porción:") + " $size $unit",
                    color = BytesNavy.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(6.dp))
            //one-line macros summary
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
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                //servings textfield (same underline style as other screens)
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
                //check button to update servings
                OutlinedIconButton(
                    onClick = {
                        val s = servingsText.toDoubleOrNull() ?: currentServings
                        onUpdateServings(s.coerceAtLeast(0.0))
                    },
                    border = BorderStroke(2.dp, BytesMint),
                    colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = BytesMint)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Update")
                }
                //trash button to delete item
                OutlinedIconButton(
                    onClick = onDelete,
                    border = BorderStroke(2.dp, BytesPink),
                    colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = BytesPink)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
private fun DailyLogScreenPreview() {
    MacrosProjectTheme {
        DailyLogScreen()
    }
}
