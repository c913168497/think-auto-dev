package think.auto.dev.agent.chatpanel.message
/**
 * 文本块渲染
 */
class CompletableMessageTextBlock(val msg: CompletableMessage) : AbstractMessageBlock(msg) {
    override val type: MessageBlockType = MessageBlockType.PlainText
}
