package com.industrybrain.service;

import com.industrybrain.dto.response.TechnicalProfileResponse;

/**
 * 技术画像业务接口
 * 负责企业技术能力分析、画像生成
 */
public interface ITechnicalProfileService {

    /**
     * 生成企业技术画像
     */
    TechnicalProfileResponse generateProfile(Long enterpriseId);
}
