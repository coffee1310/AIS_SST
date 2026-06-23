package org.example.ais_sst.dto.event_roles;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleOccupancyInfo {
    private Long eventRoleId;
    private long mainOccupied;
    private int mainCapacity;
    private long reserveOccupied;
    private int reserveCapacity;
    private int mainAvailable;
    private int reserveAvailable;
    private boolean isMainFull;
    private boolean isReserveFull;

    public boolean hasMainSlots() {
        return mainAvailable > 0;
    }

    public boolean hasReserveSlots() {
        return reserveAvailable > 0;
    }

    public boolean isFullyFull() {
        return isMainFull && isReserveFull;
    }
}