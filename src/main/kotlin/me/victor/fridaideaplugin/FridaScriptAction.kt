package me.victor.fridaideaplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class FridaScriptAction : AnAction() {

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

        val fridaScript = generateFridaScript(methodInfo)
        copyToClipboard(fridaScript)
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

    private fun generateFridaScript(info: MethodInfo): String {
        val argCount = info.parameterTypes.size
        val argList = (1..argCount).map { "arg$it" }  // 生成 arg1, arg2, ..., argN
        val logArgs = argList.joinToString(", ") { "$it=\${$it}" } // 生成 "arg1=${arg1}, arg2=${arg2}"

        // 如果方法被重载，使用 .overload 并指定参数类型
        val overloadString = if (info.isOverloaded) {
            val fridaParamTypes = info.parameterTypes.joinToString(", ") { "\"$it\"" }
            ".overload($fridaParamTypes)"
        } else {
            ""
        }

        // 如果是构造函数，使用 $init 作为方法名，且不打印返回值
        val methodNameInScript = if (info.isConstructor) "\$init" else info.methodName

        // 判断方法是否为 void 类型，如果是，则不打印返回值
        return if (info.returnType == "void" || info.isConstructor) {
            """
            Java.use("${info.classFQN}").${methodNameInScript}${overloadString}.implementation = function(${
            argList.joinToString(", ") { "$it: any" }}) {
                DMLog.i("${info.simpleClassName}", `called $methodNameInScript with args: $logArgs`);
                this.${methodNameInScript}(${argList.joinToString(", ")});
            };
    """.trimIndent()
        } else {
            """
            Java.use("${info.classFQN}").${methodNameInScript}${overloadString}.implementation = function(${
                argList.joinToString(", ") { "$it: any" }
            }) {
                DMLog.i("${info.simpleClassName}", `called ${info.methodName} with args: $logArgs`);
                const ret = this.${methodNameInScript}(${argList.joinToString(", ")});
                DMLog.i("${info.simpleClassName}", "called ${info.methodName} return: " + ret);
                return ret;
            };
        """.trimIndent()
        }
    }


    private fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, selection)
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
