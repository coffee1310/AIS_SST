package com.example.ais_sst_mobile.domain.repository

import com.example.ais_sst_mobile.data.network.dto.AuthResponse
import com.example.ais_sst_mobile.data.network.dto.LoginRequest
import com.example.ais_sst_mobile.data.network.dto.RegisterRequest

interface AuthRepository {
    suspend fun login(request: LoginRequest): Result<AuthResponse>
    suspend fun register(request: RegisterRequest): Result<Unit>
    suspend fun requestPasswordReset(email: String): Result<Unit>
    suspend fun verifyPasswordReset(email: String, code: String, newPassword: String): Result<Unit>
    suspend fun sendRegistrationCode(name: String, surname: String, studentEmail: String): Result<Unit>
    suspend fun verifyAndCreateAccount(
        email: String,
        code: String,
        accountRequest: RegisterRequest
    ): Result<Unit>
}