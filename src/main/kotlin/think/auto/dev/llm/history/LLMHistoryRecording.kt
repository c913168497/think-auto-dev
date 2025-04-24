package think.auto.dev.llm.history

interface LLMHistoryRecording {
    fun write(instruction: LLMHistoryRecordingInstruction)
}
