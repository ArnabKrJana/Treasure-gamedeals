package com.example.treasure.data.remote.networkDto.steam


import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class SteamRequirementsDeserializer : JsonDeserializer<SteamRequirementsDto?> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): SteamRequirementsDto? {
        // 1. If Steam sends an empty array [], return null
        if (json.isJsonArray) {
            return null
        }
        // 2. Otherwise, parse it as a normal Object
        return Gson().fromJson(json, SteamRequirementsDto::class.java)
    }
}