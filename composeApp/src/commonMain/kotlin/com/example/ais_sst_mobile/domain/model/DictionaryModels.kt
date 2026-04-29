package com.example.ais_sst_mobile.domain.model

data class Speciality(
    val id: Int,
    val title: String
)

data class SocialStatus(
    val id: Int,
    val title: String
)

data class Group(
    val id: Int,
    val title: String,
    val course: Int
)