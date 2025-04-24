package think.auto.dev.agent.flow.core
import com.intellij.openapi.progress.ProgressIndicator
import think.auto.dev.settings.agentFlowAiProvider.database.AgentFunctionFlowAiProviderDataBaseComponent
import think.auto.dev.settings.aiProvider.AiProvider
import think.auto.dev.settings.aiProvider.AiProviderDBComponent

abstract class BaseWorkflowStep<C : WorkflowContext>(private val indicator: ProgressIndicator) : WorkflowStep<C> {
    private var nextStep: WorkflowStep<C>? = null

    override fun setNextStep(next: WorkflowStep<C>): WorkflowStep<C> {
        this.nextStep = next
        return next
    }

    override fun execute(context: C): C {
        val updatedContext = process(context)
        return nextStep?.execute(updatedContext) ?: updatedContext
    }

    abstract fun process(context: C): C

    /**
     * 获取
     */
    fun getAiProvider(functionName: String): AiProvider? {
        val aiProviderFlow =
            AgentFunctionFlowAiProviderDataBaseComponent.getAgentFunctionFlowAiProviderByTitle(functionName)
        var aiProvider: AiProvider? = null
        if (aiProviderFlow == null) {
            val allAiProviders = AiProviderDBComponent.getAllAiProviders()
            aiProvider = allAiProviders[0]
        } else {
            val aiProviderId = aiProviderFlow.aiProviderId
            aiProvider = AiProviderDBComponent.getAiProviderById(aiProviderId)
        }

        return aiProvider
    }
}