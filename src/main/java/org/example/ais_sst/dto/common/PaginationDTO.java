package org.example.ais_sst.dto.common;

import lombok.Data;
import org.springframework.data.domain.Sort;

@Data
public class PaginationDTO {
    private int page = 0;
    private int size = 10;
    private String sortBy = "id";
    private Sort.Direction sortDirection = Sort.Direction.ASC;
}