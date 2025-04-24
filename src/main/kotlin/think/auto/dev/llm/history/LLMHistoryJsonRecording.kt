package think.auto.dev.llm.history

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class LLMHistoryJsonRecording(val project: Project) : LLMHistoryRecording {
    private val recordingPath: Path = Path.of(project.guessProjectDir()!!.path, "AutoDevHistoryRecord.json")
    override fun write(instruction: LLMHistoryRecordingInstruction) {
        if (!recordingPath.toFile().exists()) {
            recordingPath.toFile().createNewFile()
        }

        recordingPath.toFile().appendText(Json.encodeToString(instruction) + "\n")
    }
}

