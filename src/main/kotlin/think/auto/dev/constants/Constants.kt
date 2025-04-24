package think.auto.dev.constants

val DEFAULT_AI_ENGINE = "openAI"
val DEFAULT_AI_MODEL = "deepseek-chat"
var DEFAULT_HUMAN_LANGUAGE = HUMAN_LANGUAGES.ENGLISH.display

enum class HUMAN_LANGUAGES(val abbr: String, val display: String) {
    ENGLISH("en", "English"),
    CHINESE("zh", "中文");

    companion object {
        private val map: Map<String, HUMAN_LANGUAGES> = entries.associateBy { it.display }

        fun getAbbrByDisplay(display: String): String {
            return map.getOrDefault(display, ENGLISH).abbr
        }
    }
}
