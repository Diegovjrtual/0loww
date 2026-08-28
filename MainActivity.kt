package com.olow.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

private val Bg = Color(0xFF0B0D10)
private val Panel = Color(0xFF11141A)
private val Panel2 = Color(0xFF181C23)
private val Accent = Color(0xFF5865F2)

class MainActivity : ComponentActivity() {
    private val screenShareCode = 501
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OlowApp() }
    }

    fun startScreenShare() {
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mgr.createScreenCaptureIntent(), screenShareCode)
    }
}

private class OlowStore(context: Context) {
    private val p = context.getSharedPreferences("olow", Context.MODE_PRIVATE)
    var name: String
        get() = p.getString("name", "0low User") ?: "0low User"
        set(v) = p.edit().putString("name", v).apply()
    var logged: Boolean
        get() = p.getBoolean("logged", false)
        set(v) = p.edit().putBoolean("logged", v).apply()
    var nitro: Boolean
        get() = p.getBoolean("nitro", false)
        set(v) = p.edit().putBoolean("nitro", v).apply()
    var boosts: Int
        get() = p.getInt("boosts", 0)
        set(v) = p.edit().putInt("boosts", v).apply()
}

enum class Page { HOME, DMS, PROFILE, SETTINGS, SERVER }

data class Msg(val author: String, val text: String)

data class Server(val name: String, val channels: MutableList<String> = mutableListOf("Geral", "memes", "avisos", "Sala de voz"))

@Composable
fun OlowApp() {
    val context = LocalContext.current
    val store = remember { OlowStore(context) }
    var logged by remember { mutableStateOf(store.logged) }
    if (!logged) {
        LoginScreen {
            store.name = it.ifBlank { "0low User" }
            store.logged = true
            logged = true
        }
    } else {
        MainShell(store)
    }
}

