package me.victor.fridaideaplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiMethod
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
        val method = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java) ?: run {
            Messages.showMessageDialog(project, "请在一个方法上右键点击。", "提示", Messages.getInformationIcon())
            return
        }

        val methodInfo = extractMethodInfo(method) ?: run {
            Messages.showMessageDialog(project, "无法提取方法信息。", "错误", Messages.getErrorIcon())
            return
        }

        val fridaTraceCommand = generateFridaTraceCommand(methodInfo)
        copyToClipboard(fridaTraceCommand)
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
