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
            var rawIp = ip.trim()
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
                if (token != null && estaOnline.value) {
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
                apiService.getStatus()
                _estaOnline.value = true
            } catch (e: Exception) {
                _estaOnline.value = false
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
        } catch (e: Exception) {
            _estaOnline.value = false
        }
    }

    fun chamarProximo() {
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
                carregarFila()
            } catch (e: Exception) {
                _mensagemErro.value = "Fila vazia ou erro: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun repetirChamada() {
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
            } catch (e: Exception) {
                _mensagemErro.value = "Erro ao repetir chamada: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun estornarSenha() {
        val token = authToken.value ?: return
        val ultimaSenha = _ultimoChamado.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _mensagemErro.value = null
            try {
                val request = SenhaEstornoRequest(senha_id = ultimaSenha.id)
                val response = apiService.estornarSenha("Bearer $token", request)
                _ultimoChamado.value = null
                _mensagemSucesso.value = "Senha ${response.senha} estornada com sucesso!"
                carregarFila()
            } catch (e: Exception) {
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
        }
    }

    override fun onCleared() {
        super.onCleared()
        pingJob?.cancel()
        filaJob?.cancel()
    }
}
