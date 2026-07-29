package com.industrybrain.service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.industrybrain.dto.request.DiagnosisRequest;
import com.industrybrain.dto.response.*;

import java.util.List;

/**
 * 强弱链诊断业务接口 — D01~D04
 * @author 何锦鹏
 */
public interface IDiagnosisService {

    // ─── 旧接口（保持兼容，返回空对象） ───
    Object diagnose(DiagnosisRequest request);
    DiagnosisResponse getDiagnosisResult(Long chainId, String year);

    // ─── D01~D04 ───
    List<DiagnosisRecordResponse> getDiagnosisList(String regionCode, String status);
    DiagnosisNodeDetailResponse getNodeDetail(Long nodeId);
    String triggerRecalc(String regionCode);
    RecalcStatusResponse getRecalcStatus(String taskId);
    DiagnosisSummaryResponse getSummary(String regionCode);

    /** 异步执行重新诊断 */
    void executeRecalcAsync(String taskId, String regionCode);
}
