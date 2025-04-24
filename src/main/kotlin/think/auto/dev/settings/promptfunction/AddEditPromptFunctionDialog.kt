package think.auto.dev.settings.promptfunction


import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import think.auto.dev.agent.chatpanel.message.gui.whenDisposed
import think.auto.dev.settings.aiProvider.AiProvider
import think.auto.dev.settings.aiProvider.AiProviderDBComponent
import think.auto.dev.settings.language.ThinkAutoDevMessagesBundle
import think.auto.dev.settings.prompt.database.PromptDataBaseComponent
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

class AddEditPromptFunctionDialog(val project: Project?, val promptsComboBox: JComboBox<PromptFunction>, val id: Int?) :
    DialogWrapper(project) {
    private lateinit var titleField: JBTextField
    private lateinit var contentEditor: Editor
    private lateinit var regexField: JBTextField
    private lateinit var hidePromptCombo: JComboBox<Boolean>
    private lateinit var modelCombo: JComboBox<AiProvider>
    private lateinit var historyCombo: JComboBox<String>
    private var promptFunction: PromptFunction? = null

    init {
        if (id == null) {
            title =
                ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.customPromptTemplate.function.add.dialog.title")
        } else {
            title =
                ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.customPromptTemplate.function.edit.dialog.title")
            promptFunction = PromptFunctionDataBaseComponent.getPromptFunctionById(id)
        }
        init()

    }

    override fun createCenterPanel(): JComponent {
        // 主面板使用 BorderLayout
        val mainPanel = JPanel(BorderLayout())
        mainPanel.preferredSize = Dimension(800, 600)

        // 上部面板 - 10%
        val topPanel = createTopPanel()
        mainPanel.add(topPanel, BorderLayout.NORTH)

        // 中部面板 - 使用 JSplitPane 实现可拖动分割
        val centerPanel = createSplitPanePanel()
        mainPanel.add(centerPanel, BorderLayout.CENTER)
        // 下部面板 - 40%
        val bottomPanel = createBottomPanel()
        mainPanel.add(bottomPanel, BorderLayout.SOUTH)
        return mainPanel
    }

    private fun createTopPanel(): JPanel {
        val panel = JPanel(BorderLayout(5, 5))
        panel.preferredSize = Dimension(0, 30) // 10% of 600
        var title = ""
        if (promptFunction != null) {
            title = promptFunction!!.title
        }
        val label =
            JBLabel(ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.customPromptTemplate.function.form.title"))
        titleField = JBTextField(title)

        panel.add(label, BorderLayout.WEST)
        panel.add(titleField, BorderLayout.CENTER)

        return panel
    }


    private fun createSplitPanePanel(): JSplitPane {
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        splitPane.dividerSize = 5
        splitPane.isOneTouchExpandable = true


        // 左侧编辑器 - 初始70%
        // 创建带边框的编辑器
        val newPromptPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            preferredSize = Dimension(1000, 680) // 设置首选宽度
            minimumSize = Dimension(800, 380) // 设置最小宽度
            maximumSize = Dimension(1200, 680) // 设置最大宽度
            border =
                BorderFactory.createTitledBorder(ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.customPromptTemplate.function.form.content"))
        }

        contentEditor = createEditor()
        newPromptPanel.add(JScrollPane(contentEditor.component))
        splitPane.leftComponent = newPromptPanel

        // 右侧表单 - 现在不会被拉伸了
        val formPanel = createFormPanel()
        splitPane.rightComponent = formPanel
        // 设置初始分割比例
        splitPane.preferredSize = Dimension(800, 300)
        splitPane.dividerLocation = 550 // 550:250的比例

        return splitPane
    }

    private fun createEditor(): Editor {
        val editorFactory = EditorFactory.getInstance()
        var content = ""
        if (promptFunction != null) {
            content = promptFunction!!.content
        }

        val document = editorFactory.createDocument(content)
        val editor = editorFactory.createEditor(
            document, project, FileTypeManager.getInstance().getFileTypeByExtension(""), false
        )

        // 注册一个 资源释放回调，当父级 disposable 被销毁时，自动释放编辑器资源，防止内存泄漏。
        disposable.whenDisposed(disposable) {
            EditorFactory.getInstance().releaseEditor(editor)
        }

        (editor as? EditorEx)?.let { ex ->
            // 基本编辑器设置
            ex.settings.isLineNumbersShown = true
            ex.settings.isFoldingOutlineShown = true
            ex.settings.isAutoCodeFoldingEnabled = true
            ex.settings.isRightMarginShown = true

            // 设置特殊文本高亮
            setupSpecialTextHighlighting(ex)

            // 添加文档监听器实现实时高亮
            document.addDocumentListener(object : DocumentListener {
                override fun documentChanged(event: com.intellij.openapi.editor.event.DocumentEvent) {
                    updateHighlighters(ex)
                }
            })
        }

        editor.component.preferredSize = Dimension(0, 300)
        editor.component.minimumSize = Dimension(100, 300)

        return editor
    }

    // 特殊文本高亮设置
    private fun setupSpecialTextHighlighting(editor: EditorEx) {
        // 初始高亮
        updateHighlighters(editor)

        // 添加文档监听器（也可以放在createEditor方法中）
        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: com.intellij.openapi.editor.event.DocumentEvent) {
                updateHighlighters(editor)
            }
        })
    }

    // 更新高亮器
    private fun updateHighlighters(editor: EditorEx) {
        val markupModel = editor.markupModel
        markupModel.removeAllHighlighters()

        val text = editor.document.charsSequence
        val variablePattern = Regex("""\$\{(\w+)}""")

        // 定义不同变量的样式
        val attributesMap = mapOf(
            "SPEC_" to TextAttributes().apply { foregroundColor = Color(199, 104, 19) }, // 棕红色
            "SELECTION" to TextAttributes().apply { foregroundColor = Color(0, 0, 255) }, // 蓝色
        )

        variablePattern.findAll(text).forEach { match ->
            val variableName = match.groupValues[1]
            val attributes = attributesMap.entries.find {
                variableName.startsWith(it.key) || variableName == it.key
            }?.value ?: attributesMap["DEFAULT"]!!

            markupModel.addRangeHighlighter(
                match.range.first,
                match.range.last + 1,
                HighlighterLayer.SYNTAX,
                attributes,
                HighlighterTargetArea.EXACT_RANGE
            )
        }
    }


    private fun createFormPanel(): JPanel {
        // 主面板使用 BorderLayout，这样我们可以控制内容的位置
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

        // 内容面板使用垂直 BoxLayout
        val contentPanel = JPanel()
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)

        // 创建紧凑的表单行
        fun createCompactRow(labelText: String, component: JComponent): JPanel {
            val rowPanel = JPanel(BorderLayout(5, 5))
            rowPanel.add(JBLabel(labelText), BorderLayout.WEST)

            // 固定组件大小
            component.preferredSize = Dimension(150, component.preferredSize.height)
            component.maximumSize = Dimension(150, component.preferredSize.height)

            rowPanel.add(component, BorderLayout.CENTER)
            return rowPanel
        }


        var defaultConfig = PromptFunctionConfig(".*", true, 0, null)
        if (promptFunction != null) {
            val config = promptFunction!!.config
            val functionConfig = Json.decodeFromString(PromptFunctionConfig.serializer(), config)
            defaultConfig = PromptFunctionConfig(
                functionConfig.selectedMatchRegex,
                functionConfig.hidePrompt,
                functionConfig.useAiModel,
                functionConfig.loadChatContext
            )
        }

        // 第一行：正则过滤
        regexField = JBTextField(defaultConfig.selectedMatchRegex)
        contentPanel.add(createCompactRow("正则过滤选中内容:", regexField))
        contentPanel.add(Box.createVerticalStrut(10))

        // 第二行：是否隐藏提示词
        hidePromptCombo = JComboBox(arrayOf(true, false))
        hidePromptCombo.selectedIndex = arrayOf(true, false).indexOf(defaultConfig.hidePrompt)
        contentPanel.add(createCompactRow("是否隐藏提问提示词:", hidePromptCombo))
        contentPanel.add(Box.createVerticalStrut(10))

        // 第三行：AI模型选择
        val allAiProviders = AiProviderDBComponent.getAllAiProviders()
        val allAiProviderArr = allAiProviders.toTypedArray()
        modelCombo = JComboBox(allAiProviderArr)
        modelCombo.selectedIndex = allAiProviderArr.map { v -> v.id }.toTypedArray().indexOf(defaultConfig.useAiModel)
        contentPanel.add(createCompactRow("使用AI模型:", modelCombo))
        contentPanel.add(Box.createVerticalStrut(10))

        // 第四行：历史上下文
