package think.auto.dev.agent.chatpanel.message.gui

import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorMarkupModel
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.editor.ex.MarkupModelEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.messages.Topic
import com.intellij.util.ui.JBUI
import think.auto.dev.agent.chatpanel.message.CompletableMessage
import think.auto.dev.agent.chatpanel.message.CompletableMessageCodeBlock
import think.auto.dev.agent.chatpanel.message.MessageBlockView
import think.auto.dev.agent.chatpanel.message.SNIPPET_NAME
import think.auto.dev.llm.role.ChatRole
import think.auto.dev.utils.MessageFormatCode
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent

/**
 * 代码块 视图
 */
class ChatPanelCodeBlockView(
    private val block: CompletableMessageCodeBlock,
    private val project: Project,
    private val disposable: Disposable,
) : MessageBlockView {
    private var editorInfo: CodePartEditorInfo? = null

    init {
        block.addTextListener {
            if (editorInfo == null) return@addTextListener
            updateOrCreateCodeView()
        }
    }

    override fun getBlock(): CompletableMessageCodeBlock {
        return block
    }

    override fun getComponent(): JComponent {
        return editorInfo?.component ?: return updateOrCreateCodeView()!!.component
    }

    val codeContent: String
        get() {
            return editorInfo?.code?.get() ?: ""
        }

    override fun initialize() {
        if (editorInfo == null) {
            updateOrCreateCodeView()
        }
    }

    /**
     * 更新或创建代码块视图
     */
    private fun updateOrCreateCodeView(): CodePartEditorInfo? {
        val code: MessageFormatCode = getBlock().code
        if (editorInfo == null) {
            val graphProperty = PropertyGraph(null, false).property(code.text)
            val editorInfo: CodePartEditorInfo = createCodeViewer(
                project, graphProperty, disposable, code.language, getBlock().getMessage()
            )
            this.editorInfo = editorInfo
        } else {
            val codePartEditorInfo = editorInfo
            if (codePartEditorInfo!!.language == code.language) {
                editorInfo!!.language = code.language
            }

            editorInfo!!.code.set(code.text)
        }

        return editorInfo
    }

    /**
     * 代码块视图对象
     */
    companion object {

        /**
         * 创建代码视图
         */
        fun createCodeViewer(
            project: Project,
            graphProperty: GraphProperty<String>,
            disposable: Disposable,
            language: Language,
            message: CompletableMessage,
        ): CodePartEditorInfo {
            // 内存中的虚拟文件，不会写入磁盘，轻量级实现，适合临时显示代码片段 可以关联到编辑器进行语法高亮和基本代码编辑 当不再需要时会被自动清理
            // SnippetName 表示临时文件名， language 决定了文件的语法高亮、代码分析等行为，
            // graphProperty.get() 方法获取当前存储的字符串值，这将是文件的实际内容(代码文本)
            val file = LightVirtualFile(SNIPPET_NAME, language, graphProperty.get())
            // 如果未知则表示 ，默认文本
            if (file.fileType == UnknownFileType.INSTANCE) {
                file.fileType = PlainTextFileType.INSTANCE
            }
            // 寻找document对象
            val document: Document = file.findDocument() ?: throw IllegalStateException("Document not found")
            // 创建编辑器 （自定义代码显示编辑器）
            val editor: EditorEx = createCodeViewerEditor(project, file, document, disposable)
            // tool bar 条组
            val toolbarActionGroup = ActionUtil.getActionGroup("ThinkAutoDev.ToolWindow.Snippet.Toolbar")!!
            // 自定义实现工具栏
            toolbarActionGroup.let {
                //  // 1. 创建自定义工具栏
                val toolbar: ActionToolbarImpl =
                    object : ActionToolbarImpl(ActionPlaces.MAIN_TOOLBAR, toolbarActionGroup, true) {
                        override fun updateUI() {
                            super.updateUI()
                            editor.component.setBorder(JBUI.Borders.empty())
                        }
                    }

                // 2. 配置工具栏样式
                toolbar.setBackground(editor.backgroundColor)
                toolbar.setOpaque(true)
                toolbar.targetComponent = editor.contentComponent

                // 3. 将工具栏添加到编辑器头部
                editor.headerComponent = toolbar

                // 4. 订阅主题颜色变化
                val connect = project.messageBus.connect(disposable)
                val topic: Topic<EditorColorsListener> = EditorColorsManager.TOPIC
                connect.subscribe(topic, EditorColorsListener {
                    toolbar.setBackground(editor.backgroundColor)
                })
            }

            editor.scrollPane.setBorder(JBUI.Borders.empty())
            editor.component.setBorder(JBUI.Borders.empty())
            val forceFoldEditorByDefault = message.getRole() === ChatRole.User
            val editorFragment = ChatPanelEditorFragment(project, editor, message)
            editorFragment.setCollapsed(forceFoldEditorByDefault)
            editorFragment.updateExpandCollapseLabel()
            return CodePartEditorInfo(graphProperty, editorFragment.getContent(), editor, file)
        }

        /**
         * 创建代码块视图编辑器
         */
        private fun createCodeViewerEditor(
            project: Project,
            file: LightVirtualFile,
            document: Document,
            disposable: Disposable,
        ): EditorEx {
            // 在 非阻塞线程安全 的方式下，创建一个 只读预览编辑器（EditorEx），用于显示代码内容。
            // ReadAction.compute<EditorEx, Throwable> 这是 IntelliJ 平台的 线程安全工具，确保代码块在 读取操作（非写操作）中执行，避免并发问题。
            // EditorFactory.getInstance().createViewer() 通过编辑器工厂创建一个 只读查看器（Viewer），参数：document：要显示的文档内容（关联到虚拟文件）
            // EditorKind.PREVIEW：指定编辑器类型为“预览模式”（只读、无完整IDE功能）
            val editor: EditorEx = ReadAction.compute<EditorEx, Throwable> {
                EditorFactory.getInstance().createViewer(document, project, EditorKind.PREVIEW) as EditorEx
            }

            // 注册一个 资源释放回调，当父级 disposable 被销毁时，自动释放编辑器资源，防止内存泄漏。
            disposable.whenDisposed(disposable) {
                EditorFactory.getInstance().releaseEditor(editor)
            }

            editor.setFile(file)
            // 启用编辑器的光标（插入符号）显示，允许用户聚焦和交互
            editor.setCaretEnabled(true)
            // 为编辑器创建并设置语法高亮器，根据文件类型自动匹配对应的代码着色规则。
            val highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, file)
            editor.highlighter = highlighter
            // 禁用编辑器**右侧错误条纹（Error Stripe）**的显示，通常用于简化预览界面的视觉效果。
            // editor.markupModel 获取编辑器的标记模型，用于管理代码注解（如错误提示、警告、断点等）。
            //  (markupModel as EditorMarkupModel).isErrorStripeVisible  隐藏右侧显示错误、警告等标记的缩略。（临时代码不需要编译）
            val markupModel: MarkupModelEx = editor.markupModel
            (markupModel as EditorMarkupModel).isErrorStripeVisible = false

            val settings = editor.settings.also {
                // 交互相关
                it.isDndEnabled = false                // 禁用拖拽功能
                it.isShowIntentionBulb = false         // 隐藏意图提示灯泡

                // 行号与边栏
                it.isLineNumbersShown = false          // 隐藏行号
                it.isLineMarkerAreaShown = false       // 隐藏行标记区域（断点等）

                // 滚动与布局
                it.additionalLinesCount = 0            // 底部不保留额外空行
                it.isRefrainFromScrolling = true       // 禁止自动滚动到光标
                it.isAdditionalPageAtBottom = false    // 底部不添加额外分页

                // 代码折叠与边距
                it.isFoldingOutlineShown = false       // 隐藏代码折叠轮廓
                it.isRightMarginShown = false          // 隐藏右侧边距

                // 文本渲染
                it.isUseSoftWraps = true               // 启用软换行（不横向滚动）

                // 光标显示
                it.isCaretRowShown = false             // 隐藏光标所在行高亮
            }

            editor.addFocusListener(object : FocusChangeListener {
                override fun focusGained(focusEditor: Editor) {
                    settings.isCaretRowShown = true
                }

                override fun focusLost(focusEditor: Editor) {
                    settings.isCaretRowShown = false
                    editor.markupModel.removeAllHighlighters()
                }
            })

            return editor
        }


    }
}

@RequiresReadLock
fun VirtualFile.findDocument(): Document? {
    return ReadAction.compute<Document, Throwable> {
        FileDocumentManager.getInstance().getDocument(this)
    }
}

fun Disposable.whenDisposed(listener: () -> Unit) {
    Disposer.register(this) { listener() }
}

fun Disposable.whenDisposed(
    parentDisposable: Disposable,
    listener: () -> Unit
) {
    val isDisposed = AtomicBoolean(false)

    val disposable = Disposable {
        if (isDisposed.compareAndSet(false, true)) {
            listener()
        }
    }

    Disposer.register(this, disposable)

    Disposer.register(parentDisposable, Disposable {
        if (isDisposed.compareAndSet(false, true)) {
            Disposer.dispose(disposable)
        }
    })
}