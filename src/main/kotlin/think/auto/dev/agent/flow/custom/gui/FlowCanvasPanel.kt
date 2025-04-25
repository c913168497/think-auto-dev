package think.auto.dev.agent.flow.custom.gui

import java.awt.*
import java.awt.event.*
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities

class FlowCanvasPanel : JPanel() {
    private val nodes = mutableListOf<FlowNode>()
    private val edges = mutableListOf<FlowEdge>()

    private var selectedNode: FlowNode? = null
    private var selectedEdge: FlowEdge? = null
    private var draggedNode: FlowNode? = null
    private var dragOffsetX = 0
    private var dragOffsetY = 0

    private var edgeStartNode: FlowNode? = null
    private var isDrawingEdge = false
    private var mouseX = 0
    private var mouseY = 0

    private var nodeSelectionListener: ((FlowNode?) -> Unit)? = null
    private val nodeContextMenu = JPopupMenu()
    private val deleteNodeItem = JMenuItem("删除节点")
    private val startConnectionItem = JMenuItem("开始连接")
    private val editNodeItem = JMenuItem("编辑节点")

    // 添加边的上下文菜单
    private val edgeContextMenu = JPopupMenu()
    private val deleteEdgeItem = JMenuItem("删除连接")

    init {
        background = Color.WHITE
        // 设置节点上下文菜单
        setupContextMenu()

        // 添加键盘监听器用于删除节点
        setupKeyListener()

        // 使用MouseAdapter而不是分别添加监听器
        val mouseAdapter = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    // 检查是否点击了节点
                    val clickedNode = findNodeAt(e.x, e.y)

                    if (clickedNode != null) {
                        // 开始拖动节点
                        draggedNode = clickedNode
                        dragOffsetX = e.x - clickedNode.x
                        dragOffsetY = e.y - clickedNode.y

                        // 选择节点
                        selectNode(clickedNode)
                    } else {
                        // 检查是否点击了边
                        val clickedEdge = findEdgeAt(e.x, e.y)
                        if (clickedEdge != null) {
                            selectedEdge = clickedEdge
                            selectedNode = null
                            nodeSelectionListener?.invoke(null)
                        } else {
                            // 点击空白区域，取消选择
                            selectNode(null)
                            selectedEdge = null
                        }
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    val clickedNode = findNodeAt(e.x, e.y)
                    val clickedEdge = findEdgeAt(e.x, e.y)

                    if (clickedNode != null) {
                        // 选择节点并显示上下文菜单
                        selectNode(clickedNode)

                        // 如果正在绘制连接线，则完成连接
                        if (isDrawingEdge && edgeStartNode != null && edgeStartNode != clickedNode) {
                            // 检查是否已存在相同的连接
                            val existingEdge = edges.find {
                                it.sourceNodeId == edgeStartNode!!.id && it.targetNodeId == clickedNode.id
                            }

                            if (existingEdge == null) {
                                edges.add(
                                    FlowEdge(
                                        sourceNodeId = edgeStartNode!!.id,
                                        targetNodeId = clickedNode.id
                                    )
                                )
                            }
                            isDrawingEdge = false
                            edgeStartNode = null
                            repaint()
                        } else {
                            // 否则显示上下文菜单
                            nodeContextMenu.show(this@FlowCanvasPanel, e.x, e.y)
                        }
                    } else if (clickedEdge != null) {
                        // 显示边的上下文菜单
                        selectedEdge = clickedEdge
                        selectedNode = null
                        nodeSelectionListener?.invoke(null)
                        edgeContextMenu.show(this@FlowCanvasPanel, e.x, e.y)
                    } else if (isDrawingEdge) {
                        // 点击空白区域取消连接
                        isDrawingEdge = false
                        edgeStartNode = null
                        repaint()
                    }
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                draggedNode = null
            }

            override fun mouseDragged(e: MouseEvent) {
                if (draggedNode != null) {
                    // 更新被拖动节点的位置
                    val index = nodes.indexOfFirst { it.id == draggedNode!!.id }
                    if (index >= 0) {
                        nodes[index] = draggedNode!!.copy(
                            x = e.x - dragOffsetX,
                            y = e.y - dragOffsetY
                        )
                        repaint()
                    }
                }

                if (isDrawingEdge) {
                    mouseX = e.x
                    mouseY = e.y
                    repaint()
                }
            }

            override fun mouseMoved(e: MouseEvent) {
                if (isDrawingEdge) {
                    mouseX = e.x
                    mouseY = e.y
                    repaint()
                }
            }
        }

        // 使用统一的MouseAdapter添加所有鼠标监听
        addMouseListener(mouseAdapter)
        addMouseMotionListener(mouseAdapter)

        // 确保面板可以获取焦点以响应键盘事件
        isFocusable = true

