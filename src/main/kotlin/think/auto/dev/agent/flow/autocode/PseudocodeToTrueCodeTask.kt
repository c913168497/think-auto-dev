package think.auto.dev.agent.flow.autocode

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import think.auto.dev.agent.flow.autocode.handler.GenerateCodeFlow
import think.auto.dev.agent.flow.autocode.handler.GetNeedClassAndFunFlow
import think.auto.dev.common.ui.statusbar.ThinkAutoDevStatus
import think.auto.dev.common.ui.statusbar.ThinkAutoDevStatusService
import think.auto.dev.settings.language.ThinkAutoDevMessagesBundle

class PseudocodeToTrueCodeTask(
    project: Project,
    val autocodeContext: PseudocodeToTrueCodeContext
) :
    Task.Backgroundable(project, "伪代码生成真实代码", true) {
    private val logger = logger<PseudocodeToTrueCodeTask>()

    override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = false
        indicator.fraction = 0.1
        indicator.text = ThinkAutoDevMessagesBundle.message("intentions.chat.code.test.step.prepare-context")
        ThinkAutoDevStatusService.notifyApplication(ThinkAutoDevStatus.InProgress)
        val getNeedClassAndFun = GetNeedClassAndFunFlow(indicator)
        indicator.fraction = 0.5
        val generateCode = GenerateCodeFlow(indicator)
        indicator.text = ThinkAutoDevMessagesBundle.message("intentions.chat.code.test.step.collect-context")
        indicator.fraction = 0.3
        getNeedClassAndFun.setNextStep(generateCode)
        getNeedClassAndFun.execute(autocodeContext)
        indicator.fraction = 1.0
    }

    override fun onThrowable(error: Throwable) {
        super.onThrowable(error)
        ThinkAutoDevStatusService.notifyApplication(ThinkAutoDevStatus.Error)
    }
}