@Composable
fun LoginScreen(onLogin: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("0low", fontSize = 48.sp, fontWeight = FontWeight.Black)
            Text("0low Destiny", color = Color.Gray)
            Spacer(Modifier.height(26.dp))
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nome") })
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("E-mail") })
            Spacer(Modifier.height(10.dp))
            if (sent) OutlinedTextField(code, { code = it }, Modifier.fillMaxWidth(), label = { Text("Código") })
            Spacer(Modifier.height(12.dp))
            Button(onClick = { sent = true }, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Email, null); Spacer(Modifier.width(8.dp)); Text(if (sent) "Reenviar código" else "Enviar código")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onLogin(name) }, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Login, null); Spacer(Modifier.width(8.dp)); Text(if (sent) "Entrar" else "Entrar com Google")
            }
            Text("Modo local: os dados ficam salvos no aparelho. A autenticação online entra quando um backend for conectado.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
fun MainShell(store: OlowStore) {
    var page by remember { mutableStateOf(Page.HOME) }
    var server by remember { mutableStateOf(Server("0low Destiny")) }
    var selectedChannel by remember { mutableStateOf("Geral") }
    var messages by remember { mutableStateOf(listOf(Msg("0low", "Bem-vindo ao 0low Destiny!"))) }
    var dms by remember { mutableStateOf(listOf(Msg("Amigo", "Fala! 👋"), Msg("0low", "Sistema funcionando localmente."))) }
    var newText by remember { mutableStateOf("") }
    var showCreateChannel by remember { mutableStateOf(false) }
    var showCreateServer by remember { mutableStateOf(false) }
    var showNitro by remember { mutableStateOf(false) }
    var showRoles by remember { mutableStateOf(false) }
    var showCall by remember { mutableStateOf(false) }
    var purchaseFx by remember { mutableStateOf(false) }
    var avatar by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> avatar = uri }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) context.startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE)) else toast(context, "Permissão de câmera negada")
    }
    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok) toast(context, "Permissão de microfone negada")
    }

    Row(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.width(70.dp).fillMaxHeight().background(Color(0xFF080A0D)), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(14.dp)); ServerIcon("0") { page = Page.SERVER }
            Spacer(Modifier.height(10.dp)); IconButton({ showCreateServer = true }) { Icon(Icons.Default.Add, "Criar servidor") }
            IconButton({ page = Page.DMS }) { Icon(Icons.Default.Message, "Mensagens") }
            IconButton({ page = Page.PROFILE }) { Icon(Icons.Default.Person, "Perfil") }
        }
        Column(Modifier.width(235.dp).fillMaxHeight().background(Panel)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(server.name, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                IconButton({ page = Page.SERVER }) { Icon(Icons.Default.ExpandMore, "Servidor") }
            }
            HorizontalDivider(color = Panel2)
            Text("CANAIS", Modifier.padding(12.dp), color = Color.Gray, fontSize = 12.sp)
            server.channels.forEach { ch ->
                Row(Modifier.fillMaxWidth().clickable { selectedChannel = ch; page = Page.HOME }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (ch.contains("voz", true)) Icons.Default.VolumeUp else Icons.Default.Tag, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp)); Text(ch)
                }
            }
            Row(Modifier.fillMaxWidth().clickable { showCreateChannel = true }.padding(10.dp)) { Icon(Icons.Default.Add, null, tint = Color.Gray); Spacer(Modifier.width(8.dp)); Text("Criar canal") }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(avatar, 38); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)) { Text(store.name, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text(if (store.nitro) "0low Nitro" else "online", color = if (store.nitro) Accent else Color(0xFF55CC77), fontSize = 11.sp) }
                IconButton({ page = Page.SETTINGS }) { Icon(Icons.Default.Settings, "Configurações") }
            }
        }
        Column(Modifier.fillMaxSize()) {
            TopBar(selectedChannel,
                onCall = { showCall = true; audioPermission.launch(Manifest.permission.RECORD_AUDIO) },
                onCamera = { cameraPermission.launch(Manifest.permission.CAMERA) },
                onShare = { (context as? MainActivity)?.startScreenShare() },
                onMembers = { showRoles = true }
            )
            when (page) {
                Page.HOME -> Chat(messages, newText, { newText = it }, { if (newText.isNotBlank()) { messages = messages + Msg(store.name, newText); newText = "" } }, selectedChannel)
                Page.DMS -> Chat(dms, newText, { newText = it }, { if (newText.isNotBlank()) { dms = dms + Msg(store.name, newText); newText = "" } }, "DM")
                Page.PROFILE -> Profile(store, avatar, { gallery.launch("image/*") }, { showNitro = true })
                Page.SETTINGS -> Settings(store, { page = Page.PROFILE }, { store.logged = false; (context as Activity).recreate() })
                Page.SERVER -> ServerAdmin(server, { showCreateChannel = true }, { showRoles = true }, { store.boosts++; toast(context, "Servidor impulsionado!") }, { showNitro = true })
            }
        }
    }

    if (showCreateChannel) DialogText("Novo canal", "Nome do canal", "Criar", { value -> if (value.isNotBlank()) server = server.copy(channels = (server.channels + value).toMutableList()); showCreateChannel = false }) { showCreateChannel = false }
    if (showCreateServer) DialogText("Novo servidor", "Nome", "Criar", { value -> if (value.isNotBlank()) { server = Server(value); selectedChannel = "Geral" }; showCreateServer = false }) { showCreateServer = false }
    if (showRoles) RoleDialog { showRoles = false }
    if (showNitro) NitroDialog(store) { store.nitro = true; purchaseFx = true; showNitro = false }
    if (showCall) CallDialog(onClose = { showCall = false }, onCamera = { cameraPermission.launch(Manifest.permission.CAMERA) }, onShare = { (context as? MainActivity)?.startScreenShare() })
    if (purchaseFx) {
        var alpha by remember { mutableStateOf(1f) }
        LaunchedEffect(Unit) { delay(1400); purchaseFx = false }
        val a by animateFloatAsState(if (purchaseFx) 1f else 0f, label = "purchase")
        Box(Modifier.fillMaxSize().alpha(a), contentAlignment = Alignment.Center) {
            Card(shape = RoundedCornerShape(28.dp)) { Text("✨ 0low Nitro ativado! ✨", Modifier.padding(30.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable fun ServerIcon(text: String, onClick: () -> Unit) { Box(Modifier.size(48.dp).clip(CircleShape).background(Accent).clickable { onClick() }, contentAlignment = Alignment.Center) { Text(text, fontWeight = FontWeight.Bold, fontSize = 20.sp) } }

@Composable fun Avatar(uri: Uri?, size: Int) {
    if (uri == null) {
        Box(Modifier.size(size.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
            Text("0", fontWeight = FontWeight.Bold)
        }
    } else {
        AndroidView(
            factory = { ctx -> android.widget.ImageView(ctx).apply { scaleType = android.widget.ImageView.ScaleType.CENTER_CROP } },
            update = { it.setImageURI(uri) },
            modifier = Modifier.size(size.dp).clip(CircleShape)
        )
    }
}

@Composable fun TopBar(channel: String, onCall: () -> Unit, onCamera: () -> Unit, onShare: () -> Unit, onMembers: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Panel).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (channel == "DM") "Mensagens privadas" else "# $channel", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        IconButton(onCall) { Icon(Icons.Default.Call, "Voz") }; IconButton(onCamera) { Icon(Icons.Default.Videocam, "Câmera") }; IconButton(onShare) { Icon(Icons.Default.ScreenShare, "Compartilhar tela") }; IconButton(onMembers) { Icon(Icons.Default.People, "Membros") }
    }; HorizontalDivider(color = Panel2)
}

@Composable fun Chat(items: List<Msg>, text: String, onText: (String) -> Unit, send: () -> Unit, place: String) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(items) { m -> Row(verticalAlignment = Alignment.Top) { Box(Modifier.size(42.dp).clip(CircleShape).background(Accent)); Spacer(Modifier.width(10.dp)); Column { Text(m.author, fontWeight = FontWeight.Bold); Text(m.text, color = Color.LightGray) } } }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(text, onText, Modifier.weight(1f), placeholder = { Text("Mensagem em $place") }, singleLine = true)
            IconButton(send) { Icon(Icons.Default.Send, "Enviar") }
        }
    }
}

@Composable fun Profile(store: OlowStore, avatar: Uri?, pick: () -> Unit, nitro: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Meu perfil", fontSize = 28.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(24.dp)); Avatar(avatar, 110); Spacer(Modifier.height(12.dp)); Text(store.name, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("@${store.name.replace(" ", "_").lowercase()}", color = Color.Gray)
        Spacer(Modifier.height(22.dp)); Button(pick) { Icon(Icons.Default.Photo, null); Spacer(Modifier.width(8.dp)); Text("Trocar foto da galeria") }
        Spacer(Modifier.height(8.dp)); OutlinedButton({}) { Text("Trocar banner") }
        Spacer(Modifier.height(8.dp)); OutlinedButton(nitro) { Text(if (store.nitro) "0low Nitro ativo" else "✨ Comprar 0low Nitro") }
    }
}

