package com.industrybrain.service.embedding;

import java.util.List;

/**
 * 向量嵌入提供者接口
 * 抽象外部 LLM 嵌入 API 与本地降级实现，便于单测注入 fake provider。
 *
 * @author 产业大脑平台
 */
public interface EmbeddingProvider {

    /** 是否启用外部 embedding API（false 时调用方应走纯规则聚类降级） */
    boolean isEnabled();

    /**
     * 批量嵌入，返回与输入同序的向量列表；单条失败时对应位置为 null。
     *
     * @param texts 待嵌入文本
     * @return 向量列表
     */
    List<float[]> embed(List<String> texts);

    /** 单条文本嵌入 */
    float[] embed(String text);
}
