package com.example.treasure.data.local.entity


import androidx.room.TypeConverter
import com.example.treasure.domain.uiModels.PriceState
import com.example.treasure.domain.uiModels.StoreDeal
import com.example.treasure.domain.uiModels.RequirementType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


data class SystemRequirementEntity(
    val specName: String,
    val specValue: String,
    val type: RequirementType // Store the enum directly
)

class Converters {
    private val gson = Gson()

    // --- EXISTING CONVERTERS (List<String>, StoreDeal, etc.) ---
    @TypeConverter
    fun fromStringList(value: List<String>?): String? = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromStoreDealList(value: List<StoreDeal>?): String? = gson.toJson(value)

    @TypeConverter
    fun toStoreDealList(value: String?): List<StoreDeal>? {
        val type = object : TypeToken<List<StoreDeal>>() {}.type
        return gson.fromJson(value, type)
    }

    // --- NEW: LIST<SYSTEM_REQUIREMENT> ---
    @TypeConverter
    fun fromSysReqList(value: List<SystemRequirementEntity>?): String? = gson.toJson(value)

    @TypeConverter
    fun toSysReqList(value: String?): List<SystemRequirementEntity>? {
        val type = object : TypeToken<List<SystemRequirementEntity>>() {}.type
        return gson.fromJson(value, type)
    }

    // --- ENUMS ---
    @TypeConverter
    fun fromDealCategory(category: DealCategory): String = category.name

    @TypeConverter
    fun toDealCategory(value: String): DealCategory = enumValueOf(value)
}