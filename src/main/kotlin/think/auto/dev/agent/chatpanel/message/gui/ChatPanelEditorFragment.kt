package think.auto.dev.agent.chatpanel.message.gui

import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import think.auto.dev.agent.chatpanel.message.CompletableMessage
import java.awt.Dimension
import java.awt.Insets
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JComponent

/**
 * editor fragment 组件
 */
class ChatPanelEditorFragment(private val project: Project, private val editor: EditorEx, message: CompletableMessage) {

    private val editorLineThreshold = 6

    private val expandCollapseTextLabel: ChatPanelAIFoldLabel

    private val content: BorderLayoutPanel

    private var collapsed = false

    init {
        expandCollapseTextLabel = ChatPanelAIFoldLabel(message.getRole())
        content = object : BorderLayoutPanel() {
            override fun getPreferredSize(): Dimension {
                val preferredSize = super.getPreferredSize()
                val lineCount = editor.document.lineCount
                val shouldCollapse = lineCount > editorLineThreshold
                if (shouldCollapse && getCollapsed()) {
                    val lineHeight = editor.lineHeight
                    val insets = editor.scrollPane.getInsets()
                    val height = lineHeight * editorLineThreshold + insets.height
                    var editorMaxHeight = height + expandCollapseTextLabel.preferredSize.height + getInsets().height
                    val it = editor.headerComponent
                    if (it != null) {
                        editorMaxHeight += it.getPreferredSize().height
                    }
                    return Dimension(preferredSize.width, editorMaxHeight)
                }

                return preferredSize
            }
        }

        content.setBorder(
            JBUI.Borders.compound(
                JBUI.Borders.empty(10, 0),
                JBUI.Borders.customLine(JBColor(13949150, 4740710))
            )
        )
        content.setOpaque(false)
        content.addToLeft((ChatPanelEditorPadding(editor, 5)))
        content.addToCenter((editor.component))
        content.addToRight((ChatPanelEditorPadding(editor, 5)))
        content.addToBottom((expandCollapseTextLabel))
        expandCollapseTextLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                setCollapsed(!getCollapsed())
            }
        } as MouseListener)
    }

    fun getCollapsed(): Boolean {
        return collapsed
    }

    fun setCollapsed(value: Boolean) {
        collapsed = value
        updateExpandCollapseLabel()
    }

    fun updateExpandCollapseLabel() {
        val linesCount = editor.document.lineCount
        expandCollapseTextLabel.isVisible = linesCount > editorLineThreshold
        if (collapsed) {
            expandCollapseTextLabel.text = "More lines"
            expandCollapseTextLabel.icon = AllIcons.General.ChevronDown
            return
        }

        expandCollapseTextLabel.text = ""
        expandCollapseTextLabel.icon = AllIcons.General.ChevronUp
    }

    fun getContent(): JComponent {
        return content
    }

    val Insets.width: Int get() = left + right
    val Insets.height: Int get() = top + bottom
}
