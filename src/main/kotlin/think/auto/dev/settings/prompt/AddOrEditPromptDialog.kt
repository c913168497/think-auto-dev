package think.auto.dev.settings.prompt

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import think.auto.dev.agent.chatpanel.message.gui.whenDisposed
import think.auto.dev.settings.language.ThinkAutoDevMessagesBundle
import think.auto.dev.settings.prompt.database.Prompt
import think.auto.dev.settings.prompt.database.PromptDataBaseComponent
import java.awt.Dimension
import javax.swing.*

class AddOrEditPromptDialog(project: Project, val comboBox: JComboBox<Prompt>, val id: Int?) : DialogWrapper(project) {

    var promptTitle: String? = null

    private lateinit var promptTitleField: JTextField

    private lateinit var promptContentEditor: EditorEx

    private var templateNodePanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS) // 设置垂直布局
    }

    private var mainPanel = JPanel()
    private val editorFactory = EditorFactory.getInstance()

    init {
        if (id == null) {
            title =
                ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.customPromptTemplate.add.dialog.title")
        } else {
            title =
                ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.customPromptTemplate.edit.dialog.title")
        }

        init()
        setModal(false)
    }

    override fun createCenterPanel(): JComponent {
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
        mainPanel.border = JBUI.Borders.empty(10)
        val headPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            maximumSize = Dimension(Integer.MAX_VALUE, 50)
        }
        // 提示词标题
        val promptTitleLabel =
            JLabel(ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.customPromptTemplate.form.title"))
        promptTitleField = JTextField(30)
        // 初始化参数
        paramInit(id)
        headPanel.add(promptTitleLabel)
        headPanel.add(promptTitleField)
        mainPanel.add(headPanel)
        mainPanel.add(Box.createVerticalStrut(10))
        mainPanel.add(templateNodePanel)
        // 将 mainPanel 包裹在一个滚动面板中
        return JBScrollPane(mainPanel).apply {
            preferredSize = Dimension(1200, 800)
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }

    }

    /**
     * 参数初始化
     */
    private fun paramInit(id: Int?) {
        if (id == null) {
            initPromptContentEditor("")
            return
        }

        val prompt = PromptDataBaseComponent.getPromptsById(id) ?: return
        promptTitleField.text = prompt.title
        initPromptContentEditor(prompt.content)
    }

    private fun initPromptContentEditor(content: String): JPanel {
        // 创建新的 newPromptPanel
        val newPromptPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            maximumSize = Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE) // 设置最大高度，防止组件过高
        }

        val document = editorFactory.createDocument(content)
        // 创建新的 Editor 和按钮
        promptContentEditor = editorFactory.createEditor(
            document,
            null,
            FileTypeManager.getInstance().getFileTypeByExtension(""),
            false
        ) as EditorEx

        // 注册一个 资源释放回调，当父级 disposable 被销毁时，自动释放编辑器资源，防止内存泄漏。
        disposable.whenDisposed(disposable) {
            EditorFactory.getInstance().releaseEditor(promptContentEditor)
        }

        promptContentEditor.component.minimumSize = Dimension(800, 100)
        // 添加组件前，给组件设置一个属性
        promptContentEditor.setHorizontalScrollbarVisible(true)
        promptContentEditor.setVerticalScrollbarVisible(true)
        promptContentEditor.component.border =
            BorderFactory.createTitledBorder(ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.customPromptTemplate.form.content"))
        newPromptPanel.add(promptContentEditor.component)
        newPromptPanel.add(Box.createHorizontalStrut(10))
        // 在当前 anchorPanel 之后插入新的 anchorPanel
        templateNodePanel.add(newPromptPanel)
        // 添加垂直间隔
        templateNodePanel.add(Box.createVerticalStrut(10))
        templateNodePanel.revalidate()
        templateNodePanel.repaint()
        return newPromptPanel
    }

    override fun doOKAction() {
        promptTitle = promptTitleField.text
        val content = promptContentEditor.document.text
        if (id == null) {
            PromptDataBaseComponent.insertPrompts(Prompt(title = promptTitle!!, content = content))
        } else {
            PromptDataBaseComponent.updatePrompts(Prompt(id = id, title = promptTitle!!, content = content))
        }

        refreshPromptComboBox(comboBox)
        super.doOKAction()
    }

    override fun doValidate(): ValidationInfo? {
        if (promptTitleField.text.isNullOrEmpty()) {
            return ValidationInfo(
                ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.customPromptTemplate.form.error.message.content"),
                promptTitleField
            )
        }

        val prompts = PromptDataBaseComponent.getPromptsByTitle(promptTitleField.text)
        // 存在重复的
        if (prompts != null && (id == null || id != prompts.id)) {
            return ValidationInfo(
                ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.customPromptTemplate.form.error.message.repeat"),
                promptTitleField
            )
        }

        return super.doValidate()
    }

    private fun refreshPromptComboBox(comboBox: JComboBox<Prompt>) {
        // 清空现有数据
        comboBox.removeAllItems()
        val getAllPrompts = PromptDataBaseComponent.getAllPrompts()
        for (getAllPrompt in getAllPrompts) {
            comboBox.addItem(getAllPrompt)
        }

        // 刷新显示（可选）
        comboBox.revalidate()
        comboBox.repaint()
    }
}
