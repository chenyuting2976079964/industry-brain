package com.industrybrain.service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.industrybrain.domain.model.Enterprise;
import com.industrybrain.dto.request.EnterpriseSearchRequest;
import com.industrybrain.dto.response.*;

import java.util.List;

/**
 * 企业信息业务接口 — E01~E05
 * @author 何锦鹏
 */
public interface IEnterpriseService {

    // ─── 旧接口（保持兼容） ───
    List<EnterpriseDetailResponse> searchEnterprises(EnterpriseSearchRequest request);
    EnterpriseDetailResponse getEnterpriseDetail(Long enterpriseId);
    List<Enterprise> listEnterprises(String industry, String regionCode);

    // ─── E01~E05 ───
    EnterpriseResponse getEnterprise(Long id);
    EnterpriseProfileResponse getProfile(Long id);
    List<EnterprisePatentResponse> getPatents(Long enterpriseId, int page, int size, String keyword, String type);
    long countPatents(Long enterpriseId, String keyword, String type);
    EnterpriseEvaluateResponse getEvaluate(Long id);
    List<LocalEnterpriseResponse> getLocalEnterprises(String keyword);
    List<EnterpriseResponse> searchEnterprise(String keyword);
}
