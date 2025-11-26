package firstapp.gwu.macrosproject

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import firstapp.gwu.macrosproject.data.Goals
import firstapp.gwu.macrosproject.data.UserPreferences
import firstapp.gwu.macrosproject.ui.theme.MacrosProjectTheme

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MacrosProjectTheme {
                Dashboard()
            }
        }
    }
}

@Composable
fun Dashboard() {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }

    // Goals from DataStore (defaults if not yet saved)
    val goals by prefs.goalsFlow.collectAsState(initial = Goals())

    // TODO: replace these with Firebase totals for *today*
    var calories by remember { mutableStateOf(840.0) }
    var protein  by remember { mutableStateOf(55.0) }
    var carbs    by remember { mutableStateOf(95.0) }
    var fat      by remember { mutableStateOf(22.0) }

    fun pct(current: Double, goal: Double) =
        if (goal <= 0.0) 0f else (current / goal).toFloat().coerceIn(0f, 1f)

    val calProg = pct(calories, goals.calories)
    val pProg   = pct(protein,  goals.proteinG)
    val cProg   = pct(carbs,    goals.carbsG)
    val fProg   = pct(fat,      goals.fatG)

    Scaffold { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Today's Progress", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroRing(
                    label = "Protein",
                    progress = pProg,
                    centerText = "${protein.toInt()}g / ${goals.proteinG.toInt()}g",
                    color = Color(0xFF8E44AD)
                )
                MacroRing(
                    label = "Carbs",
                    progress = cProg,
                    centerText = "${carbs.toInt()}g / ${goals.carbsG.toInt()}g",
                    color = Color(0xFF1E88E5)
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroRing(
                    label = "Fat",
                    progress = fProg,
                    centerText = "${fat.toInt()}g / ${goals.fatG.toInt()}g",
                    color = Color(0xFFFF9800)
                )
                MacroRing(
                    label = "Calories",
                    progress = calProg,
                    centerText = "${calories.toInt()} / ${goals.calories.toInt()}",
                    color = Color(0xFFE53935)
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(onClick = {
                context.startActivity(Intent(context, FoodSearchActivity::class.java))
            }) { Text("Search Foods") }

            Spacer(Modifier.height(8.dp))

            Button(onClick = {
                context.startActivity(Intent(context, Settings::class.java))
            }) { Text("Settings") }
        }
    }
}

/** Rename to avoid colliding with any ProgressRing in ui.theme */
@Composable
fun MacroRing(
    label: String,
    progress: Float,              // 0f..1f
    centerText: String,
    color: Color,
    sizeDp: Int = 130,
    strokeDp: Int = 18
) {
    val sizeDpVal = sizeDp.dp
    val strokeDpVal = strokeDp.dp
    val strokePx = with(androidx.compose.ui.platform.LocalDensity.current) { strokeDpVal.toPx() }

    Box(
        modifier = Modifier.size(sizeDpVal),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDpVal)) {
            val ringDiameterPx = sizeDpVal.toPx() - strokePx
            val offset = Offset(
                (this.size.width - ringDiameterPx) / 2f,
                (this.size.height - ringDiameterPx) / 2f
            )
            // track
            drawArc(
                color = Color.LightGray.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = offset,
                size = Size(ringDiameterPx, ringDiameterPx),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            // progress
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = offset,
                size = Size(ringDiameterPx, ringDiameterPx),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold)
            Text(centerText, style = MaterialTheme.typography.bodySmall)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}
