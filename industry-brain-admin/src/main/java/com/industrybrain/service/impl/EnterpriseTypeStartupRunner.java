package com.industrybrain.service.impl;

import com.industrybrain.mapper.mysql.EnterpriseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动时自动为企业表中 enterprise_type 为空的数据分类填充
 * 适用场景：Docker 直接灌数据后重启应用即可自动分类
 *
 * @author chentao
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnterpriseTypeStartupRunner implements ApplicationRunner {

    private final EnterpriseMapper enterpriseMapper;
    private final EnterpriseTypeClassifier classifier;
    private final EnterpriseServiceImpl enterpriseService;

    @Override
    public void run(ApplicationArguments args) {
        long nullCount = enterpriseMapper.countNullType();
        if (nullCount == 0) {
            log.info("企业类型分类检查: 所有企业已有类型，无需自动分类");
            return;
        }

        log.info("企业类型分类检查: 发现 {} 条企业 enterprise_type 为空，启动自动分类...", nullCount);
        try {
            int updated = enterpriseService.classifyEnterpriseTypes();
            log.info("企业类型自动分类完成: 本次填充 {} 条", updated);
        } catch (Exception e) {
            log.error("企业类型自动分类失败: {}", e.getMessage(), e);
        }
    }
}
