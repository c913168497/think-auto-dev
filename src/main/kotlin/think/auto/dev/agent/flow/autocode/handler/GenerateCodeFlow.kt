package think.auto.dev.agent.flow.autocode.handler


import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import kotlinx.coroutines.runBlocking
import think.auto.dev.agent.chatpanel.sendToChatPanel
import think.auto.dev.agent.flow.autocode.AutoCodeBackgroundTask
import think.auto.dev.agent.flow.autocode.PseudocodeToTrueCodeContext
import think.auto.dev.agent.flow.core.BaseWorkflowStep
import think.auto.dev.llm.LlmFactory
import think.auto.dev.llm.role.ChatRole
import think.auto.dev.settings.pseudocode.database.PseudocodeStandardDataBaseComponent
import think.auto.dev.utils.VmFileReadUtils

class GenerateCodeFlow(val indicator: ProgressIndicator) : BaseWorkflowStep<PseudocodeToTrueCodeContext>(indicator) {

    companion object {
        const val FUNCTION_NAME = "生成代码"
    }

    override fun process(context: PseudocodeToTrueCodeContext): PseudocodeToTrueCodeContext {
        val project = context.project;
        val classAndFunInfo = context.classAndFunInfo
        val prompt = context.prompt
        val aiProvider = getAiProvider(GetNeedClassAndFunFlow.FUNCTION_NAME) ?: return context
        val llmProvider = LlmFactory().create(project, aiProvider)
        val templateRender = VmFileReadUtils("pseudocodeFlow")
        val msg_1 = templateRender.readTemplate("1_generateCode_system.vm")
        val msg_2 = "以下是我的请求和相关的类代码内容： \n 请求：\n $prompt \n 相关类代码：``` \n $classAndFunInfo\n```"
        val msg_3 = "好的，已经接收到您的请求和类信息，请问是否还有代码生成要求和规范需要补充？"
        llmProvider.appendLocalMessage(msg_1, ChatRole.System)
        llmProvider.appendLocalMessage(msg_2, ChatRole.User)
        llmProvider.appendLocalMessage(msg_3, ChatRole.Assistant)
        val bcPrompt =
            "以下是要求生成代码的方法体结构信息(包含了类，方法名、传参、返回参数信息), 生成的代码需要写在此方法内, 以下是补充信息: \n " + getFunctionInfo(
                context.editor, context.psiFile
            )

        llmProvider.appendLocalMessage(
            bcPrompt, ChatRole.User
        )

        llmProvider.appendLocalMessage(
            "好的,我已接收到您补充的当前需要填充方法所在的类、方法名、参数类信息，请问是否还有代码生成要求和规范需要补充? ",
            ChatRole.Assistant
        )

        val allPseudocodeStandardTitle = PseudocodeStandardDataBaseComponent.getAllPseudocodeStandardTitle()
        if (allPseudocodeStandardTitle.isNotEmpty()) {
            allPseudocodeStandardTitle.forEach {
                val msg = "以下是代码生成的" + it.title + "\n " + it.content
                llmProvider.appendLocalMessage(msg, ChatRole.User)
                llmProvider.appendLocalMessage(
                    "好的, 我已接收到您补充的:" + it.title + "请问是否还有代码生成要求和规范需要补充? 如果没有, 我将根据您的请求，开始填充方法所需代码。",
                    ChatRole.Assistant
                )
            }
        }

        val startPrompt = "没有了, 可以开始生成代码了，不需要其它额外的解释，只需要返回代码即可"
        runBlocking {
            // 确保在 EDT 上执行
            ApplicationManager.getApplication().invokeLater {
                sendToChatPanel(project, aiProvider, FUNCTION_NAME) { contentPanel, _ ->
                    val task = AutoCodeBackgroundTask(project, startPrompt, contentPanel, llmProvider)
                    ProgressManager.getInstance().runProcessWithProgressAsynchronously(
                        task, BackgroundableProcessIndicator(task)
                    )
                }
            }

        }

        runBlocking {
            indicator.fraction = 1.0
        }

        return context
    }

