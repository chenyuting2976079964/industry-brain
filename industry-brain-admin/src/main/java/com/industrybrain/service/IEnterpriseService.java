package com.industrybrain.service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.industrybrain.domain.model.Enterprise;
import com.industrybrain.dto.request.EnterpriseSearchRequest;
import com.industrybrain.dto.response.*;

import java.util.List;
import java.util.Map;

/**
 * 企业信息业务接口 — E01~E05 + enterprise_type 扩展
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

    // ─── enterprise_type 新增接口 ───
    /** 按企业类型查询企业列表 */
    List<Enterprise> getEnterprisesByType(String type);
    /** 按企业类型统计企业数 */
    Long getEnterprisesCountByType(String type);
    /** 获取企业类型的分布统计 */
    List<Map<String, Object>> getEnterpriseTypeStats();

    /** 批量分类并填充企业类型（仅更新 enterprise_type 为空的企业） */
    int classifyEnterpriseTypes();
}
