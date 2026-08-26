package com.diamond.gdapplication.data

import android.content.Context
import org.json.JSONArray

class SearchHistoryStore(context: Context) {

    private val preferences = context.getSharedPreferences(
        "search_history",
        Context.MODE_PRIVATE
    )

    fun load(): List<String> {
        return try {
            val raw = preferences.getString("keywords", "[]") ?: "[]"
            val array = JSONArray(raw)

            buildList {
                for (index in 0 until array.length()) {
                    val keyword = array.optString(index)

                    if (keyword.isNotBlank()) {
                        add(keyword)
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun add(keyword: String) {
        val cleanKeyword = keyword.trim()

        if (cleanKeyword.isEmpty()) {
            return
        }

        val history = load()
            .filterNot {
                it.equals(cleanKeyword, ignoreCase = true)
            }
            .toMutableList()

        history.add(0, cleanKeyword)
        save(history.take(10))
    }

    fun clear() {
        preferences
            .edit()
            .remove("keywords")
            .apply()
    }

    private fun save(history: List<String>) {
        val array = JSONArray()

        history.forEach(array::put)

        preferences
            .edit()
            .putString("keywords", array.toString())
            .apply()
    }
}