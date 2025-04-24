package think.auto.dev.settings.pseudocode.database

/**
 * 伪代码生成规范要求
 */
data class PseudocodeStandard(
    val id: Int? = null, // 数据库自增ID，新增时可以为空
    val title: String,
    var content: String
) {
    override fun toString(): String {
        return title
    }
}
