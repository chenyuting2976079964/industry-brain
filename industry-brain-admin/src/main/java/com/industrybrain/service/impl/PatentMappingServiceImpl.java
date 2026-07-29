package com.industrybrain.service.impl;

import com.industrybrain.domain.model.*;
import com.industrybrain.mapper.mysql.*;
import com.industrybrain.service.IPatentMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * A1: 专利→产业链节点智能映射引擎 (v2.0)
 * 三通道混合架构: 规则匹配 + 语义向量 + 先验映射
 * @author 何锦鹏
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatentMappingServiceImpl implements IPatentMappingService {

    private final IpcChainMappingMapper ipcChainMappingMapper;
    private final PatentNodeMappingMapper patentNodeMappingMapper;
    private final ChainNodeMapper chainNodeMapper;

    private static final double[] W_HIGH_CONFIDENCE = {0.6, 0.3, 0.1};
    private static final double[] W_MEDIUM_CONFIDENCE = {0.5, 0.4, 0.1};
    private static final double[] W_LOW_IPC = {0.3, 0.6, 0.1};
    private static final double[] W_NO_IPC = {0.1, 0.8, 0.1};
    private static final double[] W_PRIOR_HIGH = {0.2, 0.1, 0.7};
    private static final double THRESHOLD_HIGH = 0.70;
    private static final double THRESHOLD_MEDIUM = 0.40;

    @Override
    public List<IPatentMappingService.MappingResult> mapPatentToNode(Patent patent) {
        log.info("A1: mapping patent [{}] {}", patent.getPatentNumber(), patent.getPatentName());

        RuleScoreResult ruleResult = ruleChannel(patent);
        VectorScoreResult vectorResult = vectorChannel(patent);
        double priorScore = getPriorScore(patent.getId());
        double[] weights = selectWeights(ruleResult, vectorResult, priorScore);

        Map<Long, CandidateScore> candidateMap = new LinkedHashMap<>();
        ruleResult.candidates.forEach(c -> {
            if (c.nodeId == null) return;
            candidateMap.computeIfAbsent(c.nodeId, k -> new CandidateScore()).addRule(c.score, c.matchType, c.evidence);
        });
        vectorResult.candidates.forEach(c -> {
            if (c.nodeId == null) return;
            candidateMap.computeIfAbsent(c.nodeId, k -> new CandidateScore()).addVector(c.score, c.evidence);
        });

        List<IPatentMappingService.MappingResult> results = new ArrayList<>();
        for (Map.Entry<Long, CandidateScore> entry : candidateMap.entrySet()) {
            Long nodeId = entry.getKey();
            CandidateScore cs = entry.getValue();
            double combinedScore = weights[0] * cs.maxRuleScore + weights[1] * cs.maxVectorScore + weights[2] * priorScore;
            combinedScore = Math.min(combinedScore, 1.0);

            String confidenceLevel;
            if (combinedScore >= THRESHOLD_HIGH) confidenceLevel = "HIGH";
            else if (combinedScore >= THRESHOLD_MEDIUM) confidenceLevel = "MEDIUM";
            else confidenceLevel = "LOW";

            IPatentMappingService.MappingResult mr = new IPatentMappingService.MappingResult();
            mr.setPatentId(patent.getId()); mr.setNodeId(nodeId);
            mr.setConfidence(combinedScore); mr.setConfidenceLevel(confidenceLevel);
            mr.setMatchType(cs.primaryMatchType); mr.setRuleScore(cs.maxRuleScore);
            mr.setVectorScore(cs.maxVectorScore); mr.setPriorScore(priorScore);
            mr.setEvidence(cs.getCombinedEvidence());
            results.add(mr);
        }
        results.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));
        log.info("A1: patent [{}] → {} candidates (top: {})", patent.getPatentNumber(), results.size(),
            results.isEmpty() ? "none" : results.get(0).getConfidenceLevel());
        return results;
    }

    // ─── Channel 1: 规则匹配 ───
    private RuleScoreResult ruleChannel(Patent patent) {
        Map<Long, double[]> subScores = new LinkedHashMap<>();
        Map<Long, String> evidenceMap = new LinkedHashMap<>();

        // Layer 1: IPC → 节点 (权重 0.5)
        if (patent.getIpcMain() != null) {
            List<IpcChainMapping> ipcMappings = ipcChainMappingMapper.selectByIpcPrefix(patent.getIpcMain());
            if (ipcMappings != null) {
                for (IpcChainMapping m : ipcMappings) {
                    double s = m.getWeight() != null ? m.getWeight() : 0.85;
                    if ("GROUP".equals(m.getIpcLevel())) s = Math.min(s * 1.2, 1.0);
                    subScores.computeIfAbsent(m.getNodeId(), k -> new double[4]);
                    double[] scores = subScores.get(m.getNodeId());
                    scores[0] = Math.max(scores[0], s);
                    evidenceMap.merge(m.getNodeId(), "IPC:" + patent.getIpcMain(), (a, b) -> a + "; " + b);
                }
            }
        }

        // Layer 2: 关键词文本匹配 (权重 0.35)
        String patentText = (patent.getPatentName() != null ? patent.getPatentName() : "")
                + " " + (patent.getIpcMain() != null ? patent.getIpcMain() : "");
        if (!patentText.trim().isEmpty()) {
            List<ChainNode> allNodes = chainNodeMapper.selectList(null);
            if (allNodes != null) {
                for (ChainNode node : allNodes) {
                    String nodeText = node.getNodeName() != null ? node.getNodeName() : "";
                    if (nodeText.isEmpty()) continue;
                    double kwScore = textMatchScore(patentText, nodeText);
                    if (kwScore > 0.05) {
                        subScores.computeIfAbsent(node.getId(), k -> new double[4]);
                        double[] scores = subScores.get(node.getId());
                        scores[1] = Math.max(scores[1], kwScore);
                        evidenceMap.merge(node.getId(), "KW:" + nodeText + "(" + String.format("%.2f", kwScore) + ")",
                            (a, b) -> a + "; " + b);
                    }
                }
            }
        }

        // Layer 3: 申请人企业→历史映射 (权重 0.1)
        if (patent.getApplicantEnterpriseId() != null) {
            List<PatentNodeMapping> entMappings = patentNodeMappingMapper.selectByEnterpriseId(patent.getApplicantEnterpriseId());
            if (entMappings != null && !entMappings.isEmpty()) {
                Map<Long, Long> freqMap = new LinkedHashMap<>();
                long total = 0;
                for (PatentNodeMapping m : entMappings) {
                    freqMap.merge(m.getNodeId(), 1L, Long::sum);
                    total++;
                }
                for (Map.Entry<Long, Long> entry : freqMap.entrySet()) {
                    Long nodeId = entry.getKey();
                    double appScore = Math.min(entry.getValue() / (double) Math.max(total, 1), 1.0);
                    subScores.computeIfAbsent(nodeId, k -> new double[4]);
                    double[] scores = subScores.get(nodeId);
                    scores[2] = Math.max(scores[2], appScore);
                    evidenceMap.merge(nodeId, "APP:企业历史" + entry.getValue() + "/" + total, (a, b) -> a + "; " + b);
                }
            }
        }

        // Layer 4: 先验映射 (权重 0.05)
        List<PatentNodeMapping> priorMappings = patentNodeMappingMapper.selectByPatentId(patent.getId());
        if (priorMappings != null) {
            for (PatentNodeMapping m : priorMappings) {
                if (m.getMappingConfidence() == null) continue;
                subScores.computeIfAbsent(m.getNodeId(), k -> new double[4]);
                double[] scores = subScores.get(m.getNodeId());
                scores[3] = Math.max(scores[3], Math.min(m.getMappingConfidence().doubleValue(), 1.0));
                evidenceMap.merge(m.getNodeId(), "PRIOR:历史conf=" + String.format("%.2f", m.getMappingConfidence()),
                    (a, b) -> a + "; " + b);
            }
        }

        // 加权融合
        RuleScoreResult result = new RuleScoreResult();
        for (Map.Entry<Long, double[]> entry : subScores.entrySet()) {
            Long nodeId = entry.getKey();
            double[] scores = entry.getValue();
            double ruleScore = scores[0] * 0.5 + scores[1] * 0.35 + scores[2] * 0.1 + scores[3] * 0.05;
            double maxScore = 0; int maxIdx = 0;
            for (int i = 0; i < 4; i++) { if (scores[i] > maxScore) { maxScore = scores[i]; maxIdx = i; } }
            String[] typeNames = {"IPC", "KEYWORD", "APPLICANT", "PRIOR"};
            result.candidates.add(new RuleCandidate(nodeId, Math.min(ruleScore, 1.0),
                maxScore > 0 ? typeNames[maxIdx] : "UNKNOWN", evidenceMap.getOrDefault(nodeId, "")));
        }
        return result;
    }

    private double textMatchScore(String patentText, String nodeText) {
        String p = patentText.toLowerCase(), n = nodeText.toLowerCase();
        if (n.contains(p) || p.contains(n)) return 0.85;
        int overlap = 0;
        for (int i = 0; i < n.length() - 1; i++) {
            if (p.contains(n.substring(i, Math.min(i + 2, n.length())))) overlap++;
        }
        return Math.min(n.length() > 0 ? (double) overlap / n.length() : 0, 1.0) * 0.6;
    }

    // ─── Channel 2: 向量匹配 ───
    private VectorScoreResult vectorChannel(Patent patent) {
        return new VectorScoreResult();
    }

    // ─── Channel 3: 先验映射 ───
    private double getPriorScore(Long patentId) {
        List<PatentNodeMapping> existing = patentNodeMappingMapper.selectByPatentId(patentId);
        if (existing == null || existing.isEmpty()) return 0.0;
        return existing.stream().filter(m -> "HIGH".equals(m.getConfidenceLevel())).findFirst()
            .map(m -> Math.min(m.getMappingConfidence() != null ? m.getMappingConfidence().doubleValue() : 0.0, 1.0))
            .orElse(0.0);
    }

    private double[] selectWeights(RuleScoreResult rule, VectorScoreResult vector, double priorScore) {
        boolean ipcExactHit = rule.candidates.stream().anyMatch(c -> "IPC".equals(c.matchType));
        if (priorScore >= 0.95) return W_PRIOR_HIGH;
        if (ipcExactHit) return W_MEDIUM_CONFIDENCE;
        if (!vector.candidates.isEmpty()) return W_LOW_IPC;
        return W_NO_IPC;
    }

    // ─── Inner classes ───
    private static class RuleScoreResult { List<RuleCandidate> candidates = new ArrayList<>(); }
    private static class VectorScoreResult { List<VectorCandidate> candidates = new ArrayList<>(); }
    private static class RuleCandidate { Long nodeId; double score; String matchType; String evidence;
        RuleCandidate(Long n, double s, String t, String e) { nodeId=n; score=s; matchType=t; evidence=e; } }
    private static class VectorCandidate { Long nodeId; double score; String evidence; }
    private static class CandidateScore {
        double maxRuleScore, maxVectorScore; String primaryMatchType = "UNKNOWN"; List<String> evidenceList = new ArrayList<>();
        void addRule(double s, String t, String e) { if (s > maxRuleScore) { maxRuleScore = s; primaryMatchType = t; } if (e != null) evidenceList.add(e); }
        void addVector(double s, String e) { if (s > maxVectorScore) maxVectorScore = s; if (e != null) evidenceList.add(e); }
        String getCombinedEvidence() { return String.join("; ", evidenceList); }
    }
}
