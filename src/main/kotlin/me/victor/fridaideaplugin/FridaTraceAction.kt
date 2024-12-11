package me.victor.fridaideaplugin

class FridaTraceAction : BaseFridaAction() {

    override fun generateContent(info: MethodInfo): String {
        val methodName = if (info.isConstructor) "\$init" else info.methodName
        return """
            frida-trace -U -j '${info.classFQN}!${methodName}' -F
        """.trimIndent()
    }
}
