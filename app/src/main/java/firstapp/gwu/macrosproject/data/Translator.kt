package firstapp.gwu.macrosproject.data

fun translate(lang: String, en: String, es: String): String {
    return when (lang) {
        "es" -> es
        else -> en
    }
}
