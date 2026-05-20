package com.example.ais_sst_mobile.domain.repository

import com.example.ais_sst_mobile.data.network.dto.AccountRequestDto
import com.example.ais_sst_mobile.data.network.dto.UserPageResponseDto
import com.example.ais_sst_mobile.data.network.dto.UserProfileDto
import com.example.ais_sst_mobile.domain.model.User

interface UserRepository {
    suspend fun getUserProfile(): Result<User>
    suspend fun getAccountRequests(): Result<List<AccountRequestDto>>
    suspend fun rejectAccountRequest(id: Int, reason: String): Result<Unit>
    suspend fun acceptAccountRequest(id: Int): Result<Unit>
    suspend fun getAccountRequestById(id: Int): Result<AccountRequestDto>
    suspend fun getUserProfileById(id: Int): Result<UserProfileDto>
    suspend fun getActivists(page: Int = 0, size: Int = 16, searchQuery: String = ""): Result<UserPageResponseDto>
    suspend fun getAllUsers(page: Int = 0, size: Int = 16, searchQuery: String = ""): Result<UserPageResponseDto>
    suspend fun getUsersByRole(role: String): Result<List<UserProfileDto>>
}