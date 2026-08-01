package com.industrybrain.service.cluster;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动聚类结果 DTO
 * 每簇 = 一条产业链（ChainDef），标签按上中下游分层归入节点。
 *
 * @author 产业大脑平台
 */
public class ClusterResult {

    private final List<ChainDef> chains = new ArrayList<>();

    public List<ChainDef> getChains() { return chains; }

    public void addChain(ChainDef chain) { chains.add(chain); }

    /** 一条产业链定义 */
    public static class ChainDef {
        private String name;
        /** up/mid/down → 标签列表（即节点） */
        private final Map<String, List<String>> tagsByLayer = new LinkedHashMap<>();
        /** 链覆盖企业并集数 */
        private int totalEnterprises;
        /** 链内标签总数 */
        private int tagCount;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Map<String, List<String>> getTagsByLayer() { return tagsByLayer; }
        public int getTotalEnterprises() { return totalEnterprises; }
        public void setTotalEnterprises(int totalEnterprises) { this.totalEnterprises = totalEnterprises; }
        public int getTagCount() { return tagCount; }
        public void setTagCount(int tagCount) { this.tagCount = tagCount; }

        public void addTag(String layer, String tag) {
            tagsByLayer.computeIfAbsent(layer, k -> new ArrayList<>()).add(tag);
        }
    }
}
