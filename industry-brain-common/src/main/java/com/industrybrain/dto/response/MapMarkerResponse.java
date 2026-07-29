package com.industrybrain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapMarkerResponse {
    private String type;          // star/circle/diamond/diamondOpen
    private String typeSymbol;    // ★/●/◆/◇
    private String name;
    private Double leftPercent;   // CSS position %
    private Double topPercent;    // CSS position %
    private String color;
    private String description;
}
