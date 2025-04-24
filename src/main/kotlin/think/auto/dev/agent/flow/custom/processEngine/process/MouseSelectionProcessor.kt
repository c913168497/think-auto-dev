package think.auto.dev.agent.flow.custom.processEngine.process

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import think.auto.dev.agent.flow.custom.processEngine.*

// 鼠标选中文本处理器
class MouseSelectionProcessor(project: Project, var editor: Editor?, file: PsiFile?) : NodeProcessor {
    override fun canProcess(nodeType: String): Boolean {
        return nodeType == "MOUSE_SELECTION"
    }

    override fun process(node: Node, inputs: Map<String, String?>, context: ExecutionContext): NodeExecutionResult {
        // TODO: 实现获取鼠标选中文本的功能
        if (editor == null) {
            return NodeExecutionResult(
                nodeId = node.id,
                nodeName = node.name,
                output = "",
                status = ExecutionStatus.FAILURE
            )
        }

        val selectedText = getSelectedText(editor = editor!!) // 您需要实现这个方法

        return NodeExecutionResult(
            nodeId = node.id,
            nodeName = node.name,
            output = selectedText,
            status = ExecutionStatus.SUCCESS
        )
    }

    private fun getSelectedText(editor: Editor): String? {
        // 实现获取选中文本的逻辑
        return ApplicationManager.getApplication().runReadAction<String?> {
            editor.selectionModel.selectedText
        }
    }
}