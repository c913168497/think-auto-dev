package think.auto.dev.agent.flow.custom.processEngine.gui

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import think.auto.dev.agent.flow.custom.processEngine.ExecutionStatus
import think.auto.dev.agent.flow.custom.processEngine.Node
import think.auto.dev.agent.flow.custom.processEngine.NodeExecutionResult
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder

class FlowExecutionPanel : SimpleToolWindowPanel(true, true) {
    private val contentPanel = JPanel(VerticalLayout(10))
    private val scrollPane = JBScrollPane(contentPanel)
    private val nodePanels = mutableMapOf<String, NodeStatusPanel>()

    init {
        contentPanel.border = EmptyBorder(10, 10, 10, 10)
        setContent(scrollPane)
    }

    fun clear() {
        ApplicationManager.getApplication().invokeAndWait({
            contentPanel.removeAll()
            nodePanels.clear()
            contentPanel.revalidate()
            contentPanel.repaint()
        }, ModalityState.any())
    }

    fun initializeNodes(nodes: List<Node>, executionOrder: List<String>) {
        ApplicationManager.getApplication().invokeAndWait({
            // 使用计算出的执行顺序初始化节点
            for (nodeId in executionOrder) {
                val node = nodes.find { it.id == nodeId } ?: continue

                val nodePanel = NodeStatusPanel(node.name)
                nodePanels[node.id] = nodePanel
                contentPanel.add(nodePanel)

                // 初始状态为等待中
                nodePanel.updateStatus(ExecutionStatus.PENDING)
            }

            contentPanel.revalidate()
            contentPanel.repaint()
        }, ModalityState.any())
    }

    fun updateNodeStatus(nodeId: String, status: ExecutionStatus) {
        ApplicationManager.getApplication().invokeAndWait({
            nodePanels[nodeId]?.updateStatus(status)
        }, ModalityState.any())
    }

    fun updateNodeResult(result: NodeExecutionResult) {
        ApplicationManager.getApplication().invokeAndWait({
            nodePanels[result.nodeId]?.let { panel ->
                panel.updateStatus(result.status)
                panel.setOutput(result.output ?: "")
            }

            contentPanel.revalidate()
            contentPanel.repaint()
        }, ModalityState.any())
    }

    inner class NodeStatusPanel(nodeName: String) : JPanel() {
        private val statusIcon = JLabel()
        private val outputArea = JTextArea()
        private val outputPanel = JPanel(BorderLayout())
        private var isExpanded = false
        private val toggleButton = JButton()

        init {
            layout = BorderLayout()
            border = LineBorder(JBColor.border(), 1, true)

            // 创建标题面板
            val headerPanel = JPanel(BorderLayout())
            headerPanel.border = EmptyBorder(5, 5, 5, 5)

            val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
            statusIcon.icon = AllIcons.General.BalloonInformation
            leftPanel.add(statusIcon)
            leftPanel.add(JLabel(nodeName))

            headerPanel.add(leftPanel, BorderLayout.WEST)

            toggleButton.icon = AllIcons.General.ArrowDown
            toggleButton.addActionListener {
                isExpanded = !isExpanded
                toggleButton.icon = if (isExpanded) AllIcons.General.ArrowUp else AllIcons.General.ArrowDown
                outputPanel.isVisible = isExpanded
                revalidate()
                repaint()
            }

            headerPanel.add(toggleButton, BorderLayout.EAST)
            add(headerPanel, BorderLayout.NORTH)

            // 创建输出面板
            outputArea.isEditable = false
            outputArea.lineWrap = true
            outputArea.wrapStyleWord = true
            outputArea.border = EmptyBorder(5, 5, 5, 5)
            outputArea.text = "等待执行..."

            val outputScrollPane = JBScrollPane(outputArea)
            outputScrollPane.preferredSize = Dimension(0, 200)

            outputPanel.add(outputScrollPane, BorderLayout.CENTER)
            outputPanel.isVisible = false

            add(outputPanel, BorderLayout.CENTER)
        }

        fun updateStatus(status: ExecutionStatus) {
            statusIcon.icon = when (status) {
                ExecutionStatus.PENDING -> AllIcons.General.BalloonInformation
                ExecutionStatus.RUNNING -> AllIcons.Actions.Refresh
                ExecutionStatus.SUCCESS -> AllIcons.General.InspectionsOK
                ExecutionStatus.FAILURE -> AllIcons.General.Error
                ExecutionStatus.SKIPPED -> AllIcons.General.Warning
            }

            revalidate()
            repaint()
        }

        fun setOutput(output: String) {
            outputArea.text = output
        }
    }
}