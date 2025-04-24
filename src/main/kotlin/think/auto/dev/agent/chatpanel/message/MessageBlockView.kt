package think.auto.dev.agent.chatpanel.message

import java.awt.Component

interface MessageBlockView {
    fun getBlock(): MessageBlock

    fun getComponent(): Component?

    fun initialize() {}
}