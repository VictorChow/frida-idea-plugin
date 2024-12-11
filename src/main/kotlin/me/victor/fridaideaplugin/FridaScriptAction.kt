package me.victor.fridaideaplugin

class FridaScriptAction : BaseFridaAction() {

    override fun generateContent(info: MethodInfo): String {
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
                argList.joinToString(", ") { "$it: any" }
            }) {
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
}
