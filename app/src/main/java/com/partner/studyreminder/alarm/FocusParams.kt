package com.partner.studyreminder.alarm

/**
 * HyperOS focus-notification / Super Island payload.
 *
 * Local notifications carry this JSON on the extra `miui.focus.param`. The root
 * object is `param_v2` (protocol 1). Pictures are a separate `miui.focus.pics`
 * bundle whose keys are referenced by name from the JSON. Field names follow
 * the public HyperOS 2/3 template used by Xiaomi's focus notification and the
 * Aliyun EMAS Super Island guide: baseInfo, hintInfo, ticker, aodTitle, and
 * param_island (bigIslandArea.progressTextInfo + smallIslandArea).
 *
 * chatInfo is the messaging template (avatar + message). A study block is not
 * a conversation, so it is not sent.
 */
object FocusParams {
    const val PIC_ICON = "miui.focus.pic_icon"

    fun isXiaomi(manufacturer: String, brand: String): Boolean {
        fun match(raw: String): Boolean {
            val name = raw.trim().lowercase()
            return name == "xiaomi" || name == "redmi" || name == "poco"
        }
        return match(manufacturer) || match(brand)
    }

    fun json(
        title: String,
        content: String,
        ticker: String,
        aodTitle: String,
        hintTitle: String,
        remainingMinutes: Int,
        progressPercent: Int,
        timeoutMinutes: Int,
        floatOnPost: Boolean,
    ): String {
        val progress = progressPercent.coerceIn(0, 100)
        val remain = remainingMinutes.coerceAtLeast(0)
        val timeout = if (floatOnPost && timeoutMinutes <= 0) -1 else timeoutMinutes.coerceAtLeast(1)
        val islandSeconds = (timeoutMinutes.coerceAtLeast(1) * 60).coerceAtMost(12 * 60 * 60)
        return buildString {
            append("{\"param_v2\":{")
            append("\"protocol\":1,")
            append("\"business\":\"study\",")
            append("\"enableFloat\":").append(floatOnPost).append(',')
            append("\"islandFirstFloat\":").append(floatOnPost).append(',')
            append("\"updatable\":true,")
            append("\"timeout\":").append(timeout).append(',')
            append("\"ticker\":\"").append(esc(ticker)).append("\",")
            append("\"tickerPic\":\"").append(PIC_ICON).append("\",")
            append("\"aodTitle\":\"").append(esc(aodTitle)).append("\",")
            append("\"aodPic\":\"").append(PIC_ICON).append("\",")
            append("\"progress\":").append(progress).append(',')
            append("\"baseInfo\":{")
            append("\"type\":1,")
            append("\"title\":\"").append(esc(title)).append("\",")
            append("\"content\":\"").append(esc(content)).append("\",")
            append("\"colorTitle\":\"#007AFF\"},")
            append("\"hintInfo\":{")
            append("\"type\":1,")
            append("\"title\":\"").append(esc(hintTitle)).append("\",")
            append("\"content\":\"").append(esc("剩余${remain}分钟")).append("\"},")
            append("\"param_island\":{")
            append("\"islandProperty\":1,")
            append("\"islandTimeout\":").append(islandSeconds).append(',')
            append("\"bigIslandArea\":{")
            append("\"progressTextInfo\":{")
            append("\"progressInfo\":{\"progress\":").append(progress).append("},")
            append("\"textInfo\":{")
            append("\"frontTitle\":\"学习\",")
            append("\"title\":\"").append(esc("${remain}分钟")).append("\",")
            append("\"content\":\"").append(esc(ticker)).append("\"}")
            append("}},")
            append("\"smallIslandArea\":{\"picInfo\":{\"type\":1,\"pic\":\"")
            append(PIC_ICON)
            append("\"}}")
            append("}}}")
        }
    }

    private fun esc(value: String): String = buildString(value.length + 8) {
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(ch)
            }
        }
    }
}
