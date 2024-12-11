package me.victor.fridaideaplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

abstract class BaseFridaAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val offset = editor.caretModel.offset
        val element = file.findElementAt(offset) ?: return
        // 先判断是否是方法调用
        val methodCall = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression::class.java)
        if (methodCall != null) {
            val targetMethod = methodCall.resolveMethod()
            if (targetMethod != null) {
                val methodInfo = extractMethodInfo(targetMethod) ?: return
                val fridaScript = generateContent(methodInfo)
                copyToClipboard(fridaScript)
            } else {
                Messages.showMessageDialog(project, "无法解析方法调用", "错误", Messages.getErrorIcon())
            }
        } else {
            // 如果光标在方法定义上，生成该方法的 frida 命令
            val method = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)
            if (method != null) {
                val methodInfo = extractMethodInfo(method) ?: return
                val fridaScript = generateContent(methodInfo)
                copyToClipboard(fridaScript)
            } else {
                Messages.showMessageDialog(project, "无法解析方法定义", "错误", Messages.getErrorIcon())
            }
        }
    }

    protected abstract fun generateContent(info: MethodInfo): String

    private fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, selection)
    }

    private fun showErrorMsg(project: Project, msg: String) {
        Messages.showMessageDialog(project, msg, "错误", Messages.getErrorIcon())
    }

    private fun extractMethodInfo(method: PsiMethod): MethodInfo? {
        val classFQN = method.containingClass?.qualifiedName ?: return null
        val simpleClassName = method.containingClass?.name ?: return null
        val methodName = method.name
        val returnType = if (method.isConstructor) "void" else (method.returnType?.canonicalText ?: "void")
        val parameters = method.parameterList.parameters
        val parameterTypes = parameters.map { it.type.canonicalText }

        // 检查是否存在重载的方法
        val containingClass = method.containingClass ?: return null
        val overloadedMethods = containingClass.findMethodsByName(methodName, false)
        val isOverloaded = overloadedMethods.size > 1

        val isConstructor = method.isConstructor // 检测是否为构造函数

        return MethodInfo(
            classFQN = classFQN,
            simpleClassName = simpleClassName,
            methodName = methodName,
            returnType = returnType,
            parameterTypes = parameterTypes,
            isOverloaded = isOverloaded,
            isConstructor = isConstructor
        )
    }

    data class MethodInfo(
        val classFQN: String,
        val simpleClassName: String,
        val methodName: String,
        val returnType: String,
        val parameterTypes: List<String>,
        val isOverloaded: Boolean,
        val isConstructor: Boolean
    )
}
