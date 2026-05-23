package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.OperatorViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: OperatorViewModel,
    onLoginSuccess: () -> Unit
) {
    val serverIpUrl by viewModel.serverIp.collectAsState()
    val estaOnline by viewModel.estaOnline.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.mensagemErro.collectAsState()
    val successMsg by viewModel.mensagemSucesso.collectAsState()

    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Decode current stored IP and Port
    var inputIp by remember { mutableStateOf("") }
    var inputPort by remember { mutableStateOf("3000") }

    // On initial load, try to extract the IP and Port from the saved full Base URL
    LaunchedEffect(serverIpUrl) {
        val cleanUrl = serverIpUrl.replace("http://", "").replace("https://", "")
        if (cleanUrl.contains(":")) {
            inputIp = cleanUrl.substringBefore(":")
            inputPort = cleanUrl.substringAfter(":")
        } else {
            inputIp = cleanUrl
            inputPort = "3000"
        }
    }

    var login by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfig by remember { mutableStateOf(false) }

    // Validation checks
    val hasValidIp = remember(inputIp) {
        val ipRegex = """^((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}$""".toRegex()
        val hostnameRegex = """^[a-zA-Z0-9.-]+$""".toRegex()
        inputIp.isNotEmpty() && (inputIp.matches(ipRegex) || inputIp.matches(hostnameRegex))
    }
    
    val hasValidPort = remember(inputPort) {
        val portInt = inputPort.toIntOrNull()
        portInt != null && portInt in 1..65535
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SlateDarkBackground,
                            SlateSurface
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .widthIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Brand Header with Modern Typography
                Icon(
                    imageVector = Icons.Default.TapAndPlay,
                    contentDescription = null,
                    tint = TealAccentLight,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(bottom = 8.dp)
                )

                Text(
                    text = "ChamaAí",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "OPERADOR TOUCH",
                    color = TealAccentLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // SnackBar/Alert styling for Success & Error Feedbacks
                AnimatedVisibility(visible = errorMsg != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CoralRed.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .border(1.dp, CoralRed, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = "Erro", tint = CoralRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMsg ?: "",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = successMsg != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .border(1.dp, EmeraldGreen, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Sucesso", tint = EmeraldGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = successMsg ?: "",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Main Form Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateBorder, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Connection Status Status Info Pill
                        val bannerColor = if (estaOnline) EmeraldGreen else CoralRed
                        val statusText = if (estaOnline) "Servidor: Online" else "Servidor: Offline"
                        val statusIcon = if (estaOnline) Icons.Default.CloudQueue else Icons.Default.CloudOff

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(bannerColor.copy(alpha = 0.15f))
                                .border(1.dp, bannerColor.copy(alpha = 0.5f), RoundedCornerShape(30.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = bannerColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusText,
                                color = bannerColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Credentials Section
                        Text(
                            text = "Acesso do Atendente",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = login,
                            onValueChange = { login = it },
                            label = { Text("Nome de Usuário (Login)") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TealAccentLight) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealAccentLight,
                                cursorColor = TealAccentLight,
                                focusedLabelColor = TealAccentLight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = senha,
                            onValueChange = { senha = it },
                            label = { Text("Senha Secreta") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TealAccentLight) },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Ver senha",
                                        tint = TealAccentLight
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealAccentLight,
                                cursorColor = TealAccentLight,
                                focusedLabelColor = TealAccentLight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Giant tactile sign-in button
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                viewModel.limparMensagens()
                                viewModel.realizarLogin(login, senha) {
                                    onLoginSuccess()
                                }
                            },
                            enabled = !isLoading && login.isNotEmpty() && senha.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TealAccent,
                                disabledContainerColor = SlateBorder
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = "ENTRAR NO PAINEL",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick action link to toggle IP custom settings
                        TextButton(
                            onClick = { showConfig = !showConfig },
                            colors = ButtonDefaults.textButtonColors(contentColor = TealAccentLight)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (showConfig) Icons.Default.SettingsBackupRestore else Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (showConfig) "Ocultar IP do Servidor" else "Configurar IP do Servidor",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Collapsible Server Configuration Module
                        AnimatedVisibility(
                            visible = showConfig,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SlateDarkBackground.copy(alpha = 0.5f))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Endereço do Servidor Master",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = inputIp,
                                        onValueChange = { inputIp = it },
                                        label = { Text("IP / Host") },
                                        placeholder = { Text("ex: 192.168.1.100") },
                                        singleLine = true,
                                        modifier = Modifier.weight(0.65f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = TealAccentLight,
                                            cursorColor = TealAccentLight
                                        )
                                    )

                                    OutlinedTextField(
                                        value = inputPort,
                                        onValueChange = { inputPort = it },
                                        label = { Text("Porta") },
                                        placeholder = { Text("3000") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(0.35f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = TealAccentLight,
                                            cursorColor = TealAccentLight
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        val constructedUrl = "http://${inputIp.trim()}:${inputPort.trim()}"
                                        viewModel.salvarIp(constructedUrl)
                                    },
                                    enabled = hasValidIp && hasValidPort,
                                    colors = ButtonDefaults.buttonColors(containerColor = SlateBorder),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("SALVAR CONEXÃO")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Fine-print/credits footer
                Text(
                    text = "ChamaAí Atendimento © 2026",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
