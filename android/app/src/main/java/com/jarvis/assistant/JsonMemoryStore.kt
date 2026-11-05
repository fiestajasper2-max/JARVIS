package com.jarvis.assistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class JsonMemoryStore(context: Context) {
    private val memoryDir = File(context.filesDir, "memory").apply { mkdirs() }
    private val conversationFile = File(memoryDir, "conversations.json")

    @Synchronized
    fun loadConversation(): List<ChatMessage> {
        if (!conversationFile.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(conversationFile.readText())
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(ChatMessage(item.getString("role"), item.getString("content")))
                }
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun saveConversation(messages: List<ChatMessage>) {
        val array = JSONArray()
        messages.takeLast(300).forEach {
            array.put(JSONObject().put("role", it.role).put("content", it.content))
        }
        val temp = File(memoryDir, "conversations.tmp")
        temp.writeText(array.toString(2))
        if (!temp.renameTo(conversationFile)) {
            conversationFile.writeText(array.toString(2))
            temp.delete()
        }
    }
}
