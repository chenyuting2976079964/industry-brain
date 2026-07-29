package com.industrybrain.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecalcRequest {
    private String regionCode;
    private List<Long> nodeIds;  // empty = all nodes
}
