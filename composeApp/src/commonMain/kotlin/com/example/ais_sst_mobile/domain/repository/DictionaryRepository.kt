package com.example.ais_sst_mobile.domain.repository

import com.example.ais_sst_mobile.domain.model.SocialStatus
import com.example.ais_sst_mobile.domain.model.Speciality

interface DictionaryRepository {
    suspend fun getSpecialities(): Result<List<Speciality>>
    suspend fun getSocialStatuses(): Result<List<SocialStatus>>
}