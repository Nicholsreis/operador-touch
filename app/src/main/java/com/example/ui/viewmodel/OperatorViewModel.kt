package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.NetworkClient
import com.example.data.model.ChamadaRequest
import com.example.data.model.FilaResponse
import com.example.data.model.LoginRequest
import com.example.data.model.SenhaEstornoRequest
import com.example.data.prefs.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OperatorViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application.applicationContext)
    private val apiService = NetworkClient.apiService

    val serverIp = sessionManager.serverIp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "http://192.168.1.100:3000")
    val authToken = sessionManager.authToken.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val operadorId = sessionManager.operadorId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val operadorNome = sessionManager.operadorNome.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val guicheAtivo = sessionManager.guicheAtivo.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Guichê 1")

    // Premium Settings state
    val appTheme = sessionManager.appTheme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")
    val audioEnabled = sessionManager.audioEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val hapticEnabled = sessionManager.hapticEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val pausaEnabled = sessionManager.pausaEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _fila = MutableStateFlow<List<FilaResponse>>(emptyList())
    val fila = _fila.asStateFlow()

    private val _estaOnline = MutableStateFlow(false)
    val estaOnline = _estaOnline.asStateFlow()

    private val _ultimoChamado = MutableStateFlow<FilaResponse?>(null)
    val ultimoChamado = _ultimoChamado.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _mensagemErro = MutableStateFlow<String?>(null)
    val mensagemErro = _mensagemErro.asStateFlow()

    private val _mensagemSucesso = MutableStateFlow<String?>(null)
    val mensagemSucesso = _mensagemSucesso.asStateFlow()

    // Undo Timer for Estorno structure
    private val _estornandoSenha = MutableStateFlow<FilaResponse?>(null)
    val estornandoSenha = _estornandoSenha.asStateFlow()

    private val _estornoCountdown = MutableStateFlow(0)
    val estornoCountdown = _estornoCountdown.asStateFlow()

    private var estornoJob: Job? = null
    private var pingJob: Job? = null
    private var filaJob: Job? = null

    init {
        viewModelScope.launch {
            serverIp.collect { ip ->
                NetworkClient.dynamicUrlInterceptor.setBaseUrl(ip)
                testarConexao()
            }
        }
        iniciarMonitoramento()
    }

    fun limparMensagens() {
        _mensagemErro.value = null
        _mensagemSucesso.value = null
    }

    fun salvarIp(ip: String) {
        viewModelScope.launch {
            val rawIp = ip.trim()
            if (rawIp.isNotEmpty()) {
                sessionManager.saveServerIp(rawIp)
                _mensagemSucesso.value = "IP do servidor atualizado!"
            }
        }
    }

    fun salvarGuiche(guiche: String) {
        viewModelScope.launch {
            sessionManager.saveGuiche(guiche)
            _mensagemSucesso.value = "Guichê alterado para $guiche"
        }
    }

    // Settings modifiers
    fun setAppTheme(theme: String) {
        viewModelScope.launch {
            sessionManager.saveAppTheme(theme)
        }
    }

    fun setAudioEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.saveAudioEnabled(enabled)
            _mensagemSucesso.value = if (enabled) "Alerta sonoro ativado!" else "Alerta sonoro silenciado."
        }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.saveHapticEnabled(enabled)
            _mensagemSucesso.value = if (enabled) "Vibração tátil ativada!" else "Vibração tátil desativada."
            if (enabled) {
                triggerHapticFeedback("single")
            }
        }
    }

    fun togglePausa() {
        viewModelScope.launch {
            val novoEstado = !pausaEnabled.value
            sessionManager.savePausaEnabled(novoEstado)
            _mensagemSucesso.value = if (novoEstado) "Guichê em pausa temporária" else "Guichê online pronto"
            triggerHapticFeedback("single")
        }
    }

    private fun iniciarMonitoramento() {
        pingJob?.cancel()
        filaJob?.cancel()

        pingJob = viewModelScope.launch {
            while (true) {
                testarConexao()
                delay(5000)
            }
        }

        filaJob = viewModelScope.launch {
            while (true) {
                val token = authToken.value
                if (token != null) {
                    atualizarFilaSilenciosamente(token)
                }
                delay(3000)
            }
        }
    }

    fun testarConexao() {
        viewModelScope.launch {
            try {
                NetworkClient.dynamicUrlInterceptor.setBaseUrl(serverIp.value)
                val token = authToken.value
                if (token != null) {
                    apiService.getFila("Bearer $token")
                } else {
                    apiService.getStatus()
                }
                _estaOnline.value = true
            } catch (e: Exception) {
                if (e is retrofit2.HttpException) {
                    _estaOnline.value = true
                } else {
                    _estaOnline.value = false
                }
            }
        }
    }

    fun realizarLogin(login: String, senha: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _mensagemErro.value = null
            try {
                NetworkClient.dynamicUrlInterceptor.setBaseUrl(serverIp.value)
                val response = apiService.login(LoginRequest(login, senha))
                sessionManager.saveSession(
                    token = response.token,
                    id = response.user.id,
                    nome = response.user.nome
                )
                _estaOnline.value = true
                _mensagemSucesso.value = "Seja bem-vindo, ${response.user.nome}!"
                onSuccess()
            } catch (e: Exception) {
                _mensagemErro.value = "Falha ao autenticar: ${e.localizedMessage ?: "Verifique sua conexão e IP do servidor."}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun carregarFila() {
        val token = authToken.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _mensagemErro.value = null
            try {
                val lista = apiService.getFila("Bearer $token")
                _fila.value = lista
                _estaOnline.value = true
                updateFilaNotification(lista.size)
            } catch (e: Exception) {
                _mensagemErro.value = "Erro ao carregar fila: ${e.localizedMessage}"
                _estaOnline.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun atualizarFilaSilenciosamente(token: String) {
        try {
            val lista = apiService.getFila("Bearer $token")
            _fila.value = lista
            _estaOnline.value = true
            updateFilaNotification(lista.size)
        } catch (e: Exception) {
            _estaOnline.value = false
        }
    }

    fun chamarProximo() {
        if (pausaEnabled.value) {
            _mensagemErro.value = "Guichê está pausado. Retome o atendimento primeiro!"
            return
        }

        val token = authToken.value ?: return
        val idLogado = operadorId.value ?: return
        val guiche = guicheAtivo.value

        viewModelScope.launch {
            _isLoading.value = true
            _mensagemErro.value = null
            try {
                val payload = mapOf<String, Any>(
                    "operador_id" to idLogado,
                    "guiche" to guiche
                )
                val senhaChamada = apiService.chamarProxima("Bearer $token", payload)
                _ultimoChamado.value = senhaChamada
                _mensagemSucesso.value = "Senha ${senhaChamada.senha} chamada no $guiche!"
                
                triggerHapticFeedback("single")
                triggerAudioChime()
                
                carregarFila()
            } catch (e: Exception) {
                _mensagemErro.value = "Fila vazia ou erro: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun repetirChamada() {
        if (pausaEnabled.value) {
            _mensagemErro.value = "Guichê está pausado. Retome o atendimento primeiro!"
            return
        }

        val token = authToken.value ?: return
        val idLogado = operadorId.value ?: return
        val guiche = guicheAtivo.value
        val ultimaSenha = _ultimoChamado.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _mensagemErro.value = null
            try {
                val request = ChamadaRequest(
                    senha_id = ultimaSenha.id,
                    operador_id = idLogado,
                    guiche = guiche
                )
                val response = apiService.repetirChamada("Bearer $token", request)
                _mensagemSucesso.value = "Chamada repetida para Senha ${response.senha}!"
                triggerHapticFeedback("single")
                triggerAudioChime()
            } catch (e: Exception) {
                _mensagemErro.value = "Erro ao repetir chamada: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Estorno workflow with delayed timer
    fun iniciarEstorno() {
        val ticket = _ultimoChamado.value ?: return
        estornoJob?.cancel()
        _estornandoSenha.value = ticket
        _estornoCountdown.value = 5
        
        // Optimistically hide active called ticket
        _ultimoChamado.value = null
        
        triggerHapticFeedback("double")
        
        estornoJob = viewModelScope.launch {
            while (_estornoCountdown.value > 0) {
                delay(1000)
                _estornoCountdown.value -= 1
            }
            commitEstorno(ticket)
        }
    }

    fun desfazerEstorno() {
        estornoJob?.cancel()
        val ticket = _estornandoSenha.value
        if (ticket != null) {
            _ultimoChamado.value = ticket
            _estornandoSenha.value = null
            _estornoCountdown.value = 0
            _mensagemSucesso.value = "Estorno cancelado e retornado à mesa!"
            triggerHapticFeedback("single")
        }
    }

    private fun commitEstorno(ticket: FilaResponse) {
        val token = authToken.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _mensagemErro.value = null
            try {
                val request = SenhaEstornoRequest(senha_id = ticket.id)
                val response = apiService.estornarSenha("Bearer $token", request)
                _estornandoSenha.value = null
                _mensagemSucesso.value = "Senha ${response.senha} devolvida à fila!"
                carregarFila()
            } catch (e: Exception) {
                // Restore if failed
                _ultimoChamado.value = ticket
                _estornandoSenha.value = null
                _mensagemErro.value = "Erro ao estornar senha: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun realizarLogout() {
        viewModelScope.launch {
            sessionManager.clearSession()
            _ultimoChamado.value = null
            _fila.value = emptyList()
            removerFilaNotification()
        }
    }

    // Interactive Notification management
    private fun updateFilaNotification(count: Int) {
        val context = getApplication<Application>().applicationContext
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return
        
        val channelId = "chamaai_operador"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "ChamaAí Fila",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mostra o número total da fila de atendimento em segundo plano"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val text = if (count > 0) "🎫 Fila: $count aguardando" else "Fila tranquila por enquanto"
        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setContentTitle("ChamaAí Operador")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)

        notificationManager.notify(1105, builder.build())
    }

    fun removerFilaNotification() {
        val context = getApplication<Application>().applicationContext
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return
        notificationManager.cancel(1105)
    }

    // Physical triggers
    fun triggerHapticFeedback(patternType: String) {
        if (!hapticEnabled.value) return
        val vibrator = getApplication<Application>().getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator ?: return
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (patternType == "double") {
                    val pattern = longArrayOf(0, 80, 100, 80)
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, -1))
                } else {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } else {
                @Suppress("DEPRECATION")
                if (patternType == "double") {
                    vibrator.vibrate(longArrayOf(0, 80, 100, 80), -1)
                } else {
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun triggerAudioChime() {
        if (!audioEnabled.value) return
        try {
            val notificationUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = android.media.RingtoneManager.getRingtone(getApplication(), notificationUri)
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        pingJob?.cancel()
        filaJob?.cancel()
        estornoJob?.cancel()
    }
}