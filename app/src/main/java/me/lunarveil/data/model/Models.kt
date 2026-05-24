package me.lunarveil.data.model
data class Chat(val id: Long, val name: String, val type: String, val unread: Int = 0, val lastMsg: String = "", val lastDate: String? = null, val archived: Boolean = false, val username: String? = null, val phone: String? = null, val lastMedia: String? = null)
data class Message(val id: Long, val date: String? = null, val text: String = "", val out: Boolean = false, val sender: String = "", val media: String? = null)
