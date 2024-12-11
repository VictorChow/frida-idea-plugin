package me.victor.fridaideaplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class FridaTraceAction : AnAction() {

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
                val fridaTraceCommand = generateFridaTraceCommand(methodInfo)
                copyToClipboard(fridaTraceCommand)
            } else {
                Messages.showMessageDialog(project, "无法解析方法调用", "错误", Messages.getErrorIcon())
            }
        } else {
            // 如果光标在方法定义上，生成该方法的 frida-trace 命令
            val method = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)
            if (method != null) {
                val methodInfo = extractMethodInfo(method) ?: return
                val fridaTraceCommand = generateFridaTraceCommand(methodInfo)
                copyToClipboard(fridaTraceCommand)
            } else {
                Messages.showMessageDialog(project, "无法解析方法定义", "错误", Messages.getErrorIcon())
            }
        }
    }

    private fun extractMethodInfo(method: PsiMethod): MethodInfo? {
        val classFQN = method.containingClass?.qualifiedName ?: return null
        val methodName = method.name

        return MethodInfo(
            classFQN = classFQN,
            methodName = methodName
        )
    }

    private fun generateFridaTraceCommand(info: MethodInfo): String {
        return """
            frida-trace -U -j '${info.classFQN}!${info.methodName}' -F
        """.trimIndent()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, selection)
    }

    data class MethodInfo(
        val classFQN: String,
        val methodName: String
    )
}
