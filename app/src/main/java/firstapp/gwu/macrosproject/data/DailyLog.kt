package firstapp.gwu.macrosproject.data

data class DailyLog(
    val date: String = "",
    val items: List<FoodItem> = emptyList()
)
