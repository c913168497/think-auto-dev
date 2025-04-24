package think.auto.dev.settings

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import think.auto.dev.llm.LLMCoroutineScope
import think.auto.dev.llm.LlmFactory
import think.auto.dev.settings.aiProvider.AiProvider
import javax.swing.JLabel

fun Panel.testLLMConnection(project: Project?, aiProvider: AiProvider?) {
    row {
        // test result
        val result = JLabel("")
        button("Test LLM Connection") {
            if (project == null) return@button
            if (aiProvider == null) return@button
            result.text = ""

            // test custom engine
            LLMCoroutineScope.scope(project).launch {
                try {
                    val flowString: Flow<String> =
                        LlmFactory.instance.create(project, aiProvider).stream("hi", "", false)
                    flowString.collect {
                        result.text += it
                    }
                } catch (e: Exception) {
                    result.text = e.message ?: "Unknown error"
                }
            }
        }

        cell(result).align(Align.FILL)
    }
}
