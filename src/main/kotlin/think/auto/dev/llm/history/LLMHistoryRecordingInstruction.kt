package think.auto.dev.llm.history

import kotlinx.serialization.Serializable

@Serializable
data class LLMHistoryRecordingInstruction(
    val instruction: String,
    val output: String,
)
