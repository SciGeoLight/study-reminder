package com.partner.studyreminder.data

/**
 * Tiny JSON codec for the plan file. No reflection, so release shrinking cannot
 * break saved plans, and unit tests run without Android.
 */
object MiniJson {
    fun parse(text: String): Any? = Reader(text).parse()

    fun write(value: Any?): String = buildString { writeValue(this, value) }

    private fun writeValue(out: StringBuilder, value: Any?) {
        when (value) {
            null -> out.append("null")
            is String -> out.append(quote(value))
            is Boolean -> out.append(if (value) "true" else "false")
            is Int -> out.append(value)
            is Long -> out.append(value)
            is Map<*, *> -> {
                out.append('{')
                var first = true
                for ((key, child) in value) {
                    if (!first) out.append(',')
                    first = false
                    out.append(quote(key.toString()))
                    out.append(':')
                    writeValue(out, child)
                }
                out.append('}')
            }
            is List<*> -> {
                out.append('[')
                value.forEachIndexed { index, child ->
                    if (index > 0) out.append(',')
                    writeValue(out, child)
                }
                out.append(']')
            }
            else -> error("不能写成 JSON 的类型：${value.javaClass.name}")
        }
    }

    private fun quote(text: String): String {
        val out = StringBuilder(text.length + 2)
        out.append('"')
        for (ch in text) {
            when (ch) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                else -> {
                    if (ch.code < 0x20) out.append("\\u%04x".format(ch.code))
                    else out.append(ch)
                }
            }
        }
        out.append('"')
        return out.toString()
    }

    private class Reader(private val text: String) {
        private var index = 0

        fun parse(): Any? {
            skip()
            if (index >= text.length) error("空的 JSON")
            val value = read()
            skip()
            if (index != text.length) error("JSON 末尾还有多余内容")
            return value
        }

        private fun read(): Any? {
            skip()
            if (index >= text.length) error("JSON 意外结束")
            return when (val ch = text[index]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                '-', in '0'..'9' -> readNumber()
                else -> error("无法解析的字符 $ch")
            }
        }

        private fun readObject(): Map<String, Any?> {
            expect('{')
            val map = linkedMapOf<String, Any?>()
            skip()
            if (peek('}')) {
                index++
                return map
            }
            while (true) {
                val key = readString()
                skip()
                expect(':')
                map[key] = read()
                skip()
                when {
                    peek(',') -> index++
                    peek('}') -> {
                        index++
                        break
                    }
                    else -> error("对象格式错误")
                }
            }
            return map
        }

        private fun readArray(): List<Any?> {
            expect('[')
            val list = mutableListOf<Any?>()
            skip()
            if (peek(']')) {
                index++
                return list
            }
            while (true) {
                list += read()
                skip()
                when {
                    peek(',') -> index++
                    peek(']') -> {
                        index++
                        break
                    }
                    else -> error("数组格式错误")
                }
            }
            return list
        }

        private fun readString(): String {
            expect('"')
            val out = StringBuilder()
            while (index < text.length) {
                val ch = text[index++]
                when (ch) {
                    '"' -> return out.toString()
                    '\\' -> {
                        if (index >= text.length) error("字符串转义中断")
                        when (val escaped = text[index++]) {
                            '"', '\\', '/' -> out.append(escaped)
                            'b' -> out.append('\b')
                            'f' -> out.append('\u000C')
                            'n' -> out.append('\n')
                            'r' -> out.append('\r')
                            't' -> out.append('\t')
                            'u' -> {
                                if (index + 4 > text.length) error("unicode 转义中断")
                                val hex = text.substring(index, index + 4)
                                out.append(hex.toInt(16).toChar())
                                index += 4
                            }
                            else -> error("未知转义")
                        }
                    }
                    else -> out.append(ch)
                }
            }
            error("字符串没有结束")
        }

        private fun readNumber(): Long {
            val start = index
            if (peek('-')) index++
            if (index >= text.length || text[index] !in '0'..'9') error("数字格式错误")
            while (index < text.length && text[index] in '0'..'9') index++
            return text.substring(start, index).toLong()
        }

        private fun literal(word: String, value: Any?): Any? {
            if (!text.startsWith(word, index)) error("期望 $word")
            index += word.length
            return value
        }

        private fun skip() {
            while (index < text.length && text[index].isWhitespace()) index++
        }

        private fun expect(ch: Char) {
            skip()
            if (index >= text.length || text[index] != ch) error("期望 $ch")
            index++
        }

        private fun peek(ch: Char): Boolean = index < text.length && text[index] == ch
    }
}