@Composable fun Settings(store: OlowStore, profile: () -> Unit, logout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp)) { Text("Configurações", fontSize = 28.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(20.dp)); Text("Conta", color = Color.Gray); Spacer(Modifier.height(8.dp)); Button(profile) { Text("Abrir perfil") }; OutlinedButton({ toast(LocalContext.current, "Notificações locais ativadas") }) { Text("Notificações") }; OutlinedButton(logout) { Text("Sair da conta") } }
}

@Composable fun ServerAdmin(server: Server, channel: () -> Unit, roles: () -> Unit, boost: () -> Unit, nitro: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) { Text(server.name, fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("Painel do servidor", color = Color.Gray); Spacer(Modifier.height(20.dp)); Button(channel) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Criar canal") }; Button(roles) { Icon(Icons.Default.Shield, null); Spacer(Modifier.width(8.dp)); Text("Cargos e permissões") }; Button(boost) { Icon(Icons.Default.Bolt, null); Spacer(Modifier.width(8.dp)); Text("Impulsionar servidor") }; Button(nitro) { Text("0low Nitro") } }
}

@Composable fun DialogText(title: String, label: String, action: String, onOk: (String) -> Unit, onCancel: () -> Unit) { var v by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = onCancel, title = { Text(title) }, text = { OutlinedTextField(v, { v = it }, label = { Text(label) }) }, confirmButton = { Button({ onOk(v) }) { Text(action) } }, dismissButton = { TextButton(onCancel) { Text("Cancelar") } }) }

@Composable fun RoleDialog(onClose: () -> Unit) { AlertDialog(onDismissRequest = onClose, title = { Text("Cargos") }, text = { Column { listOf("Dono", "Administrador", "Moderador", "Membro").forEach { Text("• $it", Modifier.padding(6.dp)) }; Text("Permissões podem ser conectadas ao backend depois.", color = Color.Gray, fontSize = 12.sp) } }, confirmButton = { Button(onClose) { Text("OK") } }) }

@Composable fun NitroDialog(store: OlowStore, onBuy: () -> Unit) { AlertDialog(onDismissRequest = {}, title = { Text("✨ 0low Nitro") }, text = { Text("Perfil decorado, recursos premium e benefícios para servidores.\n\nNo protótipo, a compra é simulada e não cobra dinheiro.") }, confirmButton = { Button(onBuy) { Text("Ativar demo") } }, dismissButton = { TextButton({}) { Text("Agora não") } }) }

@Composable fun CallDialog(onClose: () -> Unit, onCamera: () -> Unit, onShare: () -> Unit) { AlertDialog(onDismissRequest = onClose, title = { Text("Chamada 0low") }, text = { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { Box(Modifier.size(100.dp).clip(CircleShape).background(Accent)); Spacer(Modifier.height(10.dp)); Text("0low User", fontWeight = FontWeight.Bold); Text("Conectado ao canal de voz", color = Color.Gray); Spacer(Modifier.height(14.dp)); Row { IconButton(onCamera) { Icon(Icons.Default.Videocam, "Câmera") }; IconButton(onShare) { Icon(Icons.Default.ScreenShare, "Tela") } } } }, confirmButton = { Button(onClose) { Text("Encerrar") } }) }

fun toast(context: Context, msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
