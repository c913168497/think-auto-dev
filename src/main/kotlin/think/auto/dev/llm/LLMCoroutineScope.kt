package think.auto.dev.llm

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * 协程处理
 */
@Service(Service.Level.PROJECT)
class LLMCoroutineScope {

    // 协程异常处理器
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Logger.getInstance(LLMCoroutineScope::class.java).error(throwable)
    }


    val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + coroutineExceptionHandler)

    companion object {
        fun scope(project: Project): CoroutineScope = project.service<LLMCoroutineScope>().coroutineScope
    }
}
