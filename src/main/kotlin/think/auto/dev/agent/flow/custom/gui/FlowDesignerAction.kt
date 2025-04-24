package think.auto.dev.agent.flow.custom.gui

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class FlowDesignerAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        FlowDesignerDialog(project).show()
    }
}