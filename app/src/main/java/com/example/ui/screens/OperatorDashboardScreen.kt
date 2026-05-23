package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

    val scope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()

    var showGuicheDialog by remember { mutableStateOf(false) }
    var inputGuiche by remember { mutableStateOf("") }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var settingsIpInput by remember { mutableStateOf("") }

    val serverIpConfig by viewModel.serverIp.collectAsState()

    // Synergize Toast success/failures inside elegant snackbar blocks
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Elegant Control Bar with Dual status and logout actions
            Column {
                Surface(
                    color = SlateSurface,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    // Operator Title block
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = TealAccentLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentOperator ?: "Atendente",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        // Active booth indicator with quick modification dialog
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
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                            Text(
                                text = guiche,
                                color = TealAccentLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar guiche",
                                tint = TealAccentLight,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    // Online indication Badge
                    val heartColor = if (estaOnline) EmeraldGreen else CoralRed
                    val connectionText = if (estaOnline) "Online" else "Offline"
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(heartColor.copy(alpha = 0.15f))
                            .border(1.dp, heartColor.copy(alpha = 0.4f), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 5.dp),
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
                            text = connectionText,
                            color = heartColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Settings cog button
                    IconButton(
                        onClick = {
                            settingsIpInput = serverIpConfig
                            showSettingsDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuração de Conexão", tint = Color.LightGray)
                    }

                    // Logout trigger
                    IconButton(
                        onClick = { showDisconnectDialog = true }
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Deslogar", tint = CoralRed)
                    }
                }
            }
            HorizontalDivider(color = SlateBorder, thickness = 1.dp)
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
                .background(SlateDarkBackground)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isTablet = maxWidth > 720.dp

                if (isTablet) {
                    // Split screen layout for tablets
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Pane: Password List Fila
                        Box(
                            modifier = Modifier
                                .weight(0.40f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(SlateSurface)
                                .border(1.dp, SlateBorder, RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            FilaListSection(fila = fila, onRefresh = { viewModel.carregarFila() })
                        }

                        // Right Pane: Big call actions
                        Column(
                            modifier = Modifier
                                .weight(0.60f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Ultimo chamado Display
                            ActiveTicketDisplayCard(
                                ultimoChamado = ultimoChamado,
                                guiche = guiche,
                                onRepetir = { viewModel.repetirChamada() },
                                onEstornar = { viewModel.estornarSenha() },
                                modifier = Modifier.weight(0.45f)
                            )

                            // Giant master tactile trigger
                            GiantCallButtonCard(
                                onClick = { viewModel.chamarProximo() },
                                enabled = estaOnline,
                                modifier = Modifier.weight(0.55f)
                            )
                        }
                    }
                } else {
                    // Portrait stack for mobile phones
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Display calling info
                        ActiveTicketDisplayCard(
                            ultimoChamado = ultimoChamado,
                            guiche = guiche,
                            onRepetir = { viewModel.repetirChamada() },
                            onEstornar = { viewModel.estornarSenha() },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Gigantic calling button
                        GiantCallButtonCard(
                            onClick = { viewModel.chamarProximo() },
                            enabled = estaOnline,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )

                        // Queue indicator list (with height constraint to scroll correctly inside column)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateSurface),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 250.dp, max = 500.dp)
                                .border(1.dp, SlateBorder, RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            FilaListSection(fila = fila, onRefresh = { viewModel.carregarFila() })
                        }
                    }
                }
            }
        }
    }

    // Modal Edit Guichê
    if (showGuicheDialog) {
        AlertDialog(
            onDismissRequest = { showGuicheDialog = false },
            title = { Text("Alterar Guichê ou Balcão", color = Color.White) },
            containerColor = SlateSurface,
            text = {
                OutlinedTextField(
                    value = inputGuiche,
                    onValueChange = { inputGuiche = it },
                    label = { Text("Nome/Número") },
                    placeholder = { Text("ex: Guichê 1, Mesa 2") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealAccentLight,
                        cursorColor = TealAccentLight
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
                    onClick = { showGuicheDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.LightGray)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal Edit IP from Settings Gear (Admin control)
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = TealAccentLight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajustes de Conexão", color = Color.White, fontSize = 18.sp)
                }
            },
            containerColor = SlateSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Aponta o IP e Porta do Servidor ChamaAí Master Local:",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = settingsIpInput,
                        onValueChange = { settingsIpInput = it },
                        label = { Text("Endereço Completo do Servidor") },
                        placeholder = { Text("http://192.168.1.100:3000") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccentLight,
                            cursorColor = TealAccentLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (settingsIpInput.trim().isNotEmpty()) {
                            viewModel.salvarIp(settingsIpInput.trim())
                            showSettingsDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                ) {
                    Text("Salvar IP")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSettingsDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.LightGray)
                ) {
                    Text("Fechar")
                }
            }
        )
    }

    // Modal Logout Confirmation
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("Deseja sair?", color = Color.White) },
            containerColor = SlateSurface,
            text = { Text("Isso fechará a sua sessão e redefinirá os controles ativos.", color = Color.White.copy(alpha = 0.8f)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDisconnectDialog = false
                        viewModel.realizarLogout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Desconectar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDisconnectDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.LightGray)
                ) {
                    Text("Permanecer")
                }
            }
        )
    }
}

