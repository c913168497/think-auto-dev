package think.auto.dev.agent.chatpanel.message



interface MessageBlock {
    val type: MessageBlockType

    /**
     * 文本内容获取
     */
    fun getTextContent(): String

    /**
     * 获取消息
     */
    fun getMessage(): CompletableMessage

    /**
     * 追加内容
     */
    fun addContent(addedContent: String)

    /**
     * 覆盖内容
     */
    fun replaceContent(content: String)

    /**
     * 添加文本监听器
     */
    fun addTextListener(textListener: MessageBlockTextListener)

    /**
     * 移除文本监听器
     */
    fun removeTextListener(textListener: MessageBlockTextListener)
}

