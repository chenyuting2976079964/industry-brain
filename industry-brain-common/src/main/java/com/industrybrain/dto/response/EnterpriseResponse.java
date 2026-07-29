package com.industrybrain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnterpriseResponse {
    private Long id;
    private String entName;
    private String shortName;
    private String avatarLetter;
    private List<String> tags;
    private String description;
    private String legalPerson;
    private String registeredCapital;
    private Integer establishYear;
    private String province;
    private String city;
    private String district;
    private String scale;
    private String weakness;
    private List<String> weaknessDetail;
}
