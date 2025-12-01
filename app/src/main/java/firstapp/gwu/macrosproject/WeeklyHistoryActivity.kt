package firstapp.gwu.macrosproject

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import firstapp.gwu.macrosproject.data.DailyLog
import firstapp.gwu.macrosproject.data.Goals
import firstapp.gwu.macrosproject.data.UserPreferences
import firstapp.gwu.macrosproject.data.translate
import firstapp.gwu.macrosproject.ui.components.AppLogoHeader
import firstapp.gwu.macrosproject.ui.theme.BytesMint
import firstapp.gwu.macrosproject.ui.theme.BytesNavy
import firstapp.gwu.macrosproject.ui.theme.BytesPink
import firstapp.gwu.macrosproject.ui.theme.BytesPurple
import firstapp.gwu.macrosproject.ui.theme.MacrosProjectTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

//Changed from a weekly history view to a monthly calandar
class WeeklyHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MacrosProjectTheme {
                MonthlyHistoryScreen()
            }
        }
    }
}
@Composable
fun MonthlyHistoryScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    //firebase auth and firestore
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid
    //in case we landed here with no user
    if (uid == null) {
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
    //peruser preferences so goals and language are tied to uid
    val prefs = remember(uid) { UserPreferences(context, uid) }
    val lang by prefs.languageFlow.collectAsState(initial = "en")
    val goals by prefs.goalsFlow.collectAsState(initial = Goals())
    //all logs keyed by LocalDate
    var logsByDate by remember { mutableStateOf<Map<LocalDate, DailyLog>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    //month currently shown in the calendar
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    //loadall logs for this user from firestore
    LaunchedEffect(uid) {
        db.collection("users")
            .document(uid)
            .collection("logs")
            .get()
            .addOnSuccessListener { snap ->
                val map = mutableMapOf<LocalDate, DailyLog>()
                for (doc in snap.documents) {
                    val id = doc.id   //yyyyMMdd
                    runCatching {
                        val date = LocalDate.parse(id)
                        val log = doc.toObject(DailyLog::class.java)
                        if (log != null) map[date] = log
                    }
                }
                logsByDate = map
                isLoading = false
            }
            .addOnFailureListener { e ->
                errorMessage = e.message
                isLoading = false
            }
    }
    //precompute weeks for the current month (each week = list of 7 days sun→sat)
    val weeks: List<List<LocalDate>> = remember(logsByDate, currentMonth) {
        val firstOfMonth = currentMonth.atDay(1)
        val lastOfMonth = currentMonth.atEndOfMonth()
        var start = firstOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val end = lastOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        val result = mutableListOf<List<LocalDate>>()
        while (!start.isAfter(end)) {
            val weekDays = (0..6).map { offset -> start.plusDays(offset.toLong()) }
            result += weekDays
            start = start.plusWeeks(1)
        }
        result
    }
    //calandar view reference: Example of creating a monthly calendar UI in Compose using custom layout logic (weeks, day cells, navigation between months)
    //https://androidkaleempatel.medium.com/creating-a-monthly-calendar-view-with-event-indicators-in-jetpack-compose-4dffcf0359ca
    //https://github.com/kizitonwose/Calendar
    Scaffold(containerColor = Color(0xFFffe6ee)) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(Color(0xFFffe6ee))
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            //applogo at top
            AppLogoHeader()
            //header row with back arrow and title
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
                    text = translate(lang, "HISTORY", "Historia"),
                    style = MaterialTheme.typography.titleLarge,
                    color = BytesNavy,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
                //empty spacer to balance back arrow
                Spacer(modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(6.dp))
            //monthselector with arrow buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Text("‹", color = BytesNavy, style = MaterialTheme.typography.headlineMedium)
                }
                val locale = if (lang == "es") Locale("es") else Locale.getDefault()
                val monthName = currentMonth.month.getDisplayName(
                    TextStyle.FULL,
                    locale
                )
                Text(
                    text = "$monthName ${currentMonth.year}",
                    style = MaterialTheme.typography.titleMedium,
                    color = BytesNavy,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Text("›", color = BytesNavy, style = MaterialTheme.typography.headlineMedium)
                }
            }
            Spacer(Modifier.height(10.dp))
            //weekdaylabels row (sun→sat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                //lang translations
                val labelsEn = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                val labelsEs = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
                val labels = if (lang == "es") labelsEs else labelsEn
                labels.forEach { day ->
                    Text(
                        day,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = BytesNavy
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            //loading circle
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BytesNavy)
                }
            } else {
                //calendar grid one week per row
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(weeks) { weekDays ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            weekDays.forEach { date ->
                                val log = logsByDate[date]
                                DayTile(
                                    lang = lang,
                                    date = date,
                                    log = log,
                                    goals = goals,
                                    inCurrentMonth = (YearMonth.from(date) == currentMonth),
                                    onClick = {
                                        if (log != null) {
                                            //navigate to perday history screen
                                            val intent =
                                                Intent(context, HistoryActivity::class.java)
                                            intent.putExtra("date", date.toString())
                                            context.startActivity(intent)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            //error message for firestore
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
//Reference: Explanation of using Compose’s Canvas / DrawScope and layering (drawing backgrounds, progress arcs, circles) to build custom UI components
@Composable
private fun DayTile(
    lang: String,
    date: LocalDate,
    log: DailyLog?,
    goals: Goals,
    inCurrentMonth: Boolean,
    onClick: () -> Unit
) {
    //flag for whether this day actually has any logged foods
    val hasLog = log != null
    val items = log?.items.orEmpty()
    //totals for this specific day
    val calories = items.sumOf { it.calories ?: 0.0 }
    val protein = items.sumOf { it.protein_g ?: 0.0 }
    val carbs = items.sumOf { it.carbohydrates_total_g ?: 0.0 }
    val fat = items.sumOf { it.fat_total_g ?: 0.0 }
    //percentage helper 0f..1f
    fun pct(current: Double, goal: Double) =
        if (goal <= 0.0) 0f else (current / goal).toFloat().coerceIn(0f, 1f)
    //progressvalues for mini rings
    val calProg = pct(calories, goals.calories)
    val pProg = pct(protein, goals.proteinG)
    val cProg = pct(carbs, goals.carbsG)
    val fProg = pct(fat, goals.fatG)
    //dim text if not part of current month
    val dayTextColor =
        if (inCurrentMonth) BytesNavy
        else BytesNavy.copy(alpha = 0.35f)
    Column(
        modifier = Modifier
            .width(50.dp)
            .padding(vertical = 1.dp, horizontal = 1.dp)
            .clickable(enabled = hasLog, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //daynumber
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = dayTextColor,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        if (hasLog) {
            //wrapper so we can draw glow behind the ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(44.dp)
            ) {
                //highlighttoday with soft pink circle
                if (date == LocalDate.now()) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(color = BytesPink.copy(alpha = 0.25f))
                    }
                }
                //little multiring that shows all four macros
                MiniMultiRing(
                    proteinProg = pProg,
                    carbsProg = cProg,
                    fatProg = fProg,
                    calProg = calProg
                )
            }
        } else {
            //emptyday placeholder circle
            Canvas(modifier = Modifier.size(22.dp)) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = size.minDimension / 2f
                )
            }
        }
    }
}

//Reference: Using a custom circular progress/“ring” indicator via Canvas + drawArc in Jetpack Compose
//Reference: https://medium.com/@esthcarelle/circular-progress-bar-with-jetpack-compose-a-step-by-step-guide-851e75fd06e2
//https://medium.com/@kemal_codes/jetpack-compose-canvas-10-practical-examples-532d40ed3dd0
//https://blog.droidchef.dev/custom-progress-with-jetpack-compose-tutorial/
@Composable
private fun MiniMultiRing(
    proteinProg: Float,
    carbsProg: Float,
    fatProg: Float,
    calProg: Float
) {
    //overallsize and strokewidth for the mini calendar rings
    val sizeDp = 40.dp
    val strokeDp = 4.dp
    val spacingPx = with(LocalDensity.current) { 0.8.dp.toPx() }
    val strokePx = with(LocalDensity.current) { strokeDp.toPx() }
    Canvas(modifier = Modifier.size(sizeDp)) {
        val outerRadius = (size.minDimension / 2f) - strokePx
        val center = Offset(size.width / 2f, size.height / 2f)
        //radius for each ring layer (0 = outermost)
        fun ringRadius(level: Int) = outerRadius - level * (strokePx + spacingPx)
        //drawsingle ring with its track + progress arc
        fun drawRing(level: Int, progress: Float, color: Color) {
            val r = ringRadius(level)
            val diameter = r * 2
            val topLeft = Offset(center.x - r, center.y - r)
            //trackbehind ring
            drawArc(
                color = Color.LightGray.copy(alpha = 0.18f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            //progressarc 0..1
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        //outer→inner rings mapping to protein/carbs/fat/calories
        drawRing(level = 0, progress = proteinProg, color = BytesPurple)
        drawRing(level = 1, progress = carbsProg, color = BytesMint)
        drawRing(level = 2, progress = fatProg, color = BytesPink)
        drawRing(level = 3, progress = calProg, color = BytesNavy)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MonthlyHistoryScreenPreview() {
    MacrosProjectTheme {
        MonthlyHistoryScreen()
    }
}
