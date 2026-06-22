package com.example.ais_sst_mobile.presentation.events.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.data.network.dto.*
import com.example.ais_sst_mobile.domain.model.Event
import com.example.ais_sst_mobile.domain.repository.EventsRepository
import com.example.ais_sst_mobile.domain.repository.SectorsRepository
import com.example.ais_sst_mobile.domain.repository.UserRepository
import com.example.ais_sst_mobile.presentation.events.create.RoleUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import io.ktor.util.encodeBase64

sealed interface EditEventEffect {
    data object NavigateBack : EditEventEffect
    data class ShowError(val message: String) : EditEventEffect
}

data class SelectedOrganizerUi(
    val id: Int,
    val name: String,
    val surname: String,
    val photo: String?
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class EditEventScreenModel(
    private val userRepository: UserRepository,
    private val eventsRepository: EventsRepository,
    private val sectorsRepository: SectorsRepository
) : ViewModel() {

    private var initialEvent: Event? = null
    private var originalPhotoString: String? = null
    private var isPhotoChanged = false

    val isScreenLoading = MutableStateFlow(true)
    val isLoading = MutableStateFlow(false)

    private val _effect = Channel<EditEventEffect>()
    val effect = _effect.receiveAsFlow()

    // Поля формы
    val title = MutableStateFlow("")
    val description = MutableStateFlow("")
    val date = MutableStateFlow("")
    val startTime = MutableStateFlow("")
    val endTime = MutableStateFlow("")
    val venue = MutableStateFlow("")
    val isPublic = MutableStateFlow(true)
    val isFree = MutableStateFlow(false)
    val isDraft = MutableStateFlow(false)
    val maxParticipants = MutableStateFlow("")

    val selectedSectorIds = MutableStateFlow<Set<Int>>(emptySet())
    val roles = MutableStateFlow<List<RoleUiModel>>(emptyList())
    val selectedOrganizers = MutableStateFlow<List<SelectedOrganizerUi>>(emptyList())
    val eventPhotoBase64 = MutableStateFlow<String?>(null)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredActivists = _searchQuery
        .debounce(300)
        .mapLatest { query ->
            if (query.isBlank()) emptyList()
            else userRepository.getAllUsers(page = 0, size = 20, searchQuery = query).getOrNull()?.content ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val globalRoles = MutableStateFlow<List<RoleDto>>(emptyList())
    val sectors = MutableStateFlow<List<SectorDto>>(emptyList())

    fun loadEventData(eventId: Int) {
        viewModelScope.launch {
            isScreenLoading.value = true

            val eventRes = eventsRepository.getEventById(eventId)
            val rolesRes = eventsRepository.getEventRoles(eventId)
            val globalRolesRes = eventsRepository.getGlobalRoles()
            val sectorsRes = sectorsRepository.getSectors()

            if (eventRes.isSuccess && rolesRes.isSuccess && globalRolesRes.isSuccess && sectorsRes.isSuccess) {
                val event = eventRes.getOrNull()!!
                val gRoles = globalRolesRes.getOrNull()!!

                initialEvent = event
                originalPhotoString = event.photoBase64

                val cleanPhoto = event.photoBase64?.let {
                    if (it.startsWith("data:image")) it.substringAfter("base64,").trim() else it
                }
                eventPhotoBase64.value = cleanPhoto

                globalRoles.value = gRoles
                sectors.value = sectorsRes.getOrNull()!!

                title.value = event.title
                description.value = event.description
                venue.value = event.venue
                isPublic.value = event.isPublic
                isFree.value = event.isFreeEvent
                isDraft.value = event.isDraft
                maxParticipants.value = if (event.maxParticipantsCount > 0) event.maxParticipantsCount.toString() else ""

                // Так как sectorIds - это только для участников, загружаем как есть
                selectedSectorIds.value = event.sectorIds.toSet()

                // Парсинг дат
                val dParts = event.rawDate.split("-")
                if (dParts.size == 3) {
                    date.value = "${dParts[2]}${dParts[1]}${dParts[0]}"
                }
                startTime.value = event.rawStartTime.take(5).replace(":", "")
                endTime.value = event.rawEndTime.take(5).replace(":", "")

                // Маппинг организаторов
                selectedOrganizers.value = event.organizers.map { org ->
                    SelectedOrganizerUi(org.userId, org.userName, org.userSurname, org.userPhoto)
                }

                // Маппинг ролей
                val mappedRoles = rolesRes.getOrNull()!!.map { r ->
                    val dDate = r.deadline?.substringBefore("T")?.split("-")?.let { if (it.size == 3) "${it[2]}${it[1]}${it[0]}" else "" } ?: ""
                    val dTime = r.deadline?.substringAfter("T")?.take(5)?.replace(":", "") ?: ""

                    RoleUiModel(
                        originalId = r.id,
                        name = r.globalEventRoleTitle,
                        tasks = r.description ?: "",
                        deadlineDate = dDate,
                        deadlineTime = dTime,
                        peopleCount = r.capacity.toString(),
                        reserveCount = r.reserveCapacity.toString(),
                        sector = gRoles.find { it.id == r.globalEventRoleId }?.sectorTitle ?: "",
                        isOrganizer = false,
                        isGlobalSelected = true,
                        isExpanded = false,
                        isDeleted = false,
                        globalRoleId = r.globalEventRoleId,
                        points = gRoles.find { it.id == r.globalEventRoleId }?.defaultPoints?.toString() ?: ""
                    )
                }.toMutableList()

                val orgCount = event.maxOrganizersCount - event.organizers.size
                if (orgCount > 0) {
                    mappedRoles.add(0, RoleUiModel(
                        name = "Организатор",
                        isOrganizer = true,
                        isGlobalSelected = true,
                        peopleCount = orgCount.toString(),
                        isExpanded = false
                    ))
                }

                roles.value = mappedRoles
                isScreenLoading.value = false
            } else {
                showError("Не удалось загрузить данные мероприятия")
            }
        }
    }

    // Методы работы с UI
    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    fun updateEventPhoto(photoBytes: ByteArray) {
        eventPhotoBase64.value = photoBytes.encodeBase64()
        isPhotoChanged = true
    }

    fun showError(message: String) { viewModelScope.launch { _effect.send(EditEventEffect.ShowError(message)) } }

    fun addOrganizer(user: UserProfileDto) {
        val currentList = selectedOrganizers.value.toMutableList()
        if (!currentList.any { it.id == user.id }) {
            currentList.add(SelectedOrganizerUi(user.id, user.name, user.surname, user.photo))
            selectedOrganizers.value = currentList
        }
        _searchQuery.value = ""
    }

    fun removeOrganizer(id: Int) {
        selectedOrganizers.value = selectedOrganizers.value.filter { it.id != id }
    }

    // Методы управления ролями
    fun updateRole(index: Int, role: RoleUiModel) {
        val list = roles.value.toMutableList()
        list[index] = role
        roles.value = list
    }
    fun removeRole(index: Int) {
        val list = roles.value.toMutableList()
        list[index] = list[index].copy(isDeleted = true)
        roles.value = list
    }
    fun addRole() { roles.value = roles.value + RoleUiModel() }
    fun addOrganizerRole() {
        roles.value = roles.value + RoleUiModel(name = "Организатор", isOrganizer = true, isGlobalSelected = true)
    }

    fun saveChanges(eventId: Int) {
        if (title.value.isBlank() || venue.value.isBlank() || date.value.length != 8 || startTime.value.length != 4 || endTime.value.length != 4) {
            showError("Заполните все обязательные поля со звездочкой (*)")
            return
        }

        val customRoles = roles.value.filter { !it.isOrganizer }
        val unselectedRole = customRoles.find { it.globalRoleId == null && !it.isDeleted }
        if (unselectedRole != null) {
            showError("Выберите роль «${unselectedRole.name.ifBlank { "Новая роль" }}» из списка!")
            return
        }

        viewModelScope.launch {
            isLoading.value = true

            val formattedDate = "${date.value.substring(4, 8)}-${date.value.substring(2, 4)}-${date.value.substring(0, 2)}"
            val formattedStartTime = "${startTime.value.substring(0, 2)}:${startTime.value.substring(2, 4)}:00"
            val formattedEndTime = "${endTime.value.substring(0, 2)}:${endTime.value.substring(2, 4)}:00"

            val maxOrg = selectedOrganizers.value.size +
                    roles.value.filter { it.isOrganizer && !it.isDeleted }.sumOf { it.peopleCount.toIntOrNull() ?: 1 }

            val maxPart = if (isFree.value) maxParticipants.value.toIntOrNull() ?: 0 else 0

            val photoPayload = eventPhotoBase64.value?.let { base64 ->
                if (base64.isNotBlank()) "data:image/jpeg;base64,$base64" else ""
            } ?: ""

            // ИСПРАВЛЕННАЯ ЛОГИКА СЕКТОРОВ: Только для карточки "Участник" (Свободное и непубличное)
            val finalSectorIds = if (isFree.value && !isPublic.value) {
                if (selectedSectorIds.value.isEmpty()) {
                    sectors.value.map { it.id }
                } else {
                    selectedSectorIds.value.toList()
                }
            } else {
                emptyList()
            }

            val updateRequest = CreateEventRequestDto(
                title = title.value,
                description = description.value,
                photo = photoPayload,
                dateOfEvent = formattedDate,
                startTime = formattedStartTime,
                endTime = formattedEndTime,
                venue = venue.value,
                organizerIds = emptyList(),
                referenceToPosition = "",
                isPublic = isPublic.value,
                isDraft = isDraft.value,
                isFreeEvent = isFree.value,
                maxParticipantsCount = maxPart,
                maxOrganizersCount = maxOrg,
                sectorIds = finalSectorIds
            )

            eventsRepository.updateEvent(eventId, updateRequest).fold(
                onSuccess = {
                    var hasErrors = false

                    // 1. Диффы Организаторов
                    val initialOrgIds = initialEvent?.organizers?.map { it.userId }?.toSet() ?: emptySet()
                    val currentOrgIds = selectedOrganizers.value.map { it.id }.toSet()

                    for (id in (currentOrgIds - initialOrgIds)) {
                        if (eventsRepository.addOrganizer(eventId, id).isFailure) hasErrors = true
                    }
                    for (id in (initialOrgIds - currentOrgIds)) {
                        if (eventsRepository.removeOrganizer(eventId, id).isFailure) hasErrors = true
                    }

                    // 2. Диффы Ролей
                    for (role in customRoles) {
                        if (role.isDeleted) {
                            if (role.originalId != null) {
                                if (eventsRepository.deleteEventRole(role.originalId).isFailure) hasErrors = true
                            }
                            continue
                        }

                        val roleDeadlineDate = "${role.deadlineDate.substring(4, 8)}-${role.deadlineDate.substring(2, 4)}-${role.deadlineDate.substring(0, 2)}"
                        val roleDeadline = "${roleDeadlineDate}T${role.deadlineTime.substring(0, 2)}:${role.deadlineTime.substring(2, 4)}:00"

                        val roleRequest = CreateEventRoleRequestDto(
                            eventId = eventId,
                            globalEventRoleId = role.globalRoleId!!,
                            capacity = role.peopleCount.toIntOrNull() ?: 1,
                            reserveCapacity = role.reserveCount.toIntOrNull() ?: 0,
                            deadline = roleDeadline,
                            description = role.tasks
                        )

                        if (role.originalId == null) {
                            if (eventsRepository.createEventRole(roleRequest).isFailure) hasErrors = true
                        } else {
                            if (eventsRepository.updateEventRole(role.originalId, roleRequest).isFailure) hasErrors = true
                        }
                    }

                    // 3. Заявка на организатора (если добавили)
                    val initiallyHadOrgApp = (initialEvent?.maxOrganizersCount ?: 0) > (initialEvent?.organizers?.size ?: 0)
                    val orgRole = roles.value.find { it.isOrganizer && !it.isDeleted }
                    if (orgRole != null && !initiallyHadOrgApp) {
                        if (eventsRepository.createOrganizerApplication(eventId).isFailure) hasErrors = true
                    }

                    isLoading.value = false
                    if (!hasErrors) {
                        _effect.send(EditEventEffect.NavigateBack)
                    } else {
                        showError("Мероприятие обновлено, но произошли ошибки при синхронизации ролей.")
                        _effect.send(EditEventEffect.NavigateBack) // Все равно выходим
                    }
                },
                onFailure = { error ->
                    isLoading.value = false
                    if (error.message?.contains("409") == true) {
                        showError("Событие с такими параметрами (название/дата) уже существует в базе!")
                    } else {
                        showError("Не удалось обновить мероприятие.")
                    }
                }
            )
        }
    }
}