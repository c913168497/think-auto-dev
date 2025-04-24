package think.auto.dev.agent.chatpanel


import cn.hutool.core.util.IdUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.observable.util.whenDisposed
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.NullableComponent
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import think.auto.dev.agent.chatcontext.ChatPrompt
import think.auto.dev.llm.role.ChatRole
import think.auto.dev.llm.role.ChatRole.User
import think.auto.dev.settings.aiProvider.AiProvider
import think.auto.dev.settings.chatcontext.ChatContextDataBaseComponent
import think.auto.dev.settings.chatcontext.ChatContextItem
import think.auto.dev.settings.chatcontext.ChatContextTitle
import think.auto.dev.utils.fullHeight
import think.auto.dev.utils.fullWidth
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.ScrollPaneConstants
import javax.swing.SwingUtilities

class ChatCodingPanel(
    private val chatCodingService: ChatCodingService,
    val aiProvider: AiProvider,
    var disposable: Disposable?
) :
    SimpleToolWindowPanel(true, true),
    NullableComponent {
    private val logger = logger<ChatCodingPanel>()

    private var progressBar: JProgressBar
    private val myTitle = JBLabel("Conversation")
    private val myList = JPanel(VerticalLayout(JBUI.scale(10)))
    private var inputSection: ChatPanelInputSection
    private val focusMouseListener: MouseAdapter
    private var panelContent: DialogPanel
    private val myScrollPane: JBScrollPane
    private var suggestionPanel: JPanel = JPanel(BorderLayout())
    private var chatHistoryContext: MutableList<ChatContextItem> = ArrayList()

    init {
        focusMouseListener = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                focusInput()
            }
        }
        // 欢迎
        myList.add(WelcomePanel())

        myTitle.foreground = JBColor.namedColor("Label.infoForeground", JBColor(Gray.x80, Gray.x8C))
        myTitle.font = JBFont.label()

        myList.isOpaque = true
        myList.background = UIUtil.getListBackground()

        myScrollPane = JBScrollPane(
            myList,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        )

        myScrollPane.verticalScrollBar.autoscrolls = false
        myScrollPane.background = UIUtil.getListBackground()
        progressBar = JProgressBar()
        inputSection = ChatPanelInputSection(chatCodingService.project, disposable)
        inputSection.addListener(object : ChatPanelInputListener {
            override fun onStop(component: ChatPanelInputSection) {
                chatCodingService.stop()
                hiddenProgressBar()
            }

            override fun onSubmit(component: ChatPanelInputSection, trigger: ChatPanelInputTrigger) {
                var prompt = component.text
                component.text = ""

                if (prompt.isEmpty() || prompt.isBlank()) {
                    return
                }

                val chatPrompt = ChatPrompt(prompt, prompt)
                chatCodingService.handlePromptAndResponse(this@ChatCodingPanel, chatPrompt, false)
                val toolWindowManager = ThinkAutoDevToolWindowFactory.getToolWindow(chatCodingService.project) ?: run {
                    logger<ChatCodingService>().warn("Tool window not found")
                    return
                }

                val contentManager = toolWindowManager.contentManager
                val selectedContent = contentManager.selectedContent
                if (selectedContent == null) {
                    return
                }
                val title = prompt.substring(0, 5)
                selectedContent.displayName = title
            }
        })

        panelContent = panel {
            row { cell(myScrollPane).fullWidth().fullHeight() }.resizableRow()
            row { cell(suggestionPanel).fullWidth() }
            row { cell(progressBar).fullWidth() }
            row {
                border = JBUI.Borders.empty(8)
                cell(inputSection).fullWidth()
            }
        }

        setContent(panelContent)

        disposable?.whenDisposed(disposable!!) {
            myList.removeAll()
        }
    }

    fun focusInput() {
        val focusManager = IdeFocusManager.getInstance(chatCodingService.project)
        focusManager.doWhenFocusSettlesDown {
            focusManager.requestFocus(this.inputSection.focusableComponent, true)
        }
    }

    fun getRole(role: String): ChatRole {
        return when (role) {
            "System" -> ChatRole.System
            "Assistant" -> ChatRole.Assistant
            "User" -> ChatRole.User
            else -> {
                User
            }
        }
    }

    fun loadHistoryMessage(historyChatMessages: List<ChatContextItem>) {
        if (historyChatMessages.isEmpty()) {
            return
        }
        val chatContextItem = historyChatMessages[0]
        val titleId = chatContextItem.titleId
        historyChatMessages.forEach {
            if (it.role == ChatRole.Assistant.roleName()) {
                val messageView = ChatMessageView("", ChatRole.Assistant, "")
                messageView.updateContent(it.content)
                myList.add(messageView)
                chatHistoryContext.add(it)
                messageView.reRenderAssistantOutput()
                updateUI()
            } else {
                val messageView = ChatMessageView(it.content, User, it.content)
                myList.add(messageView)
                chatHistoryContext.add(it)
            }

        }

        updateLayout()
        progressBar.isIndeterminate = true
        updateUI()
    }

    /**
     * Add a message to the chat panel and update ui
     */
    fun addMessage(
        message: String,
        isMe: Boolean = false,
        displayPrompt: String = "",
        isSystem: Boolean = false
    ): ChatMessageView {
        val role = if (isMe) ChatRole.User else ChatRole.Assistant
        val displayText = displayPrompt.ifEmpty { message }
        val messageView = ChatMessageView(message, role, displayText)
        // 添加到历史聊天记录中
        if (!isSystem) {
            aiProvider.id?.let { saveChatMessage(role, displayText, aiProviderId = it) }
        }

        myList.add(messageView)
        updateLayout()
        scrollToBottom()
        progressBar.isIndeterminate = true
        updateUI()
        return messageView
    }

    /**
     * 保存聊天信息到数据库
     */
    private fun saveChatMessage(role: ChatRole, message: String, aiProviderId: Int) {
        ApplicationManager.getApplication().invokeLater {
            if (chatHistoryContext.isEmpty()) {
                var title = message
                if (message.length > 4) {
                    title = message.substring(0, 5)
                }

                val titleId = IdUtil.getSnowflake().nextId()
                val chatContextTitle = ChatContextTitle(id = titleId, title = title, aiProviderId = aiProviderId)
                ChatContextDataBaseComponent.insertChatContextTitles(chatContextTitle)
                val currentTime = Date().time
                val chatContextItem = ChatContextItem(
                    titleId = titleId,
                    role = role.roleName(),
                    content = message,
                    createTime = currentTime
                )

                chatHistoryContext.add(chatContextItem)
                ChatContextDataBaseComponent.insertChatContextItem(chatContextItem)
            } else {
                val firstMessage = chatHistoryContext[0]
                val titleId = firstMessage.titleId
                val currentTime = Date().time
                val chatContextItem = ChatContextItem(
                    titleId = titleId,
                    role = role.roleName(),
                    content = message,
                    createTime = currentTime
                )

                ChatContextDataBaseComponent.insertChatContextItem(chatContextItem)
            }


        }
    }

    public fun getChatHistoryContext(): MutableList<ChatContextItem> {
        return chatHistoryContext
    }

    private fun updateLayout() {
        val layout = myList.layout
        for (i in 0 until myList.componentCount) {
            layout.removeLayoutComponent(myList.getComponent(i))
            layout.addLayoutComponent(null, myList.getComponent(i))
        }
    }

    suspend fun updateMessage(content: Flow<String>): String {
        if (myList.componentCount > 0) {
            myList.remove(myList.componentCount - 1)
        }

        showProgressBar()

        val result = updateMessageInUi(content)

        progressBar.isIndeterminate = false
        hiddenProgressBar()
        updateUI()

        // 添加到历史聊天记录中
        aiProvider.id?.let { saveChatMessage(ChatRole.Assistant, result, it) }
        return result
    }

    private fun scrollToBottom() {
        SwingUtilities.invokeLater {
            val verticalScrollBar = myScrollPane.verticalScrollBar
            verticalScrollBar.value = verticalScrollBar.maximum
        }
    }

    override fun isNull(): Boolean {
        return !isVisible
    }

    suspend fun updateReplaceableContent(content: Flow<String>, postAction: (text: String) -> Unit) {
        myList.remove(myList.componentCount - 1)
        showProgressBar()
        val text = updateMessageInUi(content)

        progressBar.isIndeterminate = false
        hiddenProgressBar()
        updateUI()

        postAction(text)
    }

    private suspend fun updateMessageInUi(content: Flow<String>): String {
        val messageView = ChatMessageView("", ChatRole.Assistant, "")
        myList.add(messageView)
        val startTime = System.currentTimeMillis()
        var text = ""
        val batchSize = 50
        val buffer = mutableListOf<String>()

        content
            .buffer(capacity = 80)
            .collect { newText ->
                buffer.add(newText)
                if (buffer.size >= batchSize) {
                    text += buffer.joinToString("")
                    buffer.clear()
                    messageView.updateContent(text)
                }
            }

        // 处理剩余的缓冲内容
        if (buffer.isNotEmpty()) {
            text += buffer.joinToString("")
            messageView.updateContent(text)
        }

        messageView.reRenderAssistantOutput()
        return text
    }

    fun setInput(trimMargin: String) {
        inputSection.text = trimMargin
        this.focusInput()
    }

    /**
     * Resets the chat session by clearing the current session and updating the UI.
     */
    fun resetChatSession() {
        chatCodingService.stop()
        suggestionPanel.removeAll()
        chatCodingService.clearSession()
        myList.removeAll()
        myList.add(WelcomePanel())
        this.hiddenProgressBar()
        updateUI()
    }

    fun hiddenProgressBar() {
        progressBar.isVisible = false
        inputSection.showSendButton()
    }

    fun showProgressBar() {
        progressBar.isVisible = true
        inputSection.showStopButton()
    }

    fun removeLastMessage() {
        if (myList.componentCount > 0) {
            myList.remove(myList.componentCount - 1)
        }

        updateUI()
    }

    fun moveCursorToStart() {
        inputSection.moveCursorToStart()
    }

    fun showSuggestion(msg: String) {
        val label = panel {
            row {
                link(msg) {
                    inputSection.text = msg
                    inputSection.requestFocus()

                    suggestionPanel.removeAll()
                    updateUI()
                }.also {
                    it.component.foreground = JBColor.namedColor("Link.activeForeground", JBColor(Gray.x80, Gray.x8C))
                }
            }
        }

        suggestionPanel.add(label)
        updateUI()
    }
}

