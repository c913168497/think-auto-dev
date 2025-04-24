package think.auto.dev.llm

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import think.auto.dev.llm.provider.OpenAIProvider
import think.auto.dev.settings.aiProvider.AiProvider

@Service
class LlmFactory {
    fun create(project: Project, aiProvider: AiProvider): LLMProvider {
        return OpenAIProvider(project, aiProvider)
    }

    companion object {
        val instance: LlmFactory = LlmFactory()
    }
}
