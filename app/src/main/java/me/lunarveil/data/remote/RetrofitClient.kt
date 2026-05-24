package me.lunarveil.data.remote
import okhttp3.OkHttpClient; import retrofit2.Retrofit; import retrofit2.converter.gson.GsonConverterFactory; import java.util.concurrent.TimeUnit
object RetrofitClient {
    private val c = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    private val r = Retrofit.Builder().baseUrl("https://api.github.com/").client(c).addConverterFactory(GsonConverterFactory.create()).build()
    val api: GitHubApi = r.create(GitHubApi::class.java)
}
