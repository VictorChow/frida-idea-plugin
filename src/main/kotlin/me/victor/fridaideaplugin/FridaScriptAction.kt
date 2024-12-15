package me.victor.fridaideaplugin

class FridaScriptAction : BaseFridaAction() {

    override fun selectOnClass(clazz: String) = null

    override fun selectOnMethod(info: MethodInfo): String {
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
            val logStatement = if (argCount == 0) "" else
                """
            DMLog.i("${info.simpleClassName}", `$methodNameInScript args: $logArgs`);
    """.trimEnd()
            """
        Java.use("${info.classFQN}").${methodNameInScript}${overloadString}.implementation = function(${
                argList.joinToString(", ") { "$it: any" }
            }) {$logStatement
            this.${methodNameInScript}(${argList.joinToString(", ")});
        };
    """.trimIndent()
        } else {
            val logStatement = if (argCount == 0) "" else
                """
            DMLog.i("${info.simpleClassName}", `${info.methodName} args: $logArgs`);
    """.trimEnd()
            """
        Java.use("${info.classFQN}").${methodNameInScript}${overloadString}.implementation = function(${
                argList.joinToString(", ") { "$it: any" }
            }) {$logStatement
            const ret = this.${methodNameInScript}(${argList.joinToString(", ")});
            DMLog.i("${info.simpleClassName}", "${info.methodName} return: " + ret);
            return ret;
        };
        """.trimIndent()
        }
    }
}
