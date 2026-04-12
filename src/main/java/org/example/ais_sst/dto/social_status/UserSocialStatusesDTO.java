package org.example.ais_sst.dto.social_status;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserSocialStatusesDTO {
    private Long userId;
    private List<Long> social_statuses_id;
}