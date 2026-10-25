package com.industrybrain.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 向量运算工具（余弦相似度 / 归一化 / 质心）
 * 用于产业链自动聚类的标签向量相似度计算。
 *
 * @author 企开得胜
 */
public final class VectorOps {

    private VectorOps() { /* utility class */ }

    /** 点积 */
    public static double dot(float[] a, float[] b) {
        int len = Math.min(a.length, b.length);
        double sum = 0;
        for (int i = 0; i < len; i++) sum += a[i] * b[i];
        return sum;
    }

    /** L2 范数 */
    public static double norm(float[] v) {
        double sum = 0;
        for (float x : v) sum += x * x;
        return Math.sqrt(sum);
    }

    /** 余弦相似度（任一向量为 0 时返回 0） */
    public static double cosine(float[] a, float[] b) {
        if (a == null || b == null) return 0;
        double na = norm(a), nb = norm(b);
        if (na == 0 || nb == 0) return 0;
        return dot(a, b) / (na * nb);
    }

    /** 归一化到指定维度（截断或补 0），返回新向量 */
    public static float[] resize(float[] v, int dim) {
        if (v == null) return new float[dim];
        float[] out = new float[dim];
        System.arraycopy(v, 0, out, 0, Math.min(v.length, dim));
        return out;
    }

    /** 计算一组向量的质心（逐维平均） */
    public static float[] centroid(List<float[]> vecs) {
        if (vecs == null || vecs.isEmpty()) return new float[0];
        int dim = vecs.get(0).length;
        float[] sum = new float[dim];
        for (float[] v : vecs) {
            int len = Math.min(dim, v.length);
            for (int i = 0; i < len; i++) sum[i] += v[i];
        }
        for (int i = 0; i < dim; i++) sum[i] /= vecs.size();
        return sum;
    }

    /** 与质心最近的元素下标 */
    public static int nearestToCentroid(List<float[]> vecs) {
        float[] c = centroid(vecs);
        int best = 0;
        double bestScore = -1;
        for (int i = 0; i < vecs.size(); i++) {
            double s = cosine(vecs.get(i), c);
            if (s > bestScore) { bestScore = s; best = i; }
        }
        return best;
    }

    /** 字符串确定性伪向量（用于 fallback 降级，同字符串稳定） */
    public static float[] hashVector(String text, int dim) {
        float[] v = new float[dim];
        if (text == null || text.isEmpty()) return v;
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            int idx = Math.floorMod(chars[i] * 31 + i * 17, dim);
            v[idx] += 1f;
        }
        // 加入 n-gram 哈希，增强同前缀文本的向量相似度
        for (int i = 0; i < chars.length - 1; i++) {
            int h = (chars[i] << 8) | chars[i + 1];
            int idx = Math.floorMod(h + i * 7, dim);
            v[idx] += 0.5f;
        }
        return v;
    }
}