        // 添加组件监听器，处理大小变化
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                // 当组件大小改变时，重新绘制
                repaint()
            }
        })
    }

    private fun setupContextMenu() {
        // 节点上下文菜单
        editNodeItem.addActionListener {
            // 触发节点编辑
            nodeSelectionListener?.invoke(selectedNode)
        }
        nodeContextMenu.add(editNodeItem)

        startConnectionItem.addActionListener {
            if (selectedNode != null) {
                edgeStartNode = selectedNode
                isDrawingEdge = true
                repaint()
            }
        }
        nodeContextMenu.add(startConnectionItem)

        deleteNodeItem.addActionListener {
            deleteSelectedNode()
        }
        nodeContextMenu.add(deleteNodeItem)

        // 边的上下文菜单
        deleteEdgeItem.addActionListener {
            deleteSelectedEdge()
        }
        edgeContextMenu.add(deleteEdgeItem)
    }

    private fun setupKeyListener() {
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_DELETE || e.keyCode == KeyEvent.VK_BACK_SPACE) {
                    if (selectedNode != null) {
                        deleteSelectedNode()
                    } else if (selectedEdge != null) {
                        deleteSelectedEdge()
                    }
                } else if (e.keyCode == KeyEvent.VK_ESCAPE && isDrawingEdge) {
                    isDrawingEdge = false
                    edgeStartNode = null
                    repaint()
                }
            }
        })
    }
    // 在FlowCanvasPanel类中添加以下方法
    fun addExistingNode(node: FlowNode) {
        // 添加节点但不改变选中状态
        nodes.add(node)
        repaint()
    }

    fun addExistingEdge(edge: FlowEdge) {
        // 添加连接
        edges.add(edge)
        repaint()
    }
    private fun deleteSelectedNode() {
        val node = selectedNode ?: return

        // 移除与该节点相关的所有边
        edges.removeIf { it.sourceNodeId == node.id || it.targetNodeId == node.id }

        // 移除节点
        nodes.removeIf { it.id == node.id }

        // 清除选择
        selectNode(null)

        repaint()
    }

    private fun deleteSelectedEdge() {
        val edge = selectedEdge ?: return

        // 移除边
        edges.removeIf { it.id == edge.id }

        // 清除选择
        selectedEdge = null

        repaint()
    }


    private fun findNodeAt(x: Int, y: Int): FlowNode? {
        // 从后往前检查，以便最上面的节点被选中
        for (i in nodes.size - 1 downTo 0) {
            val node = nodes[i]
            if (x >= node.x && x <= node.x + NODE_WIDTH &&
                y >= node.y && y <= node.y + NODE_HEIGHT
            ) {
                return node
            }
        }
        return null
    }

    private fun findEdgeAt(x: Int, y: Int): FlowEdge? {
        val hitDistance = 5 // 点击边的有效距离

        for (edge in edges) {
            val sourceNode = nodes.find { it.id == edge.sourceNodeId } ?: continue
            val targetNode = nodes.find { it.id == edge.targetNodeId } ?: continue

            val startX = sourceNode.x + NODE_WIDTH / 2
            val startY = sourceNode.y + NODE_HEIGHT / 2
            val endX = targetNode.x + NODE_WIDTH / 2
            val endY = targetNode.y + NODE_HEIGHT / 2

            // 计算点到线段的距离
            val distance = distanceToLine(x.toDouble(), y.toDouble(),
                startX.toDouble(), startY.toDouble(),
                endX.toDouble(), endY.toDouble())

            if (distance <= hitDistance) {
                return edge
            }
        }
        return null
    }

    private fun distanceToLine(x: Double, y: Double, x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val A = x - x1
        val B = y - y1
        val C = x2 - x1
        val D = y2 - y1

        val dot = A * C + B * D
        val lenSq = C * C + D * D
        var param = -1.0

        if (lenSq != 0.0) // 防止除以零
            param = dot / lenSq

        var xx: Double
        var yy: Double

        if (param < 0) {
            xx = x1
            yy = y1
        } else if (param > 1) {
            xx = x2
            yy = y2
        } else {
            xx = x1 + param * C
            yy = y1 + param * D
        }

        val dx = x - xx
        val dy = y - yy

        return Math.sqrt(dx * dx + dy * dy)
    }

    private fun selectNode(node: FlowNode?) {
        selectedNode = node
        selectedEdge = null
        nodeSelectionListener?.invoke(node)
        repaint()

        // 获取焦点以响应键盘事件
        requestFocusInWindow()
    }

    fun setNodeSelectionListener(listener: (FlowNode?) -> Unit) {
        nodeSelectionListener = listener
    }

    fun updateSelectedNode(node: FlowNode) {
        val index = nodes.indexOfFirst { it.id == node.id }
        if (index >= 0) {
            nodes[index] = node
            selectedNode = node
            repaint()
        }
    }



    fun addNewNode(type: NodeType, name: String) {
        // 保存当前选中的节点引用
        val previouslySelectedNode = selectedNode

        // 计算新节点的位置（在视图中央或者随机位置）
        val centerX = width / 2 - NODE_WIDTH / 2
        val centerY = height / 2 - NODE_HEIGHT / 2

        // 添加偏移以避免节点重叠
        val offsetX = (Math.random() * 100 - 50).toInt()
        val offsetY = (Math.random() * 100 - 50).toInt()

        val newNode = FlowNode(
            type = type,
            name = name,
            x = centerX + offsetX,
            y = centerY + offsetY
        )

        // 添加新节点但不改变选中状态
        nodes.add(newNode)

        // 只有当之前没有选中节点时，才选中新节点
        if (previouslySelectedNode == null) {
            selectNode(newNode)
        } else {
            // 否则保持原有选中状态
            repaint()
        }
    }

    fun getNodes(): List<FlowNode> = nodes.toList()

    fun getEdges(): List<FlowEdge> = edges.toList()

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // 设置支持中文的字体
        g2d.font = Font("Microsoft YaHei", Font.PLAIN, 12)

        // 绘制边
        drawEdges(g2d)

        // 绘制临时边（正在绘制中）
        if (isDrawingEdge && edgeStartNode != null) {
            g2d.color = Color.GRAY
            g2d.stroke = BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0f, floatArrayOf(5f), 0f)

            val startX = edgeStartNode!!.x + NODE_WIDTH / 2
            val startY = edgeStartNode!!.y + NODE_HEIGHT / 2

            g2d.draw(Line2D.Double(startX.toDouble(), startY.toDouble(), mouseX.toDouble(), mouseY.toDouble()))
        }

        // 绘制节点
        drawNodes(g2d)

        // 绘制执行顺序
        drawExecutionOrder(g2d)
    }

    private fun drawEdges(g2d: Graphics2D) {
        for (edge in edges) {
            val sourceNode = nodes.find { it.id == edge.sourceNodeId } ?: continue
            val targetNode = nodes.find { it.id == edge.targetNodeId } ?: continue

            val startX = sourceNode.x + NODE_WIDTH / 2
            val startY = sourceNode.y + NODE_HEIGHT / 2
            val endX = targetNode.x + NODE_WIDTH / 2
            val endY = targetNode.y + NODE_HEIGHT / 2

            // 判断是否为选中的边
            if (edge == selectedEdge) {
                g2d.color = Color.RED
                g2d.stroke = BasicStroke(2.5f)
            } else {
                g2d.color = Color.BLACK
                g2d.stroke = BasicStroke(1.5f)
            }

            g2d.draw(Line2D.Double(startX.toDouble(), startY.toDouble(), endX.toDouble(), endY.toDouble()))

            // 绘制箭头
            drawArrow(g2d, startX, startY, endX, endY)
        }
    }

    private fun drawArrow(g2d: Graphics2D, x1: Int, y1: Int, x2: Int, y2: Int) {
        val dx = x2 - x1
        val dy = y2 - y1
        val length = Math.sqrt((dx * dx + dy * dy).toDouble())
        val dirX = dx / length
        val dirY = dy / length

        val arrowSize = 10

        val arrowX = x2 - dirX * arrowSize
        val arrowY = y2 - dirY * arrowSize

        val perpX = -dirY
        val perpY = dirX

        val arrow = Polygon()
        arrow.addPoint(x2, y2)
        arrow.addPoint((arrowX + perpX * arrowSize / 2).toInt(), (arrowY + perpY * arrowSize / 2).toInt())
        arrow.addPoint((arrowX - perpX * arrowSize / 2).toInt(), (arrowY - perpY * arrowSize / 2).toInt())

        g2d.fill(arrow)
    }

    private fun drawNodes(g2d: Graphics2D) {
        for (node in nodes) {
            val isSelected = node == selectedNode

            // 绘制节点背景
            g2d.color = getNodeColor(node.type)
            if (isSelected) {
                g2d.stroke = BasicStroke(2f)
            } else {
                g2d.stroke = BasicStroke(1f)
            }

            val rect =
                Rectangle2D.Double(node.x.toDouble(), node.y.toDouble(), NODE_WIDTH.toDouble(), NODE_HEIGHT.toDouble())
            g2d.fill(rect)

            g2d.color = Color.BLACK
            g2d.draw(rect)

            // 绘制节点类型
            g2d.font = Font("Microsoft YaHei", Font.BOLD, 12)
            g2d.drawString(node.type.displayName, node.x + 10, node.y + 20)

            // 绘制节点名称
            g2d.font = Font("Microsoft YaHei", Font.PLAIN, 12)
            g2d.drawString(node.name, node.x + 10, node.y + 40)

            // 如果是特殊类型节点，显示配置信息
            when (node.type) {
                NodeType.REGEX_PROCESS -> {
                    val config =
                        node.config?.let { "正则: ${if (it.length > 15) it.take(15) + "..." else it}" }
                            ?: "无正则"
                    g2d.drawString(config, node.x + 10, node.y + 60)
                }

                NodeType.AI_MODEL_PROCESS -> {
                    val model = node.config ?: "无模型"
                    g2d.drawString("模型: $model", node.x + 10, node.y + 60)
                }

                else -> {}
            }
        }
    }

    private fun drawExecutionOrder(g2d: Graphics2D) {
        // 计算节点的执行顺序
        val executionOrder = calculateExecutionOrder()

        // 为每个节点绘制执行顺序标记
        for ((nodeId, order) in executionOrder) {
            val node = nodes.find { it.id == nodeId } ?: continue

            // 绘制执行顺序标记
            g2d.color = Color.WHITE
            g2d.fillOval(node.x - 10, node.y - 10, 24, 24)

            g2d.color = Color.BLACK
            g2d.drawOval(node.x - 10, node.y - 10, 24, 24)

            g2d.font = Font("Microsoft YaHei", Font.BOLD, 12)
            val orderStr = order.toString()
            val fm = g2d.fontMetrics
            val textWidth = fm.stringWidth(orderStr)
            g2d.drawString(orderStr, node.x + 2 - textWidth / 2, node.y + 5)
        }

        // 在连接线上绘制顺序标记
        for (edge in edges) {
            val sourceNode = nodes.find { it.id == edge.sourceNodeId } ?: continue
            val targetNode = nodes.find { it.id == edge.targetNodeId } ?: continue

            val sourceOrder = executionOrder[sourceNode.id] ?: continue
            val targetOrder = executionOrder[targetNode.id] ?: continue

            // 计算连接线中点
            val midX = (sourceNode.x + targetNode.x + NODE_WIDTH) / 2
            val midY = (sourceNode.y + targetNode.y + NODE_HEIGHT) / 2

            // 绘制连接顺序标记
            g2d.color = Color.WHITE
            g2d.fillRect(midX - 10, midY - 10, 20, 20)

            g2d.color = Color.BLUE
            g2d.drawRect(midX - 10, midY - 10, 20, 20)

            g2d.font = Font("Microsoft YaHei", Font.BOLD, 10)
            val orderText = "$sourceOrder→$targetOrder"
            val fm = g2d.fontMetrics
            val textWidth = fm.stringWidth(orderText)
            g2d.drawString(orderText, midX - textWidth / 2, midY + 4)
        }
    }

    private fun calculateExecutionOrder(): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        val visited = mutableSetOf<String>()
        var order = 1

        // 找出所有没有入边的节点（起始节点）
        val startNodes = nodes.filter { node ->
            edges.none { it.targetNodeId == node.id }
        }

        // 从每个起始节点开始进行深度优先搜索
        for (startNode in startNodes) {
            dfs(startNode.id, visited, result, order)
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

    private fun dfs(nodeId: String, visited: MutableSet<String>, result: MutableMap<String, Int>, order: Int) {
        if (nodeId in visited) return

        visited.add(nodeId)
        result[nodeId] = order

        // 找出所有从当前节点出发的边
        val outEdges = edges.filter { it.sourceNodeId == nodeId }

        // 递归处理所有相邻节点
        for (edge in outEdges) {
            dfs(edge.targetNodeId, visited, result, order + 1)
        }
    }

    private fun getNodeColor(type: NodeType): Color {
        return when (type) {
            NodeType.GET_CLASS_CONTENT -> Color(173, 216, 230)     // Light Blue
//            NodeType.EXTRACT_FROM_CLASS -> Color(144, 238, 144)    // Light Green
//            NodeType.WRITE_FILE -> Color(255, 182, 193)            // Light Pink
            NodeType.MOUSE_SELECTION -> Color(255, 222, 173)       // Navajo White
//            NodeType.GET_TABLE_STRUCTURE -> Color(221, 160, 221)   // Plum
            NodeType.REGEX_PROCESS -> Color(250, 250, 210)         // Light Goldenrod
            NodeType.AI_MODEL_PROCESS -> Color(255, 160, 122)      // Light Salmon
            NodeType.PROMPT_EDIT -> Color(255, 110, 122)      // Light Salmon
            NodeType.FOLDER_CONTENT_GET -> Color(173, 116, 130)      // Light Salmon
        }
    }

    companion object {
        const val NODE_WIDTH = 150
        const val NODE_HEIGHT = 80
    }
}