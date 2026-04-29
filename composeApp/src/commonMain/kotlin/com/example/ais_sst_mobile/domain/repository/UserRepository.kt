package com.example.ais_sst_mobile.domain.repository

import com.example.ais_sst_mobile.domain.model.User

interface UserRepository {
    suspend fun getUserProfile(): Result<User>
}