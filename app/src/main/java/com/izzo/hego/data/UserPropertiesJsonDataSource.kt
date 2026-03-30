package com.izzo.hego.data

import android.content.Context
import org.json.JSONObject

data class UserProperties(val name: String, val isValid: Boolean)

object UserPropertiesJsonDataSource {
    fun load(
        context: Context,
        fileName: String = "user_properties.json",
    ): UserProperties {
        return runCatching {
            val json = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val obj = JSONObject(json)
            UserProperties(
                name = obj.optString("name", "Unknown"),
                isValid = obj.optBoolean("isValid", false)
            )
        }.getOrDefault(UserProperties(name = "Unknown", isValid = false))
    }
}

