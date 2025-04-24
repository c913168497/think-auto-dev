package think.auto.dev.utils

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile


/**
 * 获取 PsiClass 的包路径
 *
 * @param psiClass 目标类
 * @return 包路径（例如："com.example"），如果类没有包路径则返回空字符串
 */
fun getPackagePath(psiClass: PsiClass): String {
    val containingFile = psiClass.containingFile
    if (containingFile is PsiJavaFile) {
        return containingFile.packageName
    }
    return "" // 如果文件不是 Java 文件，则返回空字符串
}

fun getClassInfoAndFunInfo(psiClass: PsiClass): String {
    val classBuilder = StringBuilder()
    classBuilder.append("\n").append(getPackagePath(psiClass)).append(".").append(psiClass.name).append("\n")
    classBuilder.append("```").append("\n")
    // 类注释
    val classDocComment = psiClass.docComment
    if (classDocComment != null) {
        classBuilder.append(classDocComment.text)
    }

    // 类注解
    val classAnnotations = psiClass.annotations
    for (annotation in classAnnotations) {
        classBuilder.append(annotation.text).append("\n")
    }

    // 类声明 只有数据承载的类, 才会去获得属性值
    classBuilder.append("public class ${psiClass.name} {\n")
    val className = psiClass.name
    if (className!!.endsWith("Dto") || className.endsWith("Vo") || className.endsWith("Bo") || className.endsWith("Excel") || className.endsWith(
            "Entity"
        ) || className.endsWith("Enum") || className.endsWith("Msg")
    ) {
        // 类的属性
        val fields = psiClass.fields
        for (field in fields) {
            // 属性注释
            val fieldDocComment = field.docComment
            if (fieldDocComment != null) {
                classBuilder.append("    ").append(fieldDocComment.text.replace("\n", "\n    ")).append("\n")
            }

            // 属性注解
            val fieldAnnotations = field.annotations
            for (annotation in fieldAnnotations) {
                classBuilder.append("    ").append(annotation.text).append("\n")
            }

            // 属性声明
            classBuilder.append("    ${field.type.presentableText} ${field.name};\n")
        }
    }


    // 构造方法
    val constructors = psiClass.constructors
    for (constructor in constructors) {
        // 构造方法注释
        val constructorDocComment = constructor.docComment
        if (constructorDocComment != null) {
            classBuilder.append(" ").append(constructorDocComment.text.replace("\n", "\n  ")).append("\n")
        }

        // 构造方法注解
        val constructorAnnotations = constructor.annotations
        for (annotation in constructorAnnotations) {
            classBuilder.append(" ").append(annotation.text).append("\n")
        }

        // 构造方法声明
        classBuilder.append("    public ${constructor.name}${constructor.parameterList.text};")
    }

    // 类方法
    val methods = psiClass.methods
    for (method in methods) {
        // 方法注释
        val methodDocComment = method.docComment
        if (methodDocComment != null) {
            classBuilder.append(" ").append(methodDocComment.text.replace("\n", "\n ")).append("\n")
        }

        // 方法注解
        val methodAnnotations = method.annotations
        for (annotation in methodAnnotations) {
            classBuilder.append(" ").append(annotation.text).append("\n")
        }

        // 方法声明
        classBuilder.append("    public ${method.returnType?.presentableText ?: "void"} ${method.name}${method.parameterList.text};\n   ")
    }

    classBuilder.append("}\n```")
    return classBuilder.toString()
}