    fun getFunctionInfo(editor: Editor, psiFile: PsiFile): String {
        val offset = editor?.caretModel?.offset ?: 0
        if (psiFile == null || psiFile.virtualFile == null) {
            return ""
        }

        var result = buildString {
            psiFile.findElementAt(offset)?.let { element ->
                PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)?.let { method ->
                    // 当前方法信息
                    appendLine("方法所在类：${psiFile.name}")
                    appendMethodInfo(method)
                } ?: appendLine("当前光标不在方法内部")
            }
        }
        // 1. 分析我的请求中, 要求用到的类方法, 并返回类文件路径给到我
        // 2. psi分析, 获取关联的类方法, 属性值
        // 3. 按我的要求开始生成我所需要实现的逻辑
        return result
    }


    // 优化后的源码加载逻辑
    private fun StringBuilder.appendForceSourceCode(clazz: PsiClass) {
        when {
            clazz.isPhysical -> handlePhysicalClass(clazz)
            else -> appendLine(decompileClass(clazz))
        }
    }

    private fun StringBuilder.handlePhysicalClass(clazz: PsiClass) {
        clazz.containingFile?.let {
            appendLine("```\n" + it.text + "\n```")
        } ?: appendLine("物理文件不存在")
    }

    private fun StringBuilder.appendCustomClassInfo(method: PsiMethod) {
        val customClasses = mutableSetOf<String>()

        ReadAction.compute<Array<PsiParameter>, Throwable> {
            method.parameterList.parameters
        }.forEach { param ->
            collectCustomTypes(ReadAction.compute<PsiType, Throwable> {
                param.type
            }, customClasses)
        }

        ReadAction.compute<PsiType, Throwable> {
            method.returnType
        }?.let {
            collectCustomTypes(it, customClasses)
        }

        appendLine("方法关联的类列表：")
        customClasses.forEach { fqn ->
            val clazz = findClassByFQN(method.project, fqn)
            clazz?.let {
                appendLine("=".repeat(50))
                appendLine("类名: ${it.name}")
                appendLine("包名: ${it.qualifiedName?.substringBeforeLast('.')}")
                appendLine("位置: ${it.containingFile?.virtualFile?.path}")
                // 获取类结构
                appendClassStructure(clazz)
                // 获取源码内容（需要物理文件存在）
//                appendSourceCode(clazz)
            } ?: appendLine("$fqn 未找到")
        }

    }


    // 增强的反编译实现（使用ClsDecompilerImpl）
    private fun decompileClass(clazz: PsiClass): String {
        return when {
            true -> {
                try {
                    clazz.text ?: "反编译失败（无输出）"
                } catch (e: Exception) {
                    "内置反编译失败: ${e.message}"
                }
            }

            clazz.isPhysical -> clazz.containingFile?.text ?: "无法读取物理文件"
            else -> "无法反编译非编译类"
        }
    }

    fun findClassByFQN(project: Project, fqn: String): PsiClass? {
        return ReadAction.compute<PsiClass, Throwable> {
            JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))
        }
    }

    private fun StringBuilder.appendClassStructure(clazz: PsiClass) {
        appendForceSourceCode(clazz) // 新增强制加载方法
    }

    private fun StringBuilder.appendMethodInfo(method: PsiMethod) {
        // 方法名
        appendLine("方法名：${method.name}")
        // 方法签名
        appendLine("方法体结构：${method.getSignature()}")
        // 方法注释
        appendLine("方法注释：${method.docComment?.text?.trim()}")

        // 收集自定义类信息
        appendCustomClassInfo(method)
    }

    private fun PsiMethod.getSignature(): String {
        return buildString {
            append(returnType.let { it?.presentableText ?: "void" })
            append(" ")
            append(name)
            append("(")
            parameterList.parameters.joinTo(this) { param ->
                param.type.presentableText + " " + param.name
            }
            append(")")

        }
    }

    private fun collectCustomTypes(type: PsiType, collector: MutableSet<String>) {
        when (type) {
            is PsiClassType -> {
                val psiClass = ReadAction.compute<PsiClass, Throwable> {
                    type.resolve()
                } ?: return
                val qualifiedName = psiClass.qualifiedName
                // 关键修改点：增加泛型参数深度解析
                if (qualifiedName != null && !qualifiedName.startsWith("java.") && !qualifiedName.startsWith("javax.")) {
                    collector.add(qualifiedName)
                    // 递归处理泛型类型参数
                    ReadAction.compute<Array<PsiType>, Throwable> {
                        type.parameters
                    }.forEach { typeArgument ->
                        collectCustomTypes(typeArgument, collector)
                    }

                }
            }

            is PsiArrayType -> collectCustomTypes(type.componentType, collector)
        }
    }


}
