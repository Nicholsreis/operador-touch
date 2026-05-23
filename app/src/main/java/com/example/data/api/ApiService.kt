package com.example.data.api

import com.example.data.model.LoginRequest
import com.example.data.model.LoginResponse
import com.example.data.model.FilaResponse
import com.example.data.model.ChamadaRequest
import com.example.data.model.SenhaEstornoRequest
import com.example.data.model.StatusResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header

interface ApiService {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/fila")
    suspend fun getFila(@Header("Authorization") token: String): List<FilaResponse>

    @POST("api/chamar-proxima")
    suspend fun chamarProxima(
        @Header("Authorization") token: String,
        @Body request: Map<String, Any>
    ): FilaResponse

    @POST("api/chamadas")
    suspend fun repetirChamada(
        @Header("Authorization") token: String,
        @Body request: ChamadaRequest
    ): FilaResponse

    @POST("api/senhas/estornar")
    suspend fun estornarSenha(
        @Header("Authorization") token: String,
        @Body request: SenhaEstornoRequest
    ): FilaResponse

    @GET("api/admin/status")
    suspend fun getStatus(): StatusResponse
}
