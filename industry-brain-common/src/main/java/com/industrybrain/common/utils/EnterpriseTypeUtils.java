package com.industrybrain.common.utils;

import com.industrybrain.common.enums.EnterpriseTypeEnum;

/**
 * 企业类型工具类 - 提供类型校验和转换辅助方法
 */
public class EnterpriseTypeUtils {

    /**
     * 验证企业类型是否为合法值
     * @param type 类型代码或标签
     * @return true 如果为合法的企业类型
     */
    public static boolean isValid(String type) {
        if (type == null || type.trim().isEmpty()) return true; // NULL 和空值视为有效（未设置）
        return EnterpriseTypeEnum.isValid(type);
    }

    /**
     * 规范化企业类型代码（标准化存储）
     * @param input 原始输入（可以是code或label）
     * @return 标准化的code值，如果无效则返回null
     */
    public static String normalize(String input) {
        if (input == null) return null;

        String trimmed = input.trim();
        if (trimmed.isEmpty()) return null;

        // 如果是code，直接返回
        if (EnterpriseTypeEnum.isValid(trimmed)) {
            return trimmed;
        }

        // 尝试通过label匹配
        EnterpriseTypeEnum found = EnterpriseTypeEnum.fromLabel(trimmed);
        if (found != null) {
            return found.getCode();
        }

        // 如果都不匹配，返回null表示无效
        return null;
    }

    /**
     * 获取所有可用的企业类型选项
     * @return list格式: [{"code":"LEADER","label":"龙头企业","description":"..."}, ...]
     */
    public static Object[] getOptions() {
        // 这里可以返回JSON格式供前端使用
        return EnterpriseTypeEnum.getOptions().toArray();
    }

    /**
     * 根据描述文本推断可能的企业类型（启发式）
     * @param description 企业简介或描述文本
     * @return 推测的企业类型code
     */
    public static String inferFromDescription(String description) {
        if (description == null) return null;
        String desc = description.toLowerCase();

        if (desc.contains("龙头") || desc.contains("领军") || desc.contains("巨头")) {
            return EnterpriseTypeEnum.LEADER.getCode();
        }
        if (desc.contains("专精特新") || desc.contains("专特新精")) {
            return EnterpriseTypeEnum.SPECIALIZED.getCode();
        }
        if (desc.contains("高新") || desc.contains("高新技术")) {
            return EnterpriseTypeEnum.HIGH_TECH.getCode();
        }
        if (desc.contains("科技型") || desc.contains("科技中小")) {
            return EnterpriseTypeEnum.TECH_SMALL.getCode();
        }
        return null;
    }

    private EnterpriseTypeUtils() {
        // 工具类，禁止实例化
    }
}
