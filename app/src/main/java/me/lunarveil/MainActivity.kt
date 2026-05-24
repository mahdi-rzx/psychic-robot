package me.lunarveil

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.lunarveil.BuildConfig
import me.lunarveil.data.local.AppPreferences
import me.lunarveil.data.model.Chat
import me.lunarveil.data.model.Message
import me.lunarveil.data.remote.RetrofitClient
import me.lunarveil.data.repository.TelegramRepository
import me.lunarveil.ui.screens.Logger
import me.lunarveil.ui.theme.LunarVeilTheme

data class TabInfo(val title: String, val icon: ImageVector, val selectedIcon: ImageVector)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LunarVeilTheme {
                var screen by remember { mutableStateOf("loading") }
                var savedToken by remember { mutableStateOf("") }
                var savedOwner by remember { mutableStateOf("") }
                var savedRepo by remember { mutableStateOf("") }
                var savedOtp by remember { mutableStateOf("") }
                var setupDone by remember { mutableStateOf(false) }
                val prefs = remember { AppPreferences(this@MainActivity) }

                LaunchedEffect(Unit) {
                    try {
                        withContext(Dispatchers.IO) {
                            setupDone = prefs.setup.first()
                            savedOtp = prefs.otp.first()
                            savedToken = prefs.token.first()
                            savedOwner = prefs.owner.first()
                            savedRepo = prefs.repo.first()
                        }
                    } catch (e: Exception) {
                        Log.e("LunarVeil", "Load prefs failed", e)
                    }
                    screen = if (!setupDone) "setup" else "lock"
                }

                when (screen) {
                    "loading" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    "setup" -> SetupScreen { t, o, r, p ->
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch { try { prefs.save(t, o, r, p) } catch (e: Exception) { Log.e("LunarVeil","Save",e) } }
                        savedToken = t; savedOwner = o; savedRepo = r; savedOtp = p
                        screen = "main"
                    }
                    "lock" -> LockScreen(savedOtp) { screen = "main" }
                    "main" -> MainApp(savedToken, savedOwner, savedRepo)
                }
            }
        }
    }
}

@Composable fun SetupScreen(onComplete: (String, String, String, String) -> Unit) {
    var token by remember { mutableStateOf("") }; var owner by remember { mutableStateOf("") }; var repo by remember { mutableStateOf("") }; var otp by remember { mutableStateOf("") }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🌙 LunarVeil", style = MaterialTheme.typography.headlineLarge); Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(value = token, onValueChange = { token = it }, label = { Text("GitHub Token") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("Repo Owner") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = repo, onValueChange = { repo = it }, label = { Text("Repo Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = otp, onValueChange = { otp = it }, label = { Text("App Password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(24.dp)); Button(onClick = { onComplete(token, owner, repo, otp) }, enabled = token.isNotBlank() && owner.isNotBlank() && repo.isNotBlank() && otp.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save") }
        }
    }
}

@Composable fun LockScreen(expectedOtp: String, onUnlock: () -> Unit) {
    var otp by remember { mutableStateOf("") }; var error by remember { mutableStateOf(false) }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🔒 LunarVeil", style = MaterialTheme.typography.headlineLarge); Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(value = otp, onValueChange = { otp = it; error = false }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true, isError = error, modifier = Modifier.fillMaxWidth())
            if (error) Text(text = "Wrong password", color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(24.dp)); Button(onClick = { if (otp == expectedOtp) onUnlock() else error = true }, modifier = Modifier.fillMaxWidth()) { Text("Unlock") }
        }
    }
}