// Left side layout helper widget: FilaListSection
@Composable
fun FilaListSection(
    fila: List<FilaResponse>,
    onRefresh: () -> Unit
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
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "${fila.size} senhas aguardando",
                    color = TealAccentLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SlateBorder)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Recarregar fila", tint = Color.White)
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nenhuma senha na fila",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(fila, key = { it.id }) { item ->
                    FilaItemCard(item = item)
                }
            }
        }
    }
}

// Queue cell card formatting: distinguishing between preferential/normal
@Composable
fun FilaItemCard(item: FilaResponse) {
    val brandBorder = if (item.prioridade) AmberOrange else TealAccent
    val tagBg = if (item.prioridade) AmberOrange.copy(alpha = 0.15f) else TealAccent.copy(alpha = 0.15f)
    val tagText = if (item.prioridade) "PREFERENCIAL" else "CONVENCIONAL"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SlateDarkBackground)
            .border(1.dp, brandBorder.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = item.senha,
                color = Color.White,
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
    guiche: String,
    onRepetir: () -> Unit,
    onEstornar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .border(1.dp, SlateBorder, RoundedCornerShape(20.dp))
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
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (ultimoChamado != null) {
                // Large styled ticket text
                Text(
                    text = ultimoChamado.senha,
                    color = if (ultimoChamado.prioridade) AmberOrange else TealAccentLight,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(SlateDarkBackground)
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (ultimoChamado.prioridade) Icons.Default.Star else Icons.Default.EventSeat,
                        contentDescription = null,
                        tint = if (ultimoChamado.prioridade) AmberOrange else TealAccentLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (ultimoChamado.prioridade) "Preferencial" else "Convencional",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tactical auxiliary control buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Repetir Chamada
                    Button(
                        onClick = onRepetir,
                        colors = ButtonDefaults.buttonColors(containerColor = SlateBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Repetir", fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Icon(
                    imageVector = Icons.Default.SentimentNeutral,
                    contentDescription = null,
                    tint = SlateBorder,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Aguardando chamada",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Toque em 'Chamar Próximo'",
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Center/Main call giant touch button
@Composable
fun GiantCallButtonCard(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) TealAccent else SlateBorder
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = modifier
            .clickable(enabled = enabled) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) {
                        Brush.verticalGradient(
                            colors = listOf(
                                TealAccentLight,
                                TealAccent
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                SlateBorder,
                                SlateBorder
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
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = if (enabled) SlateDarkBackground else Color.Gray,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "CHAMAR PRÓXIMO",
                    color = if (enabled) SlateDarkBackground else Color.Gray,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chama fila e atualiza painel principal",
                    color = if (enabled) SlateDarkBackground.copy(alpha = 0.7f) else Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
