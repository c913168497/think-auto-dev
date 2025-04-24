package think.auto.dev.settings.aiProvider


data class AiProvider(
    val id: Int?,
    val customOpenAiHost: String,
    val customEngineToken: String,
    val customEngineHead: String,
    val customModel: String,
    val customModelName: String,
    val recordingInLocal: Boolean = true
) {
    override fun toString(): String {
        return customModelName
    }
}
