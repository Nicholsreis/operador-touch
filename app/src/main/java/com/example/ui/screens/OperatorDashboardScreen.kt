package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FilaResponse
import com.example.ui.theme.*
import com.example.ui.viewmodel.OperatorViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorDashboardScreen(
    viewModel: OperatorViewModel,
    onLogout: () -> Unit
) {
    val currentOperator by viewModel.operadorNome.collectAsState()
    val guiche by viewModel.guicheAtivo.collectAsState()
    val estaOnline by viewModel.estaOnline.collectAsState()
    val fila by viewModel.fila.collectAsState()
    val ultimoChamado by viewModel.ultimoChamado.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.mensagemErro.collectAsState()
    val successMsg by viewModel.mensagemSucesso.collectAsState()

    // Premium states
    val isPausado by viewModel.pausaEnabled.collectAsState()
    val audioEnabled by viewModel.audioEnabled.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()

    val estornandoSenha by viewModel.estornandoSenha.collectAsState()
    val estornoCountdown by viewModel.estornoCountdown.collectAsState()

    val scope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()

    var showGuicheDialog by remember { mutableStateOf(false) }
    var inputGuiche by remember { mutableStateOf("") }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var settingsIpInput by remember { mutableStateOf("") }

    val serverIpConfig by viewModel.serverIp.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(errorMsg, successMsg) {
        errorMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limparMensagens()
        }
        successMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limparMensagens()
        }
    }

    // Adapt layout palettes according to material system state (Light or Dark)
    val isDark = MaterialTheme.colorScheme.background == SlateDarkBackground
    val dynamicSurface = if (isDark) SlateSurface else Color.White
    val dynamicBackground = if (isDark) SlateDarkBackground else Color(0xFFF1F5F9)
    val dynamicBorder = if (isDark) SlateBorder else Color(0xFFE2E8F0)
    
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF475569)
    val textSecondaryMuted = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF64748B)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = dynamicBackground,
        topBar = {
            Column {
                Surface(
                    color = dynamicSurface,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Operator Info
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = if (isDark) TealAccentLight else TealAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentOperator ?: "Atendente",
                                    color = textPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // Active booth clickable modifier
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable {
                                        inputGuiche = guiche
                                        showGuicheDialog = true
                                    }
                            ) {
                                Text(
                                    text = "Ativo no: ",
                                    color = textSecondaryMuted,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = guiche,
                                    color = if (isDark) TealAccentLight else TealAccent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Alterar guichê",
                                    tint = if (isDark) TealAccentLight else TealAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // Pause status selector
                        Row(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.togglePausa() }
                                .background(
                                    if (isPausado) AmberOrange.copy(alpha = 0.2f)
                                    else Color.Gray.copy(alpha = 0.1f)
                                )
                                .border(
                                    1.dp,
                                    if (isPausado) AmberOrange else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPausado) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPausado) "Retomar" else "Pausar",
                                tint = if (isPausado) AmberOrange else textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPausado) "PAUSADO" else "PAUSAR",
                                color = if (isPausado) AmberOrange else textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Connection State indicator
                        val heartColor = if (estaOnline) EmeraldGreen else CoralRed
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(heartColor.copy(alpha = 0.15f))
                                .border(1.dp, heartColor.copy(alpha = 0.4f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(heartColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (estaOnline) "Online" else "Offline",
                                color = heartColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Settings Gear Dialog
                        IconButton(
                            onClick = {
                                settingsIpInput = serverIpConfig
                                showSettingsDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = textSecondary)
                        }

                        // Logout trigger
                        IconButton(
                            onClick = { showDisconnectDialog = true }
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = "Sair", tint = CoralRed)
                        }
                    }
                }
                HorizontalDivider(color = dynamicBorder, thickness = 1.dp)
            }
        }
    ) { padding ->
        // Pull-to-refresh structure
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.carregarFila() },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(dynamicBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Interactive Undoing progress bar above calling controllers
                AnimatedVisibility(
                    visible = estornandoSenha != null,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    estornandoSenha?.let { ticket ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) SlateSurface else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .border(2.dp, CoralRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SettingsBackupRestore,
                                        contentDescription = null,
                                        tint = CoralRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Estornando Senha ${ticket.senha}...",
                                            color = textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Será devolvida à fila em ${estornoCountdown}s",
                                            color = textSecondary,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { estornoCountdown / 5.0f },
                                            color = CoralRed,
                                            trackColor = CoralRed.copy(alpha = 0.2f),
                                            modifier = Modifier
                                                .fillMaxWidth(0.8f)
                                                .height(4.dp)
                                                .clip(CircleShape)
                                        )
                                    }
                                }
                                
                                Button(
                                    onClick = { viewModel.desfazerEstorno() },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Undo, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("DESFAZER", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Pause Mode prominent banner
                AnimatedVisibility(
                    visible = isPausado,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AmberOrange.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Pause, contentDescription = null, tint = AmberOrange)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Guichê em pausa temporária. O painel não aceitará chamadas.",
                                        color = if (isDark) Color.White else Color(0xFF78350F),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                TextButton(
                                    onClick = { viewModel.togglePausa() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = AmberOrange)
                                ) {
                                    Text("RETOMAR", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                }
                            }
                            HorizontalDivider(color = AmberOrange.copy(alpha = 0.3f), thickness = 1.dp)
                        }
                    }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isLandscape = maxWidth > maxHeight

                    if (isLandscape) {
                        // Clone perfeito da tela "ControleTouch.tsx" do ChamaAí em horizontal
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Lado Esquerdo - Painel de Atendimento e Estatísticas (50% de largura)
                            Box(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .fillMaxHeight()
                            ) {
                                ActiveTicketDisplayCard(
                                    ultimoChamado = ultimoChamado,
                                    fila = fila,
                                    guiche = guiche,
                                    onRepetir = { viewModel.repetirChamada() },
                                    onEstornar = { viewModel.iniciarEstorno() },
                                    modifier = Modifier.fillMaxSize(),
                                    isDark = isDark,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    dynamicSurface = dynamicSurface,
                                    dynamicBorder = dynamicBorder,
                                    dynamicBackground = dynamicBackground
                                )
                            }

                            // Lado Direito - Botoeira Gigante de Ação (50% de largura)
                            Column(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 1. Botão PRÓXIMO (Emerald/Verde - Ocupa 50% de altura)
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (estaOnline && !isPausado) Color(0xFF059669) else Color(0xFF9CA3AF)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = if (estaOnline && !isPausado) 6.dp else 0.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.50f)
                                        .clickable(enabled = estaOnline && !isPausado) { viewModel.chamarProximo() }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(16.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Campaign,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "PRÓXIMO",
                                            color = Color.White,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 2.sp
                                        )
                                    }
                                }

                                // 2. Botão REPETIR (Azul - Ocupa 30% de altura)
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (estaOnline && !isPausado && ultimoChamado != null) Color(0xFF2563EB) else Color(0xFF94A3B8)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = if (estaOnline && !isPausado && ultimoChamado != null) 4.dp else 0.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.30f)
                                        .clickable(enabled = estaOnline && !isPausado && ultimoChamado != null) { viewModel.repetirChamada() }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(30.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "REPETIR",
                                            color = Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.5.sp
                                        )
                                    }
                                }

                                // 3. Botão DEVOLVER À FILA (Branco / Amber - Ocupa 20% de altura)
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDark) SlateSurface else Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.20f)
                                        .border(
                                            2.dp,
                                            if (ultimoChamado != null) Color(0xFFD97706) else Color(0xFFE2E8F0),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable(enabled = ultimoChamado != null) { viewModel.iniciarEstorno() }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Undo,
                                            contentDescription = null,
                                            tint = if (ultimoChamado != null) Color(0xFFD97706) else Color(0xFF94A3B8),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "DEVOLVER À FILA",
                                            color = if (ultimoChamado != null) Color(0xFFD97706) else Color(0xFF94A3B8),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Portrait stack
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ActiveTicketDisplayCard(
                                ultimoChamado = ultimoChamado,
                                fila = fila,
                                guiche = guiche,
                                onRepetir = { viewModel.repetirChamada() },
                                onEstornar = { viewModel.iniciarEstorno() },
                                modifier = Modifier.fillMaxWidth(),
                                isDark = isDark,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                dynamicSurface = dynamicSurface,
                                dynamicBorder = dynamicBorder,
                                dynamicBackground = dynamicBackground
                            )

                            GiantCallButtonCard(
                                onClick = { viewModel.chamarProximo() },
                                enabled = estaOnline && !isPausado,
                                isPausado = isPausado,
                                isDark = isDark,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            )

                            Card(
                                colors = CardDefaults.cardColors(containerColor = dynamicSurface),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 280.dp, max = 550.dp)
                                    .border(1.dp, dynamicBorder, RoundedCornerShape(20.dp))
                                    .padding(16.dp)
                            ) {
                                FilaListSection(
                                    fila = fila,
                                    onRefresh = { viewModel.carregarFila() },
                                    isDark = isDark,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    dynamicBorder = dynamicBorder,
                                    dynamicBackground = dynamicBackground
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Settings Modal
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = if (isDark) TealAccentLight else TealAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configurações do Guichê", color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = dynamicSurface,
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Customize áudio, retorno tátil de tablet e temas visuais do aplicativo.",
                        color = textSecondary,
                        fontSize = 13.sp
                    )
                    
                    // Theme Row Switcher
                    Column {
                        Text("Tema do Aplicativo", color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("system" to "Padrão", "light" to "Claro", "dark" to "Escuro").forEach { (id, label) ->
                                val selected = appTheme == id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) (if (isDark) TealAccentLight.copy(alpha = 0.2f) else TealAccent.copy(alpha = 0.15f))
                                            else (if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f))
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) (if (isDark) TealAccentLight else TealAccent) else dynamicBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.setAppTheme(id) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) (if (isDark) TealAccentLight else TealAccent) else textSecondary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = dynamicBorder)

                    // Audio Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sinal Sonoro", color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Tocar alerta sonoro curto a cada nova senha chamada", color = textSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = audioEnabled,
                            onCheckedChange = { viewModel.setAudioEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = TealAccent, checkedTrackColor = TealAccent.copy(alpha = 0.4f))
                        )
                    }

                    // Haptic Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Feedback Háptico (Vibrar)", color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Vibrações rápidas em toques do operador", color = textSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = hapticEnabled,
                            onCheckedChange = { viewModel.setHapticEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = TealAccent, checkedTrackColor = TealAccent.copy(alpha = 0.4f))
                        )
                    }

                    HorizontalDivider(color = dynamicBorder)

                    // Server IP configuration
                    Column {
                        Text("Servidor ChamaAí Master", color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = settingsIpInput,
                            onValueChange = { settingsIpInput = it },
                            label = { Text("IP do Servidor") },
                            placeholder = { Text("http://192.168.1.100:3000") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = if (isDark) TealAccentLight else TealAccent,
                                cursorColor = if (isDark) TealAccentLight else TealAccent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (settingsIpInput.trim().isNotEmpty()) {
                            viewModel.salvarIp(settingsIpInput.trim())
                        }
                        showSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                ) {
                    Text("Concluir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSettingsDialog = false }
                ) {
                    Text("Fechar", color = textSecondary)
                }
            }
        )
    }

    // Change Guiche Dialog
    if (showGuicheDialog) {
        AlertDialog(
            onDismissRequest = { showGuicheDialog = false },
            title = { Text("Alterar Guichê ou Mesa", color = textPrimary, fontWeight = FontWeight.Bold) },
            containerColor = dynamicSurface,
            text = {
                OutlinedTextField(
                    value = inputGuiche,
                    onValueChange = { inputGuiche = it },
                    label = { Text("Identificador") },
                    placeholder = { Text("ex: Guichê 1, Mesa 2") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = if (isDark) TealAccentLight else TealAccent,
                        cursorColor = if (isDark) TealAccentLight else TealAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputGuiche.trim().isNotEmpty()) {
                            viewModel.salvarGuiche(inputGuiche.trim())
                            showGuicheDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGuicheDialog = false }
                ) {
                    Text("Cancelar", color = textSecondary)
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("Sair do Sistema?", color = textPrimary, fontWeight = FontWeight.Bold) },
            containerColor = dynamicSurface,
            text = { Text("Esta operação encerrará seu painel ativo no $guiche.", color = textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDisconnectDialog = false
                        viewModel.realizarLogout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Sair")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDisconnectDialog = false }
                ) {
                    Text("Permanecer", color = textSecondary)
                }
            }
        )
    }
}

