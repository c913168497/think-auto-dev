package think.auto.dev.agent.chatpanel.message.gui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.Box

/**
 * editor padding 组件
 */
class ChatPanelEditorPadding(private val editor: Editor, pad: Int) :
    Box.Filler(Dimension(pad, 0), Dimension(pad, 0), Dimension(pad, 32767)) {
    init {
        setOpaque(true)
        editor.caretModel.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                this@ChatPanelEditorPadding.repaint()
            }
        })
    }

    override fun getBackground(): Color {
        return editor.contentComponent.getBackground()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
    }
}