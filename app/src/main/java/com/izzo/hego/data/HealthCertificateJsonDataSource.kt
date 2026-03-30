package com.izzo.hego.data

import android.content.Context
import org.json.JSONObject

object HealthCertificateJsonDataSource {
    fun isCertificateValid(
        context: Context,
        fileName: String = "health_certificate_test.json",
    ): Boolean {
        return runCatching {
            val json = context.assets.open(fileName).bufferedReader().use { it.readText() }
            JSONObject(json).optBoolean("isValid", false)
        }.getOrDefault(false)
    }
}

