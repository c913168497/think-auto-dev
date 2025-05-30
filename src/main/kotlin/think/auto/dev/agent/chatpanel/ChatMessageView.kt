package think.auto.dev.agent.chatpanel


import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import think.auto.dev.agent.chatpanel.message.*
import think.auto.dev.agent.chatpanel.message.gui.ChatPanelCodeBlockView
import think.auto.dev.agent.chatpanel.message.gui.ChatPanelTextBlockView
import think.auto.dev.llm.role.ChatRole
import java.awt.*
import javax.swing.*
import kotlin.jvm.internal.Ref

class ChatMessageView(private val message: String, val role: ChatRole, private val displayText: String) :
    JBPanel<ChatMessageView>(), DataProvider {
    private val myNameLabel: Component
    private val component: ChatMessageDisplayComponent = ChatMessageDisplayComponent(message)
    private var centerPanel: JPanel = JPanel(VerticalLayout(JBUI.scale(8)))
    private var messageView: ChatSimpleMessage? = null
    private var answer: String = ""

    init {
        isDoubleBuffered = true
        isOpaque = true
        background = when (role) {
            ChatRole.System -> JBColor(0xEAEEF7, 0x45494A)
            ChatRole.Assistant -> JBColor(0xEAEEF7, 0x2d2f30)
            ChatRole.User -> JBColor(0xE0EEF7, 0x2d2f30)
        }

        val authorLabel = JLabel()
        authorLabel.setFont(JBFont.h4())
        authorLabel.setText(role.getRoleName(role))
        myNameLabel = authorLabel

        this.border = JBEmptyBorder(8)
        layout = BorderLayout(JBUI.scale(8), 0)

        centerPanel = JPanel(VerticalLayout(JBUI.scale(8)))
        centerPanel.isOpaque = false
        centerPanel.border = JBUI.Borders.emptyRight(8)

        centerPanel.add(myNameLabel)
        add(centerPanel, BorderLayout.CENTER)

        if (role == ChatRole.User) {
            ApplicationManager.getApplication().invokeLater {
                val simpleMessage = ChatSimpleMessage(displayText, message, role)
                messageView = simpleMessage
                renderInPartView(simpleMessage)
            }
        } else {
            component.updateMessage(message)
            component.revalidate()
            component.repaint()
            centerPanel.add(component)
        }
    }

    /**
     * 创建 title
     */
    private fun createTitlePanel(): JPanel {
        val panel = BorderLayoutPanel()
        panel.setOpaque(false)
        panel.addToCenter(this.myNameLabel)

        val group = ActionUtil.getActionGroup("AutoDev.ToolWindow.Message.Toolbar.Assistant")

        if (group != null) {
            val toolbar = ActionToolbarImpl(javaClass.getName(), group, true)
            toolbar.component.setOpaque(false)
            toolbar.component.setBorder(JBUI.Borders.empty())
            toolbar.targetComponent = this
            panel.addToRight(toolbar.component)
        }

        panel.setOpaque(false)
        return panel
    }

    private fun renderInPartView(message: ChatSimpleMessage) {
        val parts = layoutAll(message)
        parts.forEach {
            val blockView = when (it) {
                is CompletableMessageCodeBlock -> {
                    val project = ProjectManager.getInstance().openProjects.firstOrNull()
                    ChatPanelCodeBlockView(it, project!!) { }
                }

                else -> ChatPanelTextBlockView(it)
            }

            blockView.initialize()
            val component = blockView.getComponent() ?: return@forEach

            component.setForeground(JBUI.CurrentTheme.Label.foreground())
            centerPanel.add(component)
        }
    }


    fun updateContent(content: String) {
        this.answer = content
        MessageWorker(content).execute()
    }

    fun scrollToBottom() {
        SwingUtilities.invokeLater {
            val bounds: Rectangle = bounds
            scrollRectToVisible(bounds)
        }
    }

    fun reRenderAssistantOutput() {
        ApplicationManager.getApplication().invokeLater {
            centerPanel.remove(component)
            centerPanel.updateUI()

            centerPanel.add(myNameLabel)
            centerPanel.add(createTitlePanel())

            val message = ChatSimpleMessage(answer, answer, ChatRole.Assistant)
            this.messageView = message
            renderInPartView(message)

            centerPanel.revalidate()
            centerPanel.repaint()
        }
    }

    internal inner class MessageWorker(private val message: String) : SwingWorker<Void?, String?>() {
        @Throws(Exception::class)
        override fun doInBackground(): Void? {
            return null
        }

        override fun done() {
            try {
                get()
                component.updateMessage(message)
                component.updateUI()
            } catch (e: Exception) {
                logger.error(message, e.message)
            }
        }
    }

    companion object {
        private val logger = logger<ChatMessageView>()
        private fun createPart(
            blockStart: Int,
            partUpperOffset: Int,
            messageText: String,
            currentContextType: Ref.ObjectRef<MessageBlockType>,
            message: CompletableMessage
        ): MessageBlock {
            check(blockStart < messageText.length)
            check(partUpperOffset < messageText.length)

            val blockText = messageText.substring(blockStart, partUpperOffset + 1)
            val part: MessageBlock = when (currentContextType.element!!) {
                MessageBlockType.CodeEditor -> CompletableMessageCodeBlock(message)
                MessageBlockType.PlainText -> CompletableMessageTextBlock(message)
            }

            if (blockText.isNotEmpty()) {
                part.addContent(blockText)
            }

            return part
        }

        private fun pushPart(
            blockStart: Ref.IntRef,
            messageText: String,
            currentContextType: Ref.ObjectRef<MessageBlockType>,
            message: CompletableMessage,
            list: MutableList<MessageBlock>,
            partUpperOffset: Int
        ) {
            val newPart = createPart(blockStart.element, partUpperOffset, messageText, currentContextType, message)
            list.add(newPart)

            blockStart.element = partUpperOffset + 1
            currentContextType.element = MessageBlockType.PlainText
        }

        fun layoutAll(message: CompletableMessage): List<MessageBlock> {
            val messageText: String = message.displayText
            val contextTypeRef = Ref.ObjectRef<MessageBlockType>()
            contextTypeRef.element = MessageBlockType.PlainText

            val blockStart: Ref.IntRef = Ref.IntRef()

            val parts = mutableListOf<MessageBlock>()

            for ((index, item) in messageText.withIndex()) {
                val param = Parameters(item, index, messageText)
                val processor = MessageCodeBlockCharProcessor()
                val suggestTypeChange =
                    processor.suggestTypeChange(param, contextTypeRef.element, blockStart.element) ?: continue

                when {
                    suggestTypeChange.contextType == contextTypeRef.element -> {
                        if (suggestTypeChange.borderType == BorderType.START) {
                            logger.error("suggestTypeChange return ${contextTypeRef.element} START while there is already ${contextTypeRef.element} opened")
                        } else {
                            pushPart(blockStart, messageText, contextTypeRef, message, parts, index)
                        }
                    }

                    suggestTypeChange.borderType == BorderType.START -> {
                        if (index > blockStart.element) {
                            pushPart(blockStart, messageText, contextTypeRef, message, parts, index - 1)
                        }

                        blockStart.element = index
                        contextTypeRef.element = suggestTypeChange.contextType
                    }

                    else -> {
                        logger.error("suggestTypeChange return ${contextTypeRef.element} END when there wasn't open tag")
                    }
                }
            }

            if (blockStart.element < messageText.length) {
                pushPart(blockStart, messageText, contextTypeRef, message, parts, messageText.length - 1)
            }

            return parts
        }

    }

    override fun getData(dataId: String): CompletableMessage? {
        return if (CompletableMessage.key.`is`(dataId)) {
            return this.messageView
        } else {
            null
        }
    }
}
