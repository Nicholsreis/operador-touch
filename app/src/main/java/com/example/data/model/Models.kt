package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(val login: String, val senha: String)

@JsonClass(generateAdapter = true)
data class LoginResponse(val token: String, val user: User)

@JsonClass(generateAdapter = true)
data class User(val id: Int, val nome: String, val perfil: String)

@JsonClass(generateAdapter = true)
data class FilaResponse(
    @com.squareup.moshi.Json(name = "id") val rawId: Int? = null,
    @com.squareup.moshi.Json(name = "senha") val rawSenha: String? = null,
    val numero: Any? = null,
    @com.squareup.moshi.Json(name = "prioridade") val rawPrioridade: Boolean? = null,
    val preferencial: Any? = null,
    @com.squareup.moshi.Json(name = "status") val rawStatus: String? = null,
    val success: Boolean? = null,
    val data: FilaResponse? = null
) {
    val id: Int
        get() = rawId ?: data?.id ?: 0

    val senha: String
        get() = rawSenha ?: numero?.toString() ?: data?.senha ?: rawId?.toString() ?: ""

    val prioridade: Boolean
        get() = rawPrioridade == true || data?.prioridade == true || run {
            val prefStr = preferencial?.toString() ?: data?.preferencial?.toString() ?: ""
            prefStr == "1" || prefStr == "1.0" || prefStr.lowercase() == "true"
        }

    val status: String
        get() = rawStatus ?: data?.status ?: ""
}

@JsonClass(generateAdapter = true)
data class ChamadaRequest(
    val senha_id: Int,
    val operador_id: Int,
    val guiche: String
)

@JsonClass(generateAdapter = true)
data class SenhaEstornoRequest(val senha_id: Int)

@JsonClass(generateAdapter = true)
data class StatusResponse(val status: String? = null, val online: Boolean? = null)

