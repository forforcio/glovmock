package com.izzo.hego.data

import android.content.Context
import com.izzo.hego.model.Delivery
import org.json.JSONArray

object DeliveryJsonDataSource {
    fun loadFromAssets(context: Context, fileName: String = "deliveries_test.json"): List<Delivery> {
        return runCatching {
            val json = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val name = item.optString("restaurantName").trim()
                    val address = item.optString("restaurantAddress").trim()
                    val type = item.optString("restaurantType").trim()
                    if (name.isEmpty() || address.isEmpty()) continue
                    add(
                        Delivery(
                            id = item.optString("id").ifBlank { "delivery_$index" },
                            restaurantName = name,
                            restaurantAddress = address,
                            restaurantType = type.ifBlank { "general" },
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}