// Fila List Section with custom Empty Queue visual asset
@Composable
fun FilaListSection(
    fila: List<FilaResponse>,
    onRefresh: () -> Unit,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    dynamicBorder: Color,
    dynamicBackground: Color
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Fila de Espera",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "${fila.size} senhas aguardando",
                    color = if (isDark) TealAccentLight else TealAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isDark) SlateBorder else Color(0xFFE2E8F0))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Recarregar fila", tint = textPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (fila.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EventSeat,
                        contentDescription = "Fila Vazia",
                        tint = if (isDark) SlateBorder else Color(0xFFCBD5E1),
                        modifier = Modifier.size(68.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Fila tranquila por enquanto",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Aproveite o momento ou declare intervalo caso necessário.",
                        color = textSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(fila, key = { it.id }) { item ->
                    FilaItemCard(
                        item = item,
                        isDark = isDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        dynamicBackground = dynamicBackground
                    )
                }
            }
        }
    }
}

// Queue cell card formatting: distinguishing between preferential/normal
@Composable
fun FilaItemCard(
    item: FilaResponse,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    dynamicBackground: Color
) {
    val brandBorder = if (item.prioridade) AmberOrange else TealAccent
    val tagBg = if (item.prioridade) AmberOrange.copy(alpha = 0.15f) else TealAccent.copy(alpha = 0.15f)
    val tagText = if (item.prioridade) "PREFERENCIAL" else "CONVENCIONAL"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(dynamicBackground)
            .border(1.dp, brandBorder.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = item.senha,
                color = textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(tagBg)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = tagText,
                    color = brandBorder,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Icon(
            imageVector = if (item.prioridade) Icons.Default.Star else Icons.Default.ArrowForwardIos,
            contentDescription = null,
            tint = brandBorder,
            modifier = Modifier.size(20.dp)
        )
    }
}

