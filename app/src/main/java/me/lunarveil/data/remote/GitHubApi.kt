package me.lunarveil.data.remote
import retrofit2.Response; import retrofit2.http.*
interface GitHubApi {
    @POST("repos/{owner}/{repo}/actions/workflows/telegram.yml/dispatches")
    suspend fun trigger(@Path("owner") o: String, @Path("repo") r: String, @Body b: TriggerBody, @Header("Authorization") a: String): Response<Unit>
    @GET("repos/{owner}/{repo}/contents/telegram_data/chats.json")
    suspend fun getChats(@Path("owner") o: String, @Path("repo") r: String, @Header("Authorization") a: String): GitHubContent
    @GET("repos/{owner}/{repo}/contents/telegram_data/chat_{id}.json")
    suspend fun getMessages(@Path("owner") o: String, @Path("repo") r: String, @Path("id") id: Long, @Header("Authorization") a: String): GitHubContent
}
data class TriggerBody(val ref: String = "main", val inputs: TriggerInputs)
data class TriggerInputs(val actions_json: String)
data class GitHubContent(val content: String?, val encoding: String?)
