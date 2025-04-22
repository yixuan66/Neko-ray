package com.neko.server

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class PingHistoryManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("ping_history", Context.MODE_PRIVATE)

    fun savePing(server: String, ping: Int) {
        val history = getHistory(server).toMutableList()

        if (history.size >= 7) {
            history.removeAt(0) // keep max 7 entries
        }
        history.add(ping)

        val json = JSONArray()
        history.forEach { json.put(it) }

        prefs.edit().putString(server, json.toString()).apply()
    }

    fun getHistory(server: String): List<Int> {
        val json = prefs.getString(server, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            List(array.length()) { i -> array.getInt(i) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun exportToCsv(): String {
        val all = prefs.all
        val builder = StringBuilder("Server,Day1,Day2,Day3,Day4,Day5,Day6,Day7\n")
        all.forEach { (server, data) ->
            val list = try {
                val array = JSONArray(data as String)
                List(array.length()) { i -> array.getInt(i).toString() }
            } catch (e: Exception) {
                listOf()
            }
            builder.append(server).append(",").append(list.joinToString(",")).append("\n")
        }
        return builder.toString()
    }
}
