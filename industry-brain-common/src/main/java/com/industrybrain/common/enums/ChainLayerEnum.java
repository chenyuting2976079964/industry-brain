package com.industrybrain.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 产业链层级枚举
 * 对标技术架构文档: 上中下游三列图谱布局
 */
@Getter
@AllArgsConstructor
public enum ChainLayerEnum {
    UPSTREAM("upstream", "上游", "原材料、核心技术、关键零部件"),
    MIDSTREAM("midstream", "中游", "核心制造、系统集成、平台服务"),
    DOWNSTREAM("downstream", "下游", "终端产品、应用服务、市场销售");

    private final String code;
    private final String label;
    private final String description;

    public static ChainLayerEnum fromCode(String code) {
        for (ChainLayerEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
