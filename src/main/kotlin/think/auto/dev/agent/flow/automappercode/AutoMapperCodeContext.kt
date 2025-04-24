package think.auto.dev.agent.flow.automappercode

import think.auto.dev.agent.flow.core.WorkflowContext


data class AutoMapperCodeContext(
    val classInfo: String,
    val selectCode: String
) : WorkflowContext
