package think.auto.dev.llm.history

class LLMHistoryEmptyRecording : LLMHistoryRecording {
    override fun write(instruction: LLMHistoryRecordingInstruction) {
        // do nothing
    }
}
