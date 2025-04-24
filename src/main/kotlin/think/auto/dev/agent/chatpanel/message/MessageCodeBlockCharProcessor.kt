
package think.auto.dev.agent.chatpanel.message

/**
 * 	标记代码块的边界类型：START（开始）、END（结束）
 */
enum class BorderType {
    START,
    END
}

/**
 * 封装当前输入的字符、位置和完整消息内容
 */
class Parameters(val char: Char, val charIndex: Int, val fullMessage: String)

/**
 * 	表示上下文切换事件（切换到的块类型 + 边界类型）
 */
class ContextChange(@JvmField val contextType: MessageBlockType, @JvmField val borderType: BorderType)

/**
 *  消息代码块字符处理器，主要用于在聊天面板或文本编辑器中检测和处理 Markdown 风格的代码块（以 ``` 包裹的代码片段）。以下是详细分析：
 *  核心处理器，实现代码块检测逻辑
 */
class MessageCodeBlockCharProcessor {
    private val triggerChar: Char = '`'
    private val borderBlock: String = "```"

    fun suggestTypeChange(
        parameters: Parameters,
        currentContextType: MessageBlockType,
        blockStart: Int
    ): ContextChange? {
        if (parameters.char != triggerChar && parameters.char != '\n') return null

        return when (currentContextType) {
            MessageBlockType.PlainText -> {
                if (isCodeBlockStart(parameters)) {
                    ContextChange(MessageBlockType.CodeEditor, BorderType.START)
                } else {
                    null
                }
            }

            MessageBlockType.CodeEditor -> {
                if (isCodeBlockEnd(parameters, blockStart)) {
                    ContextChange(MessageBlockType.CodeEditor, BorderType.END)
                } else {
                    null
                }
            }
        }
    }

    private fun isCodeBlockEnd(parameters: Parameters, blockStart: Int): Boolean {
        if (parameters.charIndex - blockStart < 5) {
            return false
        }
        val fullMessage = parameters.fullMessage
        val charIndex = parameters.charIndex
        return when {
            parameters.char == triggerChar && charIndex == fullMessage.length - 1 -> {
                val subSequence = fullMessage.subSequence(charIndex - 3, charIndex + 1)
                subSequence == "\n$borderBlock"
            }

            parameters.char == '\n' && (charIndex - 3) - 1 >= 0 -> {
                val subSequence = fullMessage.subSequence(charIndex - 4, charIndex)
                subSequence == "\n$borderBlock"
            }

            else -> false
        }
    }

    private fun isCodeBlockStart(parameters: Parameters): Boolean {
        if (parameters.char == triggerChar && parameters.charIndex + 3 < parameters.fullMessage.length) {
            val isLineStart = parameters.charIndex == 0 || parameters.fullMessage[parameters.charIndex - 1] == '\n'
            if (isLineStart) {
                val subSequence = parameters.fullMessage.subSequence(parameters.charIndex, parameters.charIndex + 3)
                return subSequence.all { it == triggerChar }
            }
        }
        return false
    }
}