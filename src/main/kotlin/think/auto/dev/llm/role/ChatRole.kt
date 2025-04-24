package think.auto.dev.llm.role

enum class ChatRole {
    System,
    Assistant,
    User;

    fun roleName(): String {
        return this.name.lowercase()
    }

    fun getRoleName(role: ChatRole): String {
        return when (role) {
            ChatRole.System -> "System"
            ChatRole.Assistant -> "Assistant"
            ChatRole.User -> "User"
        }
    }

}
