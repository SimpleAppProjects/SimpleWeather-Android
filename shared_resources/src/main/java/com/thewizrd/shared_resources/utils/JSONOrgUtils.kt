package com.thewizrd.shared_resources.utils

import org.json.JSONArray
import org.json.JSONObject

fun JSONArray.forEach(action: (Any) -> Unit) {
    for (i in 0 until this.length()) {
        action(this.get(i))
    }
}

fun JSONArray.forEachString(action: (String) -> Unit) {
    for (i in 0 until this.length()) {
        action(this.getString(i))
    }
}

fun JSONArray.forEachInt(action: (Int) -> Unit) {
    for (i in 0 until this.length()) {
        action(this.getInt(i))
    }
}

fun JSONArray.forEachDouble(action: (Double) -> Unit) {
    for (i in 0 until this.length()) {
        action(this.getDouble(i))
    }
}

fun JSONObject.getAsJSONObject(name: String): JSONObject? {
    return runCatching {
        this.getJSONObject(name)
    }.getOrNull()
}

fun JSONObject.getAsJSONArray(name: String): JSONArray? {
    return runCatching {
        this.getJSONArray(name)
    }.getOrNull()
}