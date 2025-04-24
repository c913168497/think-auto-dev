package think.auto.dev.agent.flow.core

interface WorkflowStep<C: WorkflowContext> {
    fun setNextStep(next: WorkflowStep<C>): WorkflowStep<C>
    fun execute(context: C): C
}
