package think.auto.dev.agent.flow.custom.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import kotlinx.serialization.json.Json
import think.auto.dev.agent.flow.custom.gui.FlowDefinition
import think.auto.dev.agent.flow.custom.gui.FlowDesignerDialog
import think.auto.dev.settings.flowEngine.database.FlowEngineConfigDataBaseComponent
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

// 注册快捷键的Action
class FlowManagerAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        FlowManagerDialog(project).show()
    }
}

// 流程管理对话框
class FlowManagerDialog(private val project: Project) : DialogWrapper(project) {
    private val flowList = JBList<String>()
    private val listModel = DefaultListModel<String>()

    private val newButton = JButton("新增")
    private val editButton = JButton("编辑")
    private val deleteButton = JButton("删除")

    init {
        title = "流程管理"
        init()
        loadFlows()
    }

    private fun loadFlows() {
        // 从数据库加载所有流程
        val flows = FlowEngineConfigDataBaseComponent.getAllFlowEngineConfig()

        listModel.clear()
        flows.forEach {
            listModel.addElement(it.title)
        }

        // 如果有流程，默认选中第一个
        if (listModel.size > 0) {
            flowList.selectedIndex = 0
            updateButtonState()
        } else {
            updateButtonState()
        }


    }

    private fun updateButtonState() {
        val hasSelection = flowList.selectedIndex >= 0
        editButton.isEnabled = hasSelection
        deleteButton.isEnabled = hasSelection
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout(10, 10))
        mainPanel.preferredSize = Dimension(400, 300)

        // 设置列表
        flowList.model = listModel
        flowList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        flowList.addListSelectionListener {
            updateButtonState()
        }

        // 添加列表到滚动面板
        val scrollPane = JBScrollPane(flowList)
        mainPanel.add(scrollPane, BorderLayout.CENTER)

        // 按钮面板
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER, 10, 10))

        // 新增按钮
        newButton.addActionListener {
            val dialog = FlowDesignerDialog(project)
            dialog.show()

            // 对话框关闭后刷新列表
            if (dialog.isOK) {
                loadFlows()
            }
        }
        buttonPanel.add(newButton)

        // 编辑按钮
        editButton.addActionListener {
            val selectedFlow = flowList.selectedValue ?: return@addActionListener

            // 获取选中的流程
            val flowConfig = FlowEngineConfigDataBaseComponent.getFlowEngineConfigByTitle(selectedFlow)
            if (flowConfig != null) {
                // 解析流程定义
                val json = Json { ignoreUnknownKeys = true }
                try {
                    val flowDefinition = json.decodeFromString(FlowDefinition.serializer(), flowConfig.content)

                    // 打开编辑对话框，并传入已有流程
                    val dialog = FlowDesignerDialog(project, flowDefinition, flowConfig)
                    dialog.show()

                    // 对话框关闭后刷新列表
                    if (dialog.isOK) {
                        loadFlows()
                    }
                } catch (e: Exception) {
                    Messages.showErrorDialog(
                        "无法解析流程定义: ${e.message}",
                        "错误"
                    )
                }
            }
        }
        buttonPanel.add(editButton)

        // 删除按钮
        deleteButton.addActionListener {
            val selectedFlow = flowList.selectedValue ?: return@addActionListener

            // 确认删除
            val result = Messages.showYesNoDialog(
                "确定要删除流程 '$selectedFlow' 吗？",
                "确认删除",
                Messages.getQuestionIcon()
            )

            if (result == Messages.YES) {
                // 从数据库删除流程
                FlowEngineConfigDataBaseComponent.deleteFlowEngineConfig(selectedFlow)
                loadFlows()
            }
        }
        buttonPanel.add(deleteButton)

        mainPanel.add(buttonPanel, BorderLayout.SOUTH)

        // 初始化按钮状态
        updateButtonState()

        return mainPanel
    }
}