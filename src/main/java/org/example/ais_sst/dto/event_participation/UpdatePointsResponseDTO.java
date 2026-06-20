package org.example.ais_sst.dto.event_participation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePointsResponseDTO {
    private Long entityId;
    private String entityType;
    private String entityName;
    private Integer oldPoints;
    private Integer newPoints;
    private String reason;
    private Boolean success;
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkUpdateResponse {
        private Long eventId;
        private Integer updatedCount;
        private Integer totalPointsAfterUpdate;
        private List<UpdatePointsResponseDTO> details;
        private String message;
    }
}