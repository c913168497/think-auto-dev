package think.auto.dev.agent.chatpanel

import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.UIUtil
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup
import javax.accessibility.AccessibleContext
import javax.swing.JEditorPane
import javax.swing.SwingUtilities

class ChatMessageDisplayComponent(question: String) : JEditorPane() {
    init {
        this.contentType = "text/plain;charset=UTF-8"
        this.putClientProperty(HONOR_DISPLAY_PROPERTIES, true)
        this.font = UIUtil.getMenuFont()
        this.isEditable = false
        this.border = JBEmptyBorder(8)
        this.text = question
        this.isOpaque = false
        this.putClientProperty(
            AccessibleContext.ACCESSIBLE_NAME_PROPERTY, stripHtmlAndUnescapeXmlEntities(question)
        )

        if (this.caret != null) {
            this.caretPosition = 0
        }
    }

    fun updateMessage(content: String) {
        // 记录当前的选中状态
        val selectionStart = this.selectionStart
        val selectionEnd = this.selectionEnd

        // 更新内容并解析 Markdown
        this.text = content

        // 恢复选中状态
        if (selectionStart != selectionEnd) {
            SwingUtilities.invokeLater {
                this.selectionStart = selectionStart
                this.selectionEnd = selectionEnd
            }
        }
    }


    private fun stripHtmlAndUnescapeXmlEntities(input: String): String {
        // 使用 Jsoup 去除 HTML 标签
        val text = Jsoup.parse(input).text()
        // 使用 Apache Commons Text 解码 XML 实体
        return StringEscapeUtils.unescapeXml(text)
    }
}
