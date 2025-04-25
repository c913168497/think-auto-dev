package think.auto.dev.agent.flow.custom.gui

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import think.auto.dev.settings.aiProvider.AiProviderDBComponent
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class NodeConfigPanel(private val onNodeUpdated: (FlowNode) -> Unit) : JPanel() {
    private val cardLayout = CardLayout()
    private val configCards = JPanel(cardLayout)

    // 创建自定义渲染器，显示中文名称
    private val nodeTypeCombo = ComboBox(NodeType.values()).apply {
        renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is NodeType) {
                    text = value.displayName
                }
                return this
            }
        }
    }

    private val nodeNameField = JBTextField()

    // 添加节点按钮
    private val addNodeButton = JButton("添加新节点").apply {
        font = Font("Microsoft YaHei", Font.PLAIN, 12)
    }

    // 正则处理配置
    private val regexPatternField = JBTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        font = Font("Microsoft YaHei", Font.PLAIN, 12)
    }

    // AI模型处理配置
    private val aiPromptField = JBTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        font = Font("Microsoft YaHei", Font.PLAIN, 12)
    }

    // 忽略文件夹
    private val ignoredFoldersField = JBTextField()

    private val modelOptions = AiProviderDBComponent.getAllAiProviders().map { it.customModelName }.toTypedArray()
    private val modelCombo = ComboBox(modelOptions)

    // 父节点引用按钮面板
    private val parentNodesPanel = JPanel().apply {
        layout = FlowLayout(FlowLayout.LEFT)
        border = BorderFactory.createTitledBorder("引用父节点输出")
    }

    private var currentNode: FlowNode? = null
    private var canvasPanel: FlowCanvasPanel? = null

    init {
        // 使用BorderLayout来确保组件正确填充
        layout = BorderLayout()

        // 设置字体
        val defaultFont = Font("Microsoft YaHei", Font.PLAIN, 12)
        UIManager.put("Label.font", defaultFont)
        UIManager.put("TextField.font", defaultFont)
        UIManager.put("TextArea.font", defaultFont)
        UIManager.put("ComboBox.font", defaultFont)

        // 顶部通用配置面板
        val commonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            // 使组件左对齐
            alignmentX = Component.LEFT_ALIGNMENT

            // 添加节点按钮 - 使其填充整个宽度
            val buttonPanel = JPanel(BorderLayout())
            buttonPanel.add(addNodeButton, BorderLayout.CENTER)
            add(buttonPanel)
            add(Box.createVerticalStrut(20))

            // 节点类型选择
            add(JLabel("节点类型:"))
            add(nodeTypeCombo)
            add(Box.createVerticalStrut(10))

            // 节点名称
            add(JLabel("节点名称:"))
            add(nodeNameField)
            add(Box.createVerticalStrut(10))
        }

        // 创建各类型节点的配置卡片
        createConfigCards()

        // 将面板添加到主面板
        val topScrollPane = JScrollPane(commonPanel).apply {
            border = BorderFactory.createEmptyBorder()
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        }

        val configScrollPane = JScrollPane(configCards).apply {
            border = BorderFactory.createEmptyBorder()
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        }

        // 使用分割面板确保两部分都能正确显示
        val splitter = OnePixelSplitter(true, 0.3f).apply { // true表示垂直分割，0.3f是初始比例
            firstComponent = topScrollPane
            secondComponent = configScrollPane
        }

        add(splitter, BorderLayout.CENTER)
        // 添加事件监听
        nodeTypeCombo.addActionListener {
            val selectedType = nodeTypeCombo.selectedItem as NodeType
            cardLayout.show(configCards, selectedType.name)
            updateCurrentNode()
            // 当切换到PROMPT_EDIT类型时，更新父节点按钮
            if (selectedType == NodeType.PROMPT_EDIT) {
                updateParentNodesButtons()
            }
        }

        // 实时监听节点名称变化
        nodeNameField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = updateCurrentNode()
            override fun removeUpdate(e: DocumentEvent) = updateCurrentNode()
            override fun changedUpdate(e: DocumentEvent) = updateCurrentNode()
        })

        // 添加节点按钮事件
        addNodeButton.addActionListener {
            // 先保存当前节点的修改
            if (currentNode != null) {
                updateCurrentNode()
            }
            var nodeType = nodeTypeCombo.selectedItem as NodeType
            // 然后添加新节点
            canvasPanel?.addNewNode(
                nodeType,
                nodeNameField.text.ifEmpty { nodeType.displayName }
            )
        }

        // 添加组件监听器，处理大小变化
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                // 当面板大小改变时，更新布局
                revalidate()
            }
        })
    }

    fun setCanvasPanel(panel: FlowCanvasPanel) {
        canvasPanel = panel
    }

    private fun createConfigCards() {
        // 为每种节点类型创建配置卡片
        for (nodeType in NodeType.entries) {
            val card = JPanel(BorderLayout())

            when (nodeType) {
                NodeType.REGEX_PROCESS -> {
                    val regexPanel = JPanel().apply {
                        layout = BoxLayout(this, BoxLayout.Y_AXIS)
                        alignmentX = Component.LEFT_ALIGNMENT

                        add(JLabel("正则表达式:"))
                        add(JBScrollPane(regexPatternField).apply {
                            preferredSize = Dimension(400, 150)
                        })

                        regexPatternField.document.addDocumentListener(object : DocumentListener {
                            override fun insertUpdate(e: DocumentEvent) = updateCurrentNode()
                            override fun removeUpdate(e: DocumentEvent) = updateCurrentNode()
                            override fun changedUpdate(e: DocumentEvent) = updateCurrentNode()
                        })
                    }
                    card.add(regexPanel, BorderLayout.NORTH)
                }

                NodeType.AI_MODEL_PROCESS -> {
                    val aiPanel = JPanel().apply {
                        layout = BoxLayout(this, BoxLayout.Y_AXIS)
                        alignmentX = Component.LEFT_ALIGNMENT

                        add(JLabel("AI模型:"))
                        add(modelCombo)
                        add(Box.createVerticalStrut(10))

                        modelCombo.addActionListener { updateCurrentNode() }
                    }
                    card.add(aiPanel, BorderLayout.NORTH)
                }

                NodeType.PROMPT_EDIT -> {
                    val promptPanel = JPanel().apply {
                        layout = BorderLayout()

                        // 提示词编辑区域
                        val editPanel = JPanel().apply {
                            layout = BoxLayout(this, BoxLayout.Y_AXIS)
                            alignmentX = Component.LEFT_ALIGNMENT

                            add(JLabel("提示词:"))
                            add(JBScrollPane(aiPromptField).apply {
                                preferredSize = Dimension(400, 350)
                            })

                            aiPromptField.document.addDocumentListener(object : DocumentListener {
                                override fun insertUpdate(e: DocumentEvent) = updateCurrentNode()
                                override fun removeUpdate(e: DocumentEvent) = updateCurrentNode()
                                override fun changedUpdate(e: DocumentEvent) = updateCurrentNode()
                            })
                        }

                        // 父节点引用按钮区域
                        val parentNodesScrollPane = JScrollPane(parentNodesPanel).apply {
                            preferredSize = Dimension(400, 100)
                            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                        }

                        add(editPanel, BorderLayout.NORTH)
                        add(parentNodesScrollPane, BorderLayout.CENTER)
                    }
                    card.add(promptPanel, BorderLayout.NORTH)
                }


                NodeType.FOLDER_CONTENT_GET -> {
                    val ignoredFoldersPanel = JPanel().apply {
                        layout = BoxLayout(this, BoxLayout.X_AXIS)
                        alignmentX = Component.LEFT_ALIGNMENT
                        add(JLabel("忽略文件夹:"))
                        add(ignoredFoldersField)
                        ignoredFoldersField.document.addDocumentListener(object : DocumentListener {
                            override fun insertUpdate(e: DocumentEvent) = updateCurrentNode()
                            override fun removeUpdate(e: DocumentEvent) = updateCurrentNode()
                            override fun changedUpdate(e: DocumentEvent) = updateCurrentNode()
                        })
                    }

                    card.add(ignoredFoldersPanel, BorderLayout.NORTH)
                }

                else -> {
                    // 其他类型节点没有特殊配置
                    card.add(JLabel("该节点类型无需额外配置。"), BorderLayout.CENTER)
                }
            }

            configCards.add(card, nodeType.name)
        }
    }

    fun configureForNode(node: FlowNode?) {
        currentNode = node

        if (node == null) {
            nodeTypeCombo.selectedItem = NodeType.GET_CLASS_CONTENT
            nodeNameField.text = ""
            regexPatternField.text = ""
            aiPromptField.text = ""
            modelCombo.selectedIndex = 0
            ignoredFoldersField.text = ""
            return
        }

        // 设置通用属性
        nodeTypeCombo.selectedItem = node.type
        nodeNameField.text = node.name

        // 设置特定类型的属性
        when (node.type) {
            NodeType.REGEX_PROCESS -> {
                regexPatternField.text = node.config ?: ""
            }

            NodeType.FOLDER_CONTENT_GET -> {
                ignoredFoldersField.text = node.config ?: ""
            }

            NodeType.AI_MODEL_PROCESS -> {
                val selectedModel = node.config ?: ""
                val modelIndex = modelOptions.indexOf(selectedModel)
                modelCombo.selectedIndex = if (modelIndex >= 0) modelIndex else 0
            }

            NodeType.PROMPT_EDIT -> {
                aiPromptField.text = node.config ?: ""
                // 更新父节点按钮
                updateParentNodesButtons()
            }

            else -> {}
        }

        // 显示对应的配置卡片
        cardLayout.show(configCards, node.type.name)
    }

    private fun updateCurrentNode() {
        val node = currentNode ?: return

        val updatedConfig = when (nodeTypeCombo.selectedItem as NodeType) {
            // 如果节点类型为正则过滤处理， 则显示正则表达式
            NodeType.REGEX_PROCESS -> {
                regexPatternField.text
            }

            NodeType.AI_MODEL_PROCESS -> {
                modelCombo.selectedItem as String
            }

            NodeType.PROMPT_EDIT -> {
                aiPromptField.text
            }

            NodeType.FOLDER_CONTENT_GET -> {
                ignoredFoldersField.text
            }

            else -> ""
        }

        val updatedNode = node.copy(
            type = nodeTypeCombo.selectedItem as NodeType,
            name = nodeNameField.text,
            config = updatedConfig
        )

        currentNode = updatedNode
        onNodeUpdated(updatedNode)
    }

    // 更新父节点按钮
    private fun updateParentNodesButtons() {
        // 清空现有按钮
        parentNodesPanel.removeAll()

        val currentNodeId = currentNode?.id ?: return
        val canvas = canvasPanel ?: return

        // 获取所有节点和边
        val nodes = canvas.getNodes()
        val edges = canvas.getEdges()

        // 计算执行顺序
        val executionOrder = calculateExecutionOrder(nodes, edges)

        // 找出所有父节点（直接连接到当前节点的节点）
        val parentNodeIds = edges
            .filter { it.targetNodeId == currentNodeId }
            .map { it.sourceNodeId }

        // 为每个父节点创建一个按钮
        for (parentId in parentNodeIds) {
            val parentNode = nodes.find { it.id == parentId } ?: continue
            val order = executionOrder[parentId] ?: continue

            val buttonText = "${parentNode.name}-${order}"
            val button = JButton(buttonText).apply {
                font = Font("Microsoft YaHei", Font.PLAIN, 12)
                addActionListener {
                    // 点击按钮时，在提示词编辑框中插入对应的变量引用
                    val variableName = "\${input_${parentNode.name}-${order}}"
                    aiPromptField.insert(variableName, aiPromptField.caretPosition)
                    updateCurrentNode()
                }
            }

            parentNodesPanel.add(button)
        }

        // 刷新面板
        parentNodesPanel.revalidate()
        parentNodesPanel.repaint()
    }

    // 计算节点执行顺序（从FlowCanvasPanel复制并简化）
    private fun calculateExecutionOrder(nodes: List<FlowNode>, edges: List<FlowEdge>): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        val visited = mutableSetOf<String>()
        var order = 1

        // 找出所有没有入边的节点（起始节点）
        val startNodes = nodes.filter { node ->
            edges.none { it.targetNodeId == node.id }
        }

        // 从每个起始节点开始进行深度优先搜索
        for (startNode in startNodes) {
            dfs(startNode.id, visited, result, order, edges)
            order++
        }

        // 处理可能的孤立节点或环
        for (node in nodes) {
            if (node.id !in result) {
                result[node.id] = order++
            }
        }

        return result
    }

    private fun dfs(nodeId: String, visited: MutableSet<String>, result: MutableMap<String, Int>, order: Int, edges: List<FlowEdge>) {
        if (nodeId in visited) return

        visited.add(nodeId)
        result[nodeId] = order

        // 找出所有从当前节点出发的边
        val outEdges = edges.filter { it.sourceNodeId == nodeId }

        // 递归处理所有相邻节点
        for (edge in outEdges) {
            dfs(edge.targetNodeId, visited, result, order + 1, edges)
        }
    }
}