package think.auto.dev.agent.flow.custom.gui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBTextField
import think.auto.dev.settings.flowEngine.database.FlowEngineConfig
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ActionEvent
import javax.swing.*

class FlowDesignerDialog(
    private val project: Project,
    private val existingFlow: FlowDefinition? = null,
    private val flowEngineConfig: FlowEngineConfig? = null
) : DialogWrapper(project) {
    private val flowNameField = JBTextField("New Flow").apply {
        font = Font("Microsoft YaHei", Font.PLAIN, 12)
    }
    private val flowCanvasPanel = FlowCanvasPanel()
    private val nodeConfigPanel = NodeConfigPanel { flowCanvasPanel.updateSelectedNode(it) }

    private var currentFlow = existingFlow ?: FlowDefinition(name = "New Flow")

    init {
        title = if (existingFlow == null) "创建新流程" else "编辑流程 - ${existingFlow.name}"

        // 设置配置面板和画布面板的相互引用
        nodeConfigPanel.setCanvasPanel(flowCanvasPanel)

        init()

        // 如果是编辑已有流程，加载流程数据
        if (existingFlow != null) {
            loadExistingFlow()
        }
    }

    private fun loadExistingFlow() {
        // 设置流程名称
        flowNameField.text = currentFlow.name

        // 加载节点
        for (node in currentFlow.nodes) {
            flowCanvasPanel.addExistingNode(node)
        }

        // 加载连接
        for (edge in currentFlow.edges) {
            flowCanvasPanel.addExistingEdge(edge)
        }

        // 重绘画布
        flowCanvasPanel.repaint()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        mainPanel.preferredSize = Dimension(1300, 900)
        mainPanel.minimumSize = Dimension(1300, 900)
        // 顶部面板 - 流程名称
        val topPanel = JPanel(BorderLayout())
        topPanel.add(JLabel("流程名称:"), BorderLayout.WEST)
        topPanel.add(flowNameField, BorderLayout.CENTER)
        mainPanel.add(topPanel, BorderLayout.NORTH)

        // 使用OnePixelSplitter替代JSplitPane
        val splitter = OnePixelSplitter(false, 0.7f) // 水平分割，初始比例0.7

        // 中间面板 - 流程设计区域
        splitter.firstComponent = JScrollPane(flowCanvasPanel).apply {
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        }
        splitter.secondComponent = JScrollPane(nodeConfigPanel).apply {
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        }

        mainPanel.add(splitter, BorderLayout.CENTER)

        // 不再需要显式设置dividerLocation，OnePixelSplitter会基于proportion自动计算
        // 也不再需要ComponentListener，OnePixelSplitter会自动处理比例

        // 设置节点选择监听器
        flowCanvasPanel.setNodeSelectionListener { node ->
            nodeConfigPanel.configureForNode(node)
        }

        return mainPanel
    }

    override fun doOKAction() {
        // 保存流程
        currentFlow = currentFlow.copy(
            name = flowNameField.text,
            nodes = flowCanvasPanel.getNodes(),
            edges = flowCanvasPanel.getEdges()
        )
        // 序列化流程定义
        FlowSerializer.saveFlow(project, currentFlow, flowEngineConfig)
        super.doOKAction()
    }

    override fun createActions(): Array<Action> {
        val helpAction = object : DialogWrapperAction("帮助") {
            override fun doAction(e: ActionEvent) {
                JOptionPane.showMessageDialog(
                    contentPane,
                    """
                流程设计器使用帮助:
                
                创建节点:
                - 点击右侧面板中的"添加新节点"按钮
                
                选择和编辑节点:
                - 左键点击节点进行选择
                - 右键点击节点并选择"编辑节点"
                - 在右侧面板编辑节点属性
                
                连接节点:
                - 右键点击节点并选择"开始连接"
                - 然后右键点击目标节点完成连接
                
                删除节点:
                - 选中节点后按Delete键
                - 或右键点击节点并选择"删除节点"
                
                删除连接:
                - 左键点击连接线选中它
                - 按Delete键或右键点击并选择"删除连接"
                
                执行顺序:
                - 白色圆圈中的数字显示执行顺序
                - 连接线上的数字显示流程序列
                """.trimIndent(),
                    "流程设计器帮助",
                    JOptionPane.INFORMATION_MESSAGE
                )
            }
        }

        return arrayOf(helpAction, okAction, cancelAction)
    }
}