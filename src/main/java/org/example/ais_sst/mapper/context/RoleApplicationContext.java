package org.example.ais_sst.mapper.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleApplicationContext {
    // eventRoleId -> capacity (основные места)
    private Map<Long, Integer> capacityMap;

    // eventRoleId -> reserveCapacity (резервные места)
    private Map<Long, Integer> reserveCapacityMap;

    // eventRoleId -> количество принятых на основные места (isReserve = false)
    private Map<Long, Integer> currentMainParticipantsMap;

    // eventRoleId -> количество принятых на резервные места (isReserve = true)
    private Map<Long, Integer> currentReserveParticipantsMap;

    public Integer getCapacity(Long eventRoleId) {
        return capacityMap != null ? capacityMap.get(eventRoleId) : 0;
    }

    public Integer getReserveCapacity(Long eventRoleId) {
        return reserveCapacityMap != null ? reserveCapacityMap.get(eventRoleId) : 0;
    }

    public Integer getCurrentMainParticipants(Long eventRoleId) {
        return currentMainParticipantsMap != null ? currentMainParticipantsMap.getOrDefault(eventRoleId, 0) : 0;
    }

    public Integer getCurrentReserveParticipants(Long eventRoleId) {
        return currentReserveParticipantsMap != null ? currentReserveParticipantsMap.getOrDefault(eventRoleId, 0) : 0;
    }

    public Integer getRemainingMainSlots(Long eventRoleId) {
        Integer capacity = getCapacity(eventRoleId);
        if (capacity == null || capacity <= 0) return 0;
        return Math.max(0, capacity - getCurrentMainParticipants(eventRoleId));
    }

    public Integer getRemainingReserveSlots(Long eventRoleId) {
        Integer reserveCapacity = getReserveCapacity(eventRoleId);
        if (reserveCapacity == null || reserveCapacity <= 0) return 0;
        return Math.max(0, reserveCapacity - getCurrentReserveParticipants(eventRoleId));
    }

    public Boolean isMainFull(Long eventRoleId) {
        return getRemainingMainSlots(eventRoleId) <= 0;
    }

    public Boolean isReserveFull(Long eventRoleId) {
        return getRemainingReserveSlots(eventRoleId) <= 0;
    }

    public Boolean isFullyFull(Long eventRoleId) {
        return isMainFull(eventRoleId) && isReserveFull(eventRoleId);
    }
}