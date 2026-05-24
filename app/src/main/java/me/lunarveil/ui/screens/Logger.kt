package me.lunarveil.ui.screens
import androidx.compose.runtime.mutableStateListOf
object Logger { val logs = mutableStateListOf<String>(); fun i(t: String, m: String) { logs.add("[INFO][$t] $m") }; fun e(t: String, m: String) { logs.add("[ERROR][$t] $m") } }
