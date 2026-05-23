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
    val id: Int,
    val senha: String,
    val prioridade: Boolean,
    val status: String
)

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

