package firstapp.gwu.macrosproject.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.ZoneId


object LogRepository {

    private val firestore = FirebaseFirestore.getInstance()

    private val _loggedFoods = MutableStateFlow<List<FoodItem>>(emptyList())
    val loggedFoods: StateFlow<List<FoodItem>> = _loggedFoods.asStateFlow()

    private var userId: String? = null
    private var currentDayId: String? = null

    private fun todayId(): String =
        LocalDate.now(ZoneId.systemDefault()).toString()   // e.g. "2025-11-29"

    private fun todayDocRef() =
        firestore.collection("users")
            .document(userId ?: error("LogRepository not initialized with user id"))
            .collection("logs")
            .document(todayId())

    fun initForUser(uid: String) {
        if (userId == uid && currentDayId == todayId()) return
        userId = uid
        currentDayId = todayId()
        todayDocRef().addSnapshotListener { snap, error ->
            if (error != null) return@addSnapshotListener
            if (snap == null || !snap.exists()) {
                _loggedFoods.value = emptyList()
            } else {
                val daily = snap.toObject(DailyLog::class.java)
                _loggedFoods.value = daily?.items ?: emptyList()
            }
        }
    }
    //helper: return a copy of food scaled to given servings
    private fun withServings(food: FoodItem, servings: Double): FoodItem {
        val oldServings = food.servings ?: 1.0
        val perCal = (food.calories ?: 0.0) / oldServings
        val perProt = (food.protein_g ?: 0.0) / oldServings
        val perCarb = (food.carbohydrates_total_g ?: 0.0) / oldServings
        val perFat = (food.fat_total_g ?: 0.0) / oldServings

        return food.copy(
            calories = perCal * servings,
            protein_g = perProt * servings,
            carbohydrates_total_g = perCarb * servings,
            fat_total_g = perFat * servings,
            servings = servings
        )
    }

    //add a food with the given servings
    fun addFood(food: FoodItem, servings: Double = 1.0) {
        val uid = userId ?: return
        val today = todayId()
        if (today != currentDayId) {
            currentDayId = today
        }
        val scaled = withServings(food, servings)
        val newList = _loggedFoods.value + scaled
        _loggedFoods.value = newList
        val dailyLog = DailyLog(date = today, items = newList)
        todayDocRef().set(dailyLog)
    }
    //update the servings for an item
    fun updateFoodServings(index: Int, newServings: Double) {
        if (index !in _loggedFoods.value.indices) return
        val today = todayId()
        if (today != currentDayId) {
            currentDayId = today
        }
        val currentList = _loggedFoods.value.toMutableList()
        val old = currentList[index]
        val updated = withServings(old, newServings)
        currentList[index] = updated
        _loggedFoods.value = currentList
        val dailyLog = DailyLog(date = today, items = currentList)
        todayDocRef().set(dailyLog)
    }
    //remove an item
    fun removeFood(index: Int) {
        if (index !in _loggedFoods.value.indices) return
        val today = todayId()
        if (today != currentDayId) {
            currentDayId = today
        }
        val mutable = _loggedFoods.value.toMutableList()
        mutable.removeAt(index)
        _loggedFoods.value = mutable
        val dailyLog = DailyLog(date = today, items = mutable)
        todayDocRef().set(dailyLog)
    }

    //clear todays logs
    fun clearToday() {
        val today = todayId()
        currentDayId = today
        _loggedFoods.value = emptyList()
        todayDocRef().set(DailyLog(date = today, items = emptyList()))
    }
}
