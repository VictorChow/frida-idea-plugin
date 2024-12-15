package me.victor.fridaideaplugin

class FridaTraceAction : BaseFridaAction() {

    override fun selectOnClass(clazz: String): String {
        return """
            frida-trace -U -j '${clazz}!*' -F
        """.trimIndent()
    }

    override fun selectOnMethod(info: MethodInfo): String {
        val methodName = if (info.isConstructor) "\$init" else info.methodName
        return """
            frida-trace -U -j '${info.classFQN}!${methodName}' -F
        """.trimIndent()
    }
}
