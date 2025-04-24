package think.auto.dev.agent.flow.custom.processEngine.gui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class FlowExecutionToolWindowFactory : ToolWindowFactory {
    companion object {
        const val TOOL_WINDOW_ID = "Flow Execution"
        private val panels = mutableMapOf<Project, FlowExecutionPanel>()

        fun getPanel(project: Project): FlowExecutionPanel? = panels[project]

        fun showToolWindow(project: Project): FlowExecutionPanel {
            val latch = CountDownLatch(1)
            var panel: FlowExecutionPanel? = null

            ApplicationManager.getApplication().invokeAndWait({
                val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID)
                if (toolWindow != null) {
                    // 确保工具窗口已激活并显示
                    if (!toolWindow.isVisible) {
                        toolWindow.show(null)
                    }

                    // 获取或创建面板
                    panel = panels[project]
                    if (panel == null) {
                        panel = FlowExecutionPanel()
                        panels[project] = panel!!

                        val contentFactory = ContentFactory.getInstance()
                        val content = contentFactory.createContent(panel, "", false)
                        toolWindow.contentManager.addContent(content)
                    }

                    // 确保面板已激活
                    if (!toolWindow.isActive) {
                        toolWindow.activate(null)
                    }

                    latch.countDown()
                }
            }, ModalityState.any())

            // 等待面板初始化完成，最多等待2秒
            try {
                latch.await(2, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                // 忽略中断异常
            }

            return panel ?: let {
                // 如果无法获取面板，创建一个新的（这种情况应该很少发生）
                val newPanel = FlowExecutionPanel()
                panels[project] = newPanel
                newPanel
            }
        }
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = panels.getOrPut(project) { FlowExecutionPanel() }

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}