package com.industrybrain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnterprisePatentResponse {
    private Long patentId;
    private String patentNo;
    private String patentName;
    private String patentType;
    private String patentTypeLabel;
    private String applicationDate;
    private String legalStatus;
    private String statusColor;
    private String abstractPreview;
    private Boolean isHighValue;
    private String ipcMain;
    private Integer claimCount;
}