@Composable fun ChatRow(c: Chat, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), color = MaterialTheme.colorScheme.surface) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Text(text = c.name.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer) } }
            Spacer(modifier = Modifier.width(12.dp)); Column(modifier = Modifier.weight(1f)) { Text(text = c.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(text = c.lastMsg.ifEmpty { c.lastMedia?.let { "📎 Media" } ?: "" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            if (c.unread > 0) Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary) { Text(text = "${c.unread}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary) }
        }
    }
}

@Composable fun ChatScreen(name: String, messages: List<Message>, onBack: () -> Unit, onSend: (String) -> Unit) {
    var input by remember { mutableStateOf("") }; val list = rememberLazyListState()
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) list.animateScrollToItem(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = onBack) { Text("←") }; Text(text = name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        HorizontalDivider()
        LazyColumn(state = list, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), reverseLayout = true) {
            items(items = messages.reversed()) { m ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalAlignment = if (m.out) Alignment.End else Alignment.Start) {
                    Surface(shape = MaterialTheme.shapes.medium, color = if (m.out) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.widthIn(max = 280.dp)) {
                        Column(modifier = Modifier.padding(8.dp)) { if (!m.out && m.sender.isNotBlank()) Text(text = m.sender, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary); if (m.text.isNotBlank()) Text(text = m.text, style = MaterialTheme.typography.bodyMedium); if (!m.media.isNullOrEmpty()) Text(text = "📎 ${m.media}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Message...") }, singleLine = true); Spacer(modifier = Modifier.width(8.dp)); Button(onClick = { if (input.isNotBlank()) { onSend(input); input = "" } }) { Text("Send") } }
    }
}

@Composable fun LogsScreen() {
    val list = rememberLazyListState(); val scope = rememberCoroutineScope()
    LaunchedEffect(Logger.logs.size) { if (Logger.logs.isNotEmpty()) scope.launch { list.animateScrollToItem(Logger.logs.size - 1) } }
    Column(modifier = Modifier.fillMaxSize()) { Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) { TextButton(onClick = { Logger.logs.clear() }) { Text("Clear") } }; LazyColumn(state = list, modifier = Modifier.fillMaxSize()) { items(items = Logger.logs.toList()) { Text(text = it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) } } }
}

@Composable fun SettingsScreen(t: String, o: String, r: String, onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = onBack) { Text("← Back") }; Spacer(modifier = Modifier.weight(1f)); Text("Settings", style = MaterialTheme.typography.titleLarge) }
            Spacer(modifier = Modifier.height(16.dp)); Text("GitHub", style = MaterialTheme.typography.titleMedium); Spacer(modifier = Modifier.height(8.dp))
            ListItem(headlineContent = { Text("Token") }, supportingContent = { Text(t.take(8) + "..." + t.takeLast(4)) }); ListItem(headlineContent = { Text("Repo") }, supportingContent = { Text("$o/$r") })
            HorizontalDivider(); Spacer(modifier = Modifier.height(16.dp)); Text("App", style = MaterialTheme.typography.titleMedium); Spacer(modifier = Modifier.height(8.dp))
            ListItem(headlineContent = { Text("Version") }, supportingContent = { Text(BuildConfig.VERSION_NAME) })
        }
    }
}

