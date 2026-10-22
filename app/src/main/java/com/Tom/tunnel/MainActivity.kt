package com.tom.tunnel

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tom.tunnel.config.Profile
import com.tom.tunnel.config.ProfileCodec
import com.tom.tunnel.config.ProfileStore
import com.tom.tunnel.identity.DeviceIdentity

private val Navy = Color(0xFF07111F)
private val Card = Color(0xFF111F2E)
private val Neon = Color(0xFF39F2A5)
private val Cyan = Color(0xFF43D9F5)
private val Danger = Color(0xFFFF5D68)

class MainActivity : ComponentActivity() {
    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Permission result is intentionally not treated as a successful connection.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val identity = DeviceIdentity.getOrCreate(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Neon, secondary = Cyan, background = Navy, surface = Card,
                onPrimary = Navy, onBackground = Color.White, onSurface = Color.White,
                error = Danger
            )) {
                TunnelApp(identity) {
                    val intent = VpnService.prepare(this)
                    if (intent != null) vpnPermission.launch(intent)
                    else startService(Intent(this, com.tom.tunnel.vpn.TomTunnelVpnService::class.java).setAction("START"))
                }
            }
        }
    }
}

@Composable
private fun TunnelApp(identity: String, requestConnect: () -> Unit) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    var profile by remember { mutableStateOf(Profile.sample()) }
    var note by remember { mutableStateOf("Xray engine not bundled yet — VPN connection disabled for safety.") }
    var language by remember { mutableStateOf("fr") }
    var importText by remember { mutableStateOf("") }
    val profiles = remember { ProfileStore(context) }
    var savedProfiles by remember { mutableStateOf(profiles.listProfiles()) }
    var pendingExport by remember { mutableStateOf("") }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(pendingExport.toByteArray(Charsets.UTF_8)) }
                note = "Configuration exportée. Gardez ce fichier privé : il peut contenir des identifiants."
            } catch (e: Exception) { note = "Export impossible : ${e.message ?: "erreur d'écriture"}" }
        } else note = "Export annulé."
    }

    Column(Modifier.fillMaxSize().background(Navy).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("TOM_TUNNEL", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Neon)
                Text("VERSION 1.0.0 • ${identity.take(15)}", style = MaterialTheme.typography.labelSmall, color = Cyan)
            }
            TextButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/FREE_INTERNET237")))
            }) { Text("Telegram", color = Cyan) }
            Icon(Icons.Default.Security, contentDescription = null, tint = Neon, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Connexion", "Journal", "Paramètres").forEachIndexed { i, label ->
                FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(12.dp))
        when (tab) {
            0 -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = Card, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ÉTAT DU TUNNEL", color = Cyan, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("DÉCONNECTÉ", color = Danger, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = { note = "Connexion indisponible : le pont TUN vers Xray et le cycle de vie du moteur ne sont pas encore opérationnels. Aucun trafic n'est envoyé dans un faux tunnel." },
                            colors = ButtonDefaults.buttonColors(containerColor = Neon),
                            modifier = Modifier.size(180.dp), shape = RoundedCornerShape(90.dp)) {
                            Text("CONNECTER", color = Navy, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("Ping : —    Download : —    Upload : —", color = Cyan)
                        Text("Données : —    Durée : —", color = Color.LightGray)
                        Text("Serveur : ${profile.server}:${profile.port}", color = Color.LightGray)
                        Text("Protocole : ${profile.protocol}", color = Color.LightGray)
                    }
                }
                Surface(color = Card, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Configuration sélectionnée", fontWeight = FontWeight.Bold)
                        Text(profile.name, color = Neon)
                        Text("${profile.protocol} • ${profile.transport} • ${profile.security}", color = Cyan)
                        OutlinedTextField(value = importText, onValueChange = { importText = it },
                            label = { Text("Coller un lien VLESS / VMess / Trojan ou JSON") },
                            modifier = Modifier.fillMaxWidth(), minLines = 2)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                try {
                                    profile = ProfileCodec.parse(importText)
                                    profiles.save(profile)
                                    savedProfiles = profiles.listProfiles()
                                    note = "Configuration importée et enregistrée localement. Vérifiez les paramètres avant utilisation."
                                } catch (e: Exception) { note = "Import impossible : ${e.message ?: "format invalide"}" }
                            }) { Text("Analyser") }
                            OutlinedButton(onClick = {
                                try {
                                    val saved = ProfileCodec.export(profile, identity)
                                    pendingExport = saved.second
                                    exportLauncher.launch(saved.first)
                                } catch (e: Exception) { note = "Export impossible : ${e.message ?: "profil invalide"}" }
                            }) { Text("Exporter") }
                        }
                    }
                }
                if (savedProfiles.isNotEmpty()) {
                    Surface(color = Card, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Profils enregistrés (${savedProfiles.size})", fontWeight = FontWeight.Bold)
                            savedProfiles.forEach { item ->
                                OutlinedButton(onClick = { profile = item; note = "Profil sélectionné : ${item.name}" }, modifier = Modifier.fillMaxWidth()) {
                                    Text("${item.name} — ${item.protocol} — ${item.server}:${item.port}")
                                }
                            }
                        }
                    }
                }
                Text(note, color = if (note.contains("impossible") || note.contains("indisponible")) Danger else Cyan)
            }
            1 -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text("JOURNAL", color = Neon, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Text("[$identity] Application initialisée.", color = Color.LightGray)
                Text("Aucun journal de tunnel réel : le moteur n'est pas encore intégré.", color = Danger)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { note = "Aucun journal réseau à copier." }) { Text("Copier les journaux") }
            }
            else -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("PARAMÈTRES", color = Neon, style = MaterialTheme.typography.titleLarge)
                Text("Identité de l'appareil", fontWeight = FontWeight.Bold)
                SelectableTextLike(identity)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("TOM_TUNNEL ID", identity))
                        note = "Identité copiée."
                    }) { Text("Copier l'ID") }
                    OutlinedButton(onClick = { language = if (language == "fr") "en" else "fr" }) { Text("Langue : ${if (language == "fr") "Français" else "English"}") }
                }
                Text("Sécurité", fontWeight = FontWeight.Bold)
                Text("Les options anti-fuite ne seront actives qu'après l'intégration et la validation du tunnel complet.", color = Color.LightGray)
                Button(onClick = {
                    val i = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/FREE_INTERNET237"))
                    context.startActivity(i)
                }) { Text("Rejoindre le canal Telegram @FREE_INTERNET237") }
                Text("TOM_TUNNEL 1.0.0 • Xray-core • licences open source à compléter après intégration.", color = Cyan)
            }
        }
    }
}

@Composable
private fun SelectableTextLike(value: String) {
    Surface(color = Card, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Text(value, modifier = Modifier.padding(12.dp), color = Cyan)
    }
}
