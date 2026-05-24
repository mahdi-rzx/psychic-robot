package me.lunarveil.data.repository
import android.util.Base64; import com.google.gson.Gson; import com.google.gson.reflect.TypeToken; import me.lunarveil.data.model.*; import me.lunarveil.data.remote.*
class TelegramRepository(private val api: GitHubApi, private val token: String, private val owner: String, private val repo: String) {
    private val g = Gson(); private val a = "Bearer $token"
    suspend fun trigger(actions: String): Result<Unit> = try { val r = api.trigger(owner, repo, TriggerBody(inputs = TriggerInputs(actions)), a); if (r.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${r.code()}")) } catch (e: Exception) { Result.failure(e) }
    suspend fun getChats(): Result<List<Chat>> = try { val c = api.getChats(owner, repo, a); val j = decode(c.content); Result.success(g.fromJson(j, object : TypeToken<List<Chat>>() {}.type) ?: emptyList()) } catch (e: Exception) { Result.failure(e) }
    suspend fun getMessages(chatId: Long): Result<List<Message>> = try { val c = api.getMessages(owner, repo, chatId, a); val j = decode(c.content); Result.success(g.fromJson(j, object : TypeToken<List<Message>>() {}.type) ?: emptyList()) } catch (e: Exception) { Result.failure(e) }
    private fun decode(c: String?) = if (c.isNullOrBlank()) "" else try { String(Base64.decode(c.replace("\n", ""), Base64.DEFAULT)) } catch (e: Exception) { "" }
}