//        historyCombo = JComboBox(arrayOf("xxx", "curd代码"))
//        contentPanel.add(createCompactRow("载入聊天历史上下文:", historyCombo))

        // 将内容面板放在主面板的北部，这样不会拉伸
        panel.add(contentPanel, BorderLayout.NORTH)

        // 设置面板的最小大小
        panel.minimumSize = Dimension(240, 300)

        return panel
    }

    /**
     * 提示词列表
     */
    private fun createBottomPanel(): JPanel {
        val panel = JPanel().apply {
            preferredSize = Dimension(500, 180) // 设置首选宽度
            minimumSize = Dimension(500, 180) // 设置最小宽度
            border =
                BorderFactory.createTitledBorder(ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.customPromptTemplate.list.title"))
        }

        panel.layout = FlowLayout(FlowLayout.LEFT, 5, 5)
        panel.preferredSize = Dimension(0, 240) // 40% of 600

        // 创建按钮并添加点击事件
        val selectionButton = JButton("鼠标选中内容")
        selectionButton.addActionListener {
            val text = "SELECTION"
            appendToEditor("\${$text}")
        }

        panel.add(selectionButton)

        val allPrompts = PromptDataBaseComponent.getAllPrompts()
        allPrompts.forEach {
            val title = it.title
            val buttonPrompt = JButton(title)
            buttonPrompt.addActionListener {
                appendToEditor("\${SPEC_$title}")
            }
            panel.add(buttonPrompt)
        }
        return panel
    }

    private fun appendToEditor(text: String) {
        // 确保在 EDT 上执行
        ApplicationManager.getApplication().invokeLater {
            // 使用 WriteCommandAction 确保在写入状态下
            WriteCommandAction.runWriteCommandAction(project) {
                // 获取文档
                val document = contentEditor.document
                // 获取光标模型
                val caretModel = contentEditor.caretModel
                val currentCaret = caretModel.currentCaret
                // 当前光标位置
                val currentOffset = currentCaret.offset
                // 在当前光标位置插入新文本
                document.insertString(currentOffset, text)
                // 移动光标到新插入文本的末尾
                val newOffset = currentOffset + text.length
                // 移动光标到新插入文本的末尾
                caretModel.moveToOffset(newOffset)
                // 选中插入的文本
                currentCaret.setSelection(currentOffset, newOffset)
            }
        }
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return titleField
    }

    override fun doValidate(): ValidationInfo? {
        if (titleField.text.isNullOrBlank()) {
            return ValidationInfo(
                ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.customPromptTemplate.function.form.error.message.title"),
                titleField
            )
        }

        if (contentEditor.document.text.isBlank()) {
            return ValidationInfo(
                ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.customPromptTemplate.function.form.error.message.content"),
                contentEditor.component
            )
        }

        if (modelCombo.selectedItem == null) {
            return ValidationInfo(
                ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.ai.provider.form.add.error.message.save"),
                modelCombo
            )
        }

        if (regexField.text == null || regexField.text.isNullOrBlank()) {
            return ValidationInfo(
                ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.ai.provider.form.add.regex.error.message.save"),
                regexField
            )
        }

        val promptFunction = PromptFunctionDataBaseComponent.getPromptFunctionByTitle(titleField.text)
        // 存在重复的
        if (promptFunction != null && (id == null || id != promptFunction.id)) {
            return ValidationInfo(
                ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.customPromptTemplate.function.error.message.repeat"),
                titleField
            )
        }

        return super.doValidate()
    }

    override fun doOKAction() {
        val aiProvider = modelCombo.selectedItem as AiProvider
        if (aiProvider.id == null) {
            return
        }

        val hidePrompt = hidePromptCombo.selectedItem as Boolean
        val promptFunctionConfig = PromptFunctionConfig(regexField.text, hidePrompt, aiProvider.id, null)
        val promptFunctionConfigStr = Json.encodeToString(promptFunctionConfig)
        val promptFunction = PromptFunction(id, titleField.text, contentEditor.document.text, promptFunctionConfigStr)
        if (id == null) {
            PromptFunctionDataBaseComponent.insertPromptFunction(promptFunction)
        } else {
            PromptFunctionDataBaseComponent.updatePromptFunction(promptFunction)
        }

        refreshPromptFunctionComboBox(promptsComboBox)
        super.doOKAction()
    }

    /**
     * 刷新
     */
    private fun refreshPromptFunctionComboBox(comboBox: JComboBox<PromptFunction>) {
        // 清空现有数据
        comboBox.removeAllItems()
        val getAllPrompts = PromptFunctionDataBaseComponent.getAllPrompts()
        for (getAllPrompt in getAllPrompts) {
            comboBox.addItem(getAllPrompt)
        }

        // 刷新显示（可选）
        comboBox.revalidate()
        comboBox.repaint()
    }
}