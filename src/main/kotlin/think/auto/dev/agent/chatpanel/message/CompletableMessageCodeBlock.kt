package think.auto.dev.agent.chatpanel.message

import com.intellij.lang.Language
import think.auto.dev.utils.MessageFormatCode

/**
 * 代码块渲染
 */
class CompletableMessageCodeBlock(private val msg: CompletableMessage, language: Language = Language.ANY) :
    AbstractMessageBlock(msg) {
    override var type: MessageBlockType = MessageBlockType.CodeEditor

    var code: MessageFormatCode

    init {
        // Create the code content with language hint by constructing a code block string
        val content = buildString {
            append("```")
            if (language != Language.ANY) {
                append(language.displayName.lowercase())
            }
            append("\n")
            append(msg.text)
            append("\n```")
        }
        this.code = MessageFormatCode.parse(content)
    }

    override fun onContentChanged(content: String) {
        this.code = MessageFormatCode.parse(content)
    }
}