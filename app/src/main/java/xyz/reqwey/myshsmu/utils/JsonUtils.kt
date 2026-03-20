package xyz.reqwey.myshsmu.utils

import org.json.JSONObject

fun JSONObject.optCleanString(key: String, default: String = ""): String {
    if (isNull(key)) return default
    val value = opt(key) ?: return default
    if (value == JSONObject.NULL) return default
    val text = value.toString().trim()
    return if (text.equals("null", ignoreCase = true)) default else text
}
