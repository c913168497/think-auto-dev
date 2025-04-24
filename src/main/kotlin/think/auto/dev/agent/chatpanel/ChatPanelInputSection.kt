package think.auto.dev.agent.chatpanel

import com.intellij.ide.IdeTooltip
import com.intellij.ide.IdeTooltipManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon.Position
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.impl.InternalDecorator
import com.intellij.ui.HintHint
import com.intellij.util.EventDispatcher
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import think.auto.dev.settings.language.ThinkAutoDevMessagesBundle
import think.auto.dev.utils.ThinkAutoDevIcons
import java.awt.CardLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Box
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.min

/**
 *
 */
class ChatPanelInputSection(private val project: Project, val disposable: Disposable?) : BorderLayoutPanel() {
    private val chatPanelInput: ChatPanelInput
    private val documentListener: DocumentListener
    private val sendButtonPresentation: Presentation
    private val stopButtonPresentation: Presentation
    private val sendButton: ActionButton
    private val stopButton: ActionButton
    private val buttonPanel = JPanel(CardLayout())

    private val logger = logger<ChatPanelInputSection>()

    val editorListeners = EventDispatcher.create(ChatPanelInputListener::class.java)

    var text: String
        get() {
            return chatPanelInput.text
        }
        set(text) {
            chatPanelInput.recreateDocument()
            chatPanelInput.text = text
        }

    init {
        chatPanelInput = ChatPanelInput(project, listOf(), disposable, this)
        val sendButtonPresentation = Presentation(ThinkAutoDevMessagesBundle.message("chat.panel.send"))
        sendButtonPresentation.setIcon(ThinkAutoDevIcons.Send)
        this.sendButtonPresentation = sendButtonPresentation
        val stopButtonPresentation = Presentation("Stop")
        stopButtonPresentation.setIcon(ThinkAutoDevIcons.Stop)
        this.stopButtonPresentation = stopButtonPresentation
        sendButton = ActionButton(
            DumbAwareAction.create {
                object : DumbAwareAction("") {
                    override fun actionPerformed(e: AnActionEvent) {
                        editorListeners.multicaster.onSubmit(this@ChatPanelInputSection, ChatPanelInputTrigger.Button)
                    }
                }.actionPerformed(it)
            },
            this.sendButtonPresentation,
            "",
            Dimension(20, 20)
        )

        stopButton = ActionButton(
            DumbAwareAction.create {
                object : DumbAwareAction("") {
                    override fun actionPerformed(e: AnActionEvent) {
                        editorListeners.multicaster.onStop(this@ChatPanelInputSection)
                    }
                }.actionPerformed(it)
            },
            this.stopButtonPresentation,
            "",
            Dimension(20, 20)
        )



        documentListener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val i = chatPanelInput.preferredSize?.height
                if (i != chatPanelInput.height) {
                    revalidate()
                }
            }
        }

        chatPanelInput.recreateDocument()
        chatPanelInput.addDocumentListener(documentListener)
        chatPanelInput.border = JBEmptyBorder(4)
        addToCenter(chatPanelInput)
        val layoutPanel = BorderLayoutPanel()
        val horizontalGlue = Box.createHorizontalGlue()
        horizontalGlue.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                IdeFocusManager.getInstance(project).requestFocus(chatPanelInput, true)
                chatPanelInput.caretModel.moveToOffset(chatPanelInput.text.length - 1)
            }
        })
        layoutPanel.setOpaque(false)

        buttonPanel.add(sendButton, "Send")
        buttonPanel.add(stopButton, "Stop")

        layoutPanel.addToCenter(horizontalGlue)
        layoutPanel.addToRight(buttonPanel)
        addToBottom(layoutPanel)
        sendButton.setEnabled(true)
        addListener(object : ChatPanelInputListener {
            override fun editorAdded(editor: EditorEx) {
                this@ChatPanelInputSection.initEditor()
            }
        })
    }

    fun showStopButton() {
        (buttonPanel.layout as? CardLayout)?.show(buttonPanel, "Stop")
        stopButton.isEnabled = true
    }

    fun showTooltip(text: @NlsContexts.Tooltip String) {
        showTooltip(chatPanelInput, Position.above, text)
    }

    fun showTooltip(component: JComponent, position: Position, text: @NlsContexts.Tooltip String) {
        val point = Point(component.x, component.y)
        val tipComponent = IdeTooltipManager.initPane(
            text, HintHint(component, point).setAwtTooltip(true).setPreferredPosition(position), null
        )
        val tooltip = IdeTooltip(component, point, tipComponent)
        IdeTooltipManager.getInstance().show(tooltip, true)
    }

    fun showSendButton() {
        (buttonPanel.layout as? CardLayout)?.show(buttonPanel, "Send")
        buttonPanel.isEnabled = true
    }


    fun initEditor() {
        val editorEx = this.chatPanelInput.editor as? EditorEx ?: return
        setBorder(ChatPanelCoolBorder(editorEx, this))
        UIUtil.setOpaqueRecursively(this, false)
        this.revalidate()
    }

    override fun getPreferredSize(): Dimension {
        val result = super.getPreferredSize()
        result.height = max(min(result.height, maxHeight), minimumSize.height)
        return result
    }

    fun setContent(trimMargin: String) {
        val focusManager = IdeFocusManager.getInstance(project)
        focusManager.requestFocus(chatPanelInput, true)
        chatPanelInput.recreateDocument()
        this.chatPanelInput.text = trimMargin
    }

    override fun getBackground(): Color? {
        if (this.chatPanelInput == null) return super.getBackground()

        val editor = chatPanelInput.editor ?: return super.getBackground()
        return editor.colorsScheme.defaultBackground
    }

    override fun setBackground(bg: Color?) {}

    fun addListener(listener: ChatPanelInputListener) {
        editorListeners.addListener(listener)
    }

    /**
     *
     */
    fun moveCursorToStart() {
        chatPanelInput.caretModel.moveToOffset(0)
    }


    private val maxHeight: Int
        get() {
            val decorator = UIUtil.getParentOfType(InternalDecorator::class.java, this)
            val contentManager = decorator?.contentManager ?: return JBUI.scale(200)
            return contentManager.component.height / 2
        }

    val focusableComponent: JComponent get() = chatPanelInput
}
