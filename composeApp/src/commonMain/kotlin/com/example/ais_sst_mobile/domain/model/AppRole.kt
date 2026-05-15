package com.example.ais_sst_mobile.domain.model

enum class AppRole(val serverName: String, val uiName: String) {
    ADMINISTRATOR("Administrator", "Администратор"),
    SECRETARY("Secretary", "Секретарь студсовета"),
    CHAIRMAN("Chairman", "Председатель студсовета"),
    SECTOR_COORDINATOR("Sector_coordinator", "Координатор сектора"),
    DEPUTY_CHAIRMAN("Deputy_chairman", "Заместитель председателя"),
    CURATOR("Curator", "Куратор студсовета"),
    ACTIVIST("Activist", "Активист студсовета"),
    STUDENT("Student", "Студент");

    companion object {
        fun fromServerName(name: String?): AppRole {
            if (name == null) return STUDENT

            val cleanName = name.removePrefix("ROLE_")

            return entries.find { it.serverName.equals(cleanName, ignoreCase = true) } ?: STUDENT
        }
    }

    fun isBoardMember(): Boolean {
        return this == SECRETARY ||
                this == CHAIRMAN ||
                this == SECTOR_COORDINATOR ||
                this == DEPUTY_CHAIRMAN
    }
    fun isSecondBoardMember(): Boolean {
        return this == SECRETARY ||
                this == CHAIRMAN ||
                this == CURATOR ||
                this == DEPUTY_CHAIRMAN
    }
    fun isThirdBoardMember(): Boolean {
        return this == SECRETARY ||
                this == CHAIRMAN ||
                this == SECTOR_COORDINATOR ||
                this == CURATOR ||
                this == DEPUTY_CHAIRMAN
    }
}