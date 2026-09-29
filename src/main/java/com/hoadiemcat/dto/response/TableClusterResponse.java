package com.hoadiemcat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableClusterResponse {

    private Long masterTableId;
    private String masterTableNumber;
    private String masterTableName;
    private List<String> linkedTableNumbers;
    private Integer totalCapacity;
    private String message;
}
