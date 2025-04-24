package think.auto.dev.agent.chatpanel.message
/**
 * 抽象消息渲染块 类
 */
abstract class AbstractMessageBlock(open val completableMessage: CompletableMessage) : MessageBlock {
    private val contentBuilder: StringBuilder = StringBuilder()
    private val textListeners: MutableList<MessageBlockTextListener> = mutableListOf()

    override fun addContent(addedContent: String) {
        contentBuilder.append(addedContent)
        onContentAdded(addedContent)
        val content = contentBuilder.toString()
        onContentChanged(content)
        fireTextChanged(content)
    }

    override fun replaceContent(content: String) {
        contentBuilder.clear()
        contentBuilder.append(content)
        onContentChanged(content)
        fireTextChanged(content)
    }

    override fun getTextContent(): String {
        return contentBuilder.toString()
    }

    override fun getMessage(): CompletableMessage {
        return completableMessage
    }

    protected fun onContentAdded(addedContent: String) {}
    protected open fun onContentChanged(content: String) {}
    private fun fireTextChanged(text: String) {
        for (textListener in textListeners) {
            textListener.onTextChanged(text)
        }
    }

    override fun addTextListener(textListener: MessageBlockTextListener) {
        textListeners.add(textListener)
    }

    override fun removeTextListener(textListener: MessageBlockTextListener) {
        textListeners.remove(textListener)
    }
}