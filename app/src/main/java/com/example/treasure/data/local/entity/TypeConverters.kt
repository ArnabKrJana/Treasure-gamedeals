package com.example.treasure.data.local.entity

import androidx.room.TypeConverter
import com.example.treasure.domain.uiModels.RequirementType
import com.example.treasure.domain.uiModels.StoreDeal
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Persistence entity encapsulating PC hardware specification requirements stored in Room via JSON.
 *
 * @property specName Hardware specification category (e.g., "OS", "Processor", "Memory", "Graphics").
 * @property specValue Hardware capability description string.
 * @property type Hardware tier enum distinguishing minimum vs recommended tiers ([RequirementType]).
 */
data class SystemRequirementEntity(
    val specName: String,
    val specValue: String,
    val type: RequirementType
)

/**
 * Room [TypeConverter] definitions enabling persistence of complex data structures and enums into SQLite columns.
 *
 * Employs [Gson] for JSON serialization and deserialization of nested collections and entities.
 */
class Converters {
    private val gson = Gson()

    /**
     * Serializes a list of strings into a JSON string for SQLite storage.
     *
     * @param value List of strings to serialize.
     * @return Serialized JSON string, or `null`.
     */
    @TypeConverter
    fun fromStringList(value: List<String>?): String? = gson.toJson(value)

    /**
     * Deserializes a JSON string into a list of strings.
     *
     * @param value Stored JSON string.
     * @return Deserialized list of strings, or `null`.
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    /**
     * Serializes a list of [StoreDeal] objects into a JSON string.
     *
     * @param value List of store deals.
     * @return Serialized JSON string, or `null`.
     */
    @TypeConverter
    fun fromStoreDealList(value: List<StoreDeal>?): String? = gson.toJson(value)

    /**
     * Deserializes a JSON string into a list of [StoreDeal] objects.
     *
     * @param value Stored JSON string.
     * @return Deserialized list of store deals, or `null`.
     */
    @TypeConverter
    fun toStoreDealList(value: String?): List<StoreDeal>? {
        val type = object : TypeToken<List<StoreDeal>>() {}.type
        return gson.fromJson(value, type)
    }

    /**
     * Serializes a list of [SystemRequirementEntity] objects into a JSON string.
     *
     * @param value List of hardware requirements.
     * @return Serialized JSON string, or `null`.
     */
    @TypeConverter
    fun fromSysReqList(value: List<SystemRequirementEntity>?): String? = gson.toJson(value)

    /**
     * Deserializes a JSON string into a list of [SystemRequirementEntity] objects.
     *
     * @param value Stored JSON string.
     * @return Deserialized list of hardware requirements, or `null`.
     */
    @TypeConverter
    fun toSysReqList(value: String?): List<SystemRequirementEntity>? {
        val type = object : TypeToken<List<SystemRequirementEntity>>() {}.type
        return gson.fromJson(value, type)
    }

    /**
     * Converts a [DealCategory] enum instance into its string name for SQLite column storage.
     *
     * @param category The deal category enum constant.
     * @return String name of the enum constant.
     */
    @TypeConverter
    fun fromDealCategory(category: DealCategory): String = category.name

    /**
     * Converts a stored string name back into its corresponding [DealCategory] enum constant.
     *
     * @param value The stored category name.
     * @return Matching [DealCategory] constant.
     */
    @TypeConverter
    fun toDealCategory(value: String): DealCategory = enumValueOf(value)
}