@Composable fun AboutScreen(v: String) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🌙", style = MaterialTheme.typography.displayLarge); Spacer(modifier = Modifier.height(12.dp)); Text("LunarVeil", style = MaterialTheme.typography.headlineLarge); Text("v$v", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp)); Text("Telegram Bridge Client"); Text("Powered by GitHub Actions", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(modifier = Modifier.height(24.dp)); Text("me.lunarveil", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MainApp(token: String, owner: String, repo: String) {
    val nav = rememberNavController(); val drawer = rememberDrawerState(DrawerValue.Closed); val scope = rememberCoroutineScope()
    var chats by remember { mutableStateOf<List<Chat>>(emptyList()) }; var msgs by remember { mutableStateOf<List<Message>>(emptyList()) }; var loading by remember { mutableStateOf(true) }
    var cId by remember { mutableStateOf<Long?>(null) }; var cName by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }
    val snack = remember { SnackbarHostState() }
    val repository = remember { TelegramRepository(RetrofitClient.api, token, owner, repo) }

    LaunchedEffect(Unit) {
        loading = true; error = null
        withContext(Dispatchers.IO) { repository.getChats().fold(onSuccess = { chats = it }, onFailure = { error = it.message }) }
        loading = false
    }

    val tabs = listOf(TabInfo("Chats", Icons.Outlined.Chat, Icons.Filled.Chat), TabInfo("Logs", Icons.Outlined.List, Icons.Filled.List), TabInfo("About", Icons.Outlined.Info, Icons.Filled.Info))
    val bs by nav.currentBackStackEntryAsState(); val dest = bs?.destination; val detail = dest?.route == "detail" || dest?.route == "settings"

    ModalNavigationDrawer(drawerState = drawer, gesturesEnabled = !detail, drawerContent = {
        ModalDrawerSheet {
            Spacer(modifier = Modifier.height(16.dp)); Text(text = "LunarVeil", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall); HorizontalDivider()
            NavigationDrawerItem(icon = { Text("▶") }, label = { Text("Run Workflow") }, selected = false, onClick = { scope.launch { drawer.close(); withContext(Dispatchers.IO) { repository.trigger(Gson().toJson(listOf(mapOf("type" to "get_chats")))) }; snack.showSnackbar("Triggered!") } })
            NavigationDrawerItem(icon = { Text("⬇") }, label = { Text("Fetch Results") }, selected = false, onClick = { scope.launch { drawer.close(); loading = true; error = null; withContext(Dispatchers.IO) { repository.getChats().fold(onSuccess = { chats = it }, onFailure = { error = it.message }) }; loading = false } })
            HorizontalDivider()
            NavigationDrawerItem(icon = { Text("⚙") }, label = { Text("Settings") }, selected = false, onClick = { scope.launch { drawer.close(); nav.navigate("settings") } })
        }
    }) {
        Scaffold(
            snackbarHost = { SnackbarHost(snack) },
            topBar = { if (!detail) TopAppBar(title = { Text("LunarVeil") }, navigationIcon = { IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
            bottomBar = { if (!detail) NavigationBar { tabs.forEachIndexed { i, t -> val sel = dest?.hierarchy?.any { it.route == "s$i" } == true; NavigationBarItem(icon = { Icon(imageVector = if (sel) t.selectedIcon else t.icon, contentDescription = t.title) }, label = { Text(t.title) }, selected = sel, onClick = { nav.navigate("s$i") { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } }) } } }
        ) { inner ->
            NavHost(navController = nav, startDestination = "s0", modifier = Modifier.padding(inner)) {
                composable(route = "s0") {
                    if (loading) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                    else if (error != null) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(text = "⚠️ $error", color = MaterialTheme.colorScheme.error); Spacer(modifier = Modifier.height(16.dp)); Button(onClick = { loading = true; scope.launch { withContext(Dispatchers.IO) { repository.getChats().fold(onSuccess = { chats = it; error = null }, onFailure = { error = it.message }) }; loading = false } }) { Text("Retry") } } } }
                    else if (chats.isEmpty()) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("No chats"); Spacer(modifier = Modifier.height(8.dp)); Button(onClick = { scope.launch { drawer.open() } }) { Text("Open Menu") } } } }
                    else { val main = chats.filter { !it.archived }; val arch = chats.filter { it.archived }; LazyColumn(modifier = Modifier.fillMaxSize()) { items(items = main) { c -> ChatRow(c = c, onClick = { cId = c.id; cName = c.name; scope.launch { withContext(Dispatchers.IO) { repository.getMessages(c.id).fold(onSuccess = { msgs = it }, onFailure = {}) }; nav.navigate("detail") } }) }; if (arch.isNotEmpty()) { item { Text(text = "📦 Archived", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }; items(items = arch) { c -> ChatRow(c = c, onClick = { cId = c.id; cName = c.name; scope.launch { withContext(Dispatchers.IO) { repository.getMessages(c.id).fold(onSuccess = { msgs = it }, onFailure = {}) }; nav.navigate("detail") } }) } } } }
                }
                composable(route = "detail") { ChatScreen(name = cName, messages = msgs, onBack = { nav.popBackStack() }, onSend = { txt -> scope.launch { repository.trigger(Gson().toJson(listOf(mapOf("type" to "send_message", "chat_id" to cId, "text" to txt), mapOf("type" to "get_messages", "chat_id" to cId, "limit" to 50)))); snack.showSnackbar("Queued!") } }) }
                composable(route = "settings") { SettingsScreen(t = token, o = owner, r = repo, onBack = { nav.popBackStack() }) }
                composable(route = "s1") { LogsScreen() }
                composable(route = "s2") { AboutScreen(v = BuildConfig.VERSION_NAME) }
            }
        }
    }
}