// Upper display details of the called card
@Composable
fun ActiveTicketDisplayCard(
    ultimoChamado: FilaResponse?,
    fila: List<FilaResponse>,
    guiche: String,
    onRepetir: () -> Unit,
    onEstornar: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    dynamicSurface: Color,
    dynamicBorder: Color,
    dynamicBackground: Color
) {
    val filaGeralCount = fila.count { !it.prioridade }
    val filaPrioritariaCount = fila.count { it.prioridade }
    val filaTotalCount = fila.size

    Card(
        colors = CardDefaults.cardColors(containerColor = dynamicSurface),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.border(1.dp, dynamicBorder, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SENHA EM ATENDIMENTO",
                color = textSecondary.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (ultimoChamado != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Large styled ticket text
                        Text(
                            text = ultimoChamado.senha,
                            color = if (ultimoChamado.prioridade) AmberOrange else (if (isDark) TealAccentLight else TealAccent),
                            fontSize = 54.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(dynamicBackground)
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (ultimoChamado.prioridade) Icons.Default.Star else Icons.Default.EventSeat,
                                contentDescription = null,
                                tint = if (ultimoChamado.prioridade) AmberOrange else (if (isDark) TealAccentLight else TealAccent),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (ultimoChamado.prioridade) "Preferencial" else "Convencional",
                                color = textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Control buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Repetir Chamada
                            Button(
                                onClick = onRepetir,
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) SlateBorder else Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = textPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Repetir", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            // Estornar
                            Button(
                                onClick = onEstornar,
                                colors = ButtonDefaults.buttonColors(containerColor = CoralRed.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .border(1.dp, CoralRed, RoundedCornerShape(12.dp))
                            ) {
                                Icon(Icons.Default.Undo, contentDescription = null, tint = CoralRed)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Estornar", color = CoralRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "---",
                            color = if (isDark) SlateBorder else Color(0xFFCBD5E1),
                            fontSize = 54.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(dynamicBackground)
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AGUARDANDO",
                                color = textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Counters layout - GERAL, PRIORITÁRIO, TOTAL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Geral
                Card(
                    colors = CardDefaults.cardColors(containerColor = dynamicBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).border(1.dp, dynamicBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$filaGeralCount",
                            color = Color(0xFF2563EB), // Blue
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "GERAL",
                            color = textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Prioritário
                Card(
                    colors = CardDefaults.cardColors(containerColor = dynamicBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).border(1.dp, dynamicBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$filaPrioritariaCount",
                            color = Color(0xFFD97706), // Amber
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "PRIORITÁRIO",
                            color = textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Total
                Card(
                    colors = CardDefaults.cardColors(containerColor = dynamicBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).border(1.dp, dynamicBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$filaTotalCount",
                            color = textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "TOTAL",
                            color = textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// Center/Main call giant touch button
@Composable
fun GiantCallButtonCard(
    onClick: () -> Unit,
    enabled: Boolean,
    isPausado: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) TealAccent else (if (isDark) SlateBorder else Color(0xFFE2E8F0))
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 8.dp else 0.dp),
        modifier = modifier
            .clickable(enabled = enabled) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isPausado) {
                        Brush.verticalGradient(
                            colors = listOf(
                                AmberOrange.copy(alpha = 0.35f),
                                AmberOrange.copy(alpha = 0.15f)
                            )
                        )
                    } else if (enabled) {
                        Brush.verticalGradient(
                            colors = listOf(
                                TealAccent,
                                Color(0xFF0F766E) // deeper teal
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                (if (isDark) SlateBorder else Color(0xFFE2E8F0)),
                                (if (isDark) SlateBorder else Color(0xFFE2E8F0))
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = if (isPausado) Icons.Default.Pause else Icons.Default.Campaign,
                    contentDescription = null,
                    tint = if (isPausado) AmberOrange else if (enabled) Color.White else Color.Gray,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isPausado) "GUICHÊ PAUSADO" else "CHAMAR PRÓXIMO",
                    color = if (isPausado) AmberOrange else if (enabled) Color.White else Color.Gray,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isPausado) "Saia da pausa para continuar chamando senhas" else "Chama fila e atualiza painel principal",
                    color = if (isPausado) AmberOrange.copy(alpha = 0.8f) else if (enabled) Color.White.copy(alpha = 0.8f) else Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}