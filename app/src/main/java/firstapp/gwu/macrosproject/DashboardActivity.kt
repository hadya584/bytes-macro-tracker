package firstapp.gwu.macrosproject

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
    ) {
        val context = LocalContext.current
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            //no user logged in, simple fallback
            Text("No user logged in")
            return
        }
        //per-user prefs (goals + language)
        //stateflow + compose: https://developer.android.com/jetpack/compose/state#flows
        val prefs = remember(uid) { UserPreferences(context, uid) }
        val goals by prefs.goalsFlow.collectAsState(initial = Goals())
        val lang by prefs.languageFlow.collectAsState(initial = "en")
        //live list of foods logged this session
        val loggedFoods by LogRepository.loggedFoods.collectAsState()
        //sum up totals
        val calories = loggedFoods.sumOf { it.calories ?: 0.0 }
        val protein  = loggedFoods.sumOf { it.protein_g ?: 0.0 }
        val carbs    = loggedFoods.sumOf { it.carbohydrates_total_g ?: 0.0 }
        val fat      = loggedFoods.sumOf { it.fat_total_g ?: 0.0 }
        //over-goal flags
        val proteinOver = goals.proteinG > 0 && protein  > goals.proteinG
        val carbsOver   = goals.carbsG   > 0 && carbs    > goals.carbsG
        val fatOver     = goals.fatG     > 0 && fat      > goals.fatG
        val calOver     = goals.calories > 0 && calories > goals.calories
        //helper for progress ratio (can be >1f)
        fun pct(current: Double, goal: Double) =
            if (goal <= 0.0) 0f else (current / goal).toFloat()
        val calProg = pct(calories, goals.calories)
        val pProg   = pct(protein,  goals.proteinG)
        val cProg   = pct(carbs,    goals.carbsG)
        val fProg   = pct(fat,      goals.fatG)
        //pretty date label like "saturday, nov 29"
        val todayLabel = remember {
            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
        }
        Scaffold(
            containerColor = Color(0xFFffe6ee)
        ) { pad ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .background(Color(0xFFffe6ee))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                //app logo at top
                AppLogoHeader()
                Text(
                    text = translate(lang, "Today's Progress", "Progreso de hoy"),
                    style = MaterialTheme.typography.titleLarge,
                    color = BytesNavy
                )
                //date under title
                Text(
                    text = todayLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BytesNavy.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(16.dp))
                //card showing macro rings, clickable to go to daily log
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 4.dp,
                            color = BytesNavy,
                            shape = MaterialTheme.shapes.large
                        )
                        .shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFFFF1F4))
                        .clickable {
                            context.startActivity(
                                Intent(context, DailyLogActivity::class.java)
                            )
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F4))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MacroRing(
                                label = translate(lang, "Protein", "Proteína"),
                                progress = pProg,
                                centerText = "${protein.toInt()}g / ${goals.proteinG.toInt()}g",
                                color = BytesPurple,
                                overLimit = proteinOver
                            )
                            MacroRing(
                                label = translate(lang, "Carbs", "Carbohidratos"),
                                progress = cProg,
                                centerText = "${carbs.toInt()}g / ${goals.carbsG.toInt()}g",
                                color = BytesMint,
                                overLimit = carbsOver
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MacroRing(
                                label = translate(lang, "Fat", "Grasa"),
                                progress = fProg,
                                centerText = "${fat.toInt()}g / ${goals.fatG.toInt()}g",
                                color = BytesPink,
                                overLimit = fatOver
                            )
                            MacroRing(
                                label = translate(lang, "Calories", "Calorías"),
                                progress = calProg,
                                centerText = "${calories.toInt()} / ${goals.calories.toInt()}",
                                color = BytesNavy,
                                overLimit = calOver
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                //main action buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ActionButton(
                        text = translate(lang, "LOG FOODS", "BUSCAR COMIDA"),
                        onClick = {
                            context.startActivity(Intent(context, FoodSearchActivity::class.java))
                        }
                    )
                    ActionButton(
                        text = translate(lang, "DAILY LOG", "VER REGISTRO DIARIO"),
                        onClick = {
                            context.startActivity(Intent(context, DailyLogActivity::class.java))
                        }
                    )
                    ActionButton(
                        text = translate(lang, "HISTORY", "HISTORIA"),
                        onClick = {
                            context.startActivity(Intent(context, WeeklyHistoryActivity::class.java))
                        }
                    )
                    ActionButton(
                        text = translate(lang, "SETTINGS", "CONFIGURACION"),
                        onClick = {
                            context.startActivity(Intent(context, Settings::class.java))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(40.dp),
        border = BorderStroke(3.dp, Color(0xFF22235F)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFFFFF1F4),
            contentColor = Color(0xFF22235F)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )
        )
    }
}

@Composable
fun MacroRing(
    label: String,
    progress: Float,              // can be > 1f
    centerText: String,
    color: Color,
    overLimit: Boolean = false,
    outlineColor: Color = Color.Red.copy(alpha = 0.6f), // unused now
    sizeDp: Int = 130,
    strokeDp: Int = 18,
    showPercentOnly: Boolean = false
) {
    val density = LocalDensity.current
    val sizeDpVal = sizeDp.dp
    val strokeDpVal = strokeDp.dp
    val strokePx = with(density) { strokeDpVal.toPx() }
    Box(
        modifier = Modifier.size(sizeDpVal),
        contentAlignment = Alignment.Center
    ) {
        //ring drawing using compose canvas
        //canvas docs: https://developer.android.com/jetpack/compose/graphics/draw/overview
        Canvas(modifier = Modifier.size(sizeDpVal)) {
            val ringDiameter = size.minDimension - strokePx
            val offset = Offset(
                (size.width - ringDiameter) / 2f,
                (size.height - ringDiameter) / 2f
            )
            //base track
            drawArc(
                color = Color.LightGray.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = offset,
                size = Size(ringDiameter, ringDiameter),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            //main progress arc
            val effectiveProgress = progress.coerceAtLeast(0f)
            val arcProgress = effectiveProgress.coerceAtMost(1f)
            val sweep = 360f * arcProgress
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = offset,
                size = Size(ringDiameter, ringDiameter),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            //small glow at the tip, wraps if > 1f
            //brush.radialgradient: https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/Brush#radialgradient
            if (effectiveProgress > 0f) {
                val glowProgress = if (effectiveProgress <= 1f) {
                    arcProgress
                } else {
                    (effectiveProgress % 1f).let { if (it == 0f) 1f else it }
                }
                val radius = ringDiameter / 2f
                val center = this.center
                val angleDeg = -90f + 360f * glowProgress
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val tipX = center.x + (radius * kotlin.math.cos(angleRad)).toFloat()
                val tipY = center.y + (radius * kotlin.math.sin(angleRad)).toFloat()
                val tip = Offset(tipX, tipY)
                val glowRadius = strokePx * 1.2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.9f), Color.Transparent),
                        center = tip,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = tip
                )
            }
        }
        //center text (percent + label)
        val percent = (progress * 100).toInt()
        val isOver = percent > 100 || overLimit
        val percentColor = if (isOver) BytesPink else BytesNavy
        if (showPercentOnly) {
            Text(
                text = "$percent%",
                fontWeight = FontWeight.ExtraBold,
                color = percentColor,
                style = MaterialTheme.typography.titleMedium
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$percent%",
                    fontWeight = FontWeight.Bold,
                    color = percentColor
                )
                Text(centerText, style = MaterialTheme.typography.bodySmall)
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    MacrosProjectTheme {
        Dashboard()
    }
}
