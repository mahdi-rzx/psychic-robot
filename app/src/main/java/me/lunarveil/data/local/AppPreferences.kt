package me.lunarveil.data.local
import android.content.Context; import androidx.datastore.core.DataStore; import androidx.datastore.preferences.core.*; import androidx.datastore.preferences.preferencesDataStore; import kotlinx.coroutines.flow.Flow; import kotlinx.coroutines.flow.map
private val Context.ds: DataStore<Preferences> by preferencesDataStore(name = "lv")
class AppPreferences(private val ctx: Context) {
    private val TK = stringPreferencesKey("t"); private val TO = stringPreferencesKey("o"); private val TR = stringPreferencesKey("r"); private val TP = stringPreferencesKey("p"); private val TS = booleanPreferencesKey("s")
    val token: Flow<String> = ctx.ds.data.map { it[TK] ?: "" }; val owner: Flow<String> = ctx.ds.data.map { it[TO] ?: "" }; val repo: Flow<String> = ctx.ds.data.map { it[TR] ?: "" }; val otp: Flow<String> = ctx.ds.data.map { it[TP] ?: "" }; val setup: Flow<Boolean> = ctx.ds.data.map { it[TS] ?: false }
    suspend fun save(t: String, o: String, r: String, p: String) { ctx.ds.edit { it[TK] = t; it[TO] = o; it[TR] = r; it[TP] = p; it[TS] = true } }
}
