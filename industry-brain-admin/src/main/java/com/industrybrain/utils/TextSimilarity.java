package com.industrybrain.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * 文本相似度工具（Jaccard / 字符 n-gram）
 * 用于产业链自动聚类的规则亲和度计算。
 *
 * @author 产业大脑平台
 */
public final class TextSimilarity {

    private TextSimilarity() { /* utility class */ }

    /**
     * 两个字符串的 Jaccard 相似度（基于 2-gram 字符集合）
     * J(A,B) = |A∩B| / |A∪B|
     */
    public static double jaccard(String a, String b) {
        if (a == null || b == null) return 0;
        if (a.isEmpty() && b.isEmpty()) return 1;
        Set<String> sa = nGramSet(a, 2);
        Set<String> sb = nGramSet(b, 2);
        if (sa.isEmpty() || sb.isEmpty()) return 0;
        Set<String> inter = new HashSet<>(sa);
        inter.retainAll(sb);
        Set<String> union = new HashSet<>(sa);
        union.addAll(sb);
        return (double) inter.size() / union.size();
    }

    /** 字符 n-gram 集合 */
    public static Set<String> nGramSet(String s, int n) {
        Set<String> set = new HashSet<>();
        if (s == null) return set;
        String t = s.replaceAll("\\s+", "");
        for (int i = 0; i + n <= t.length(); i++) {
            set.add(t.substring(i, i + n));
        }
        return set;
    }

    /** 一方包含另一方（子串包含关系） */
    public static boolean containsEither(String a, String b) {
        if (a == null || b == null) return false;
        return a.contains(b) || b.contains(a);
    }
}
