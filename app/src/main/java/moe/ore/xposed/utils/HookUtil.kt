package moe.ore.xposed.utils

object HookUtil {
    fun getFormattedStackTrace(): String {
        val stackTrace = Exception().stackTrace
        val sb = StringBuilder("Stack Trace:\n")

        for (element in stackTrace) { // i in 3 until stackTrace.size
            // val element = stackTrace[i]

            val className = element.className
            val methodName = element.methodName
            val lineNumber = element.lineNumber

            sb.append(className)
                .append(".")
                .append(methodName)
                .append("(line: ")
                .append(lineNumber)
                .append(")\n")
        }

        return sb.toString()
    }
}
